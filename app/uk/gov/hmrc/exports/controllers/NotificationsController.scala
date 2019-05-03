/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.exports.controllers

import com.google.inject.Singleton
import javax.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.wco.dec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

@Singleton
class NotificationsController @Inject()(
  appConfig: AppConfig,
  authConnector: AuthConnector,
  headerValidator: HeaderValidator,
  notificationsRepository: NotificationsRepository,
  metrics: ExportsMetrics,
  submissionRepository: SubmissionRepository
) extends ExportController(authConnector) {

  private val logger = Logger(this.getClass())

  def saveNotification(): Action[NodeSeq] = Action.async(parse.xml) { implicit request =>
    val timer = metrics.startTimer(notificationMetric)
    headerValidator
      .validateAndExtractSubmissionNotificationHeaders(request.headers.toSimpleMap) match {
      case Right(extractedHeaders) =>
        getSubmissionNotificationFromRequest(extractedHeaders).flatMap {
            case Some(notification) =>
              save(notification).map { res =>
                timer.stop()
                res
              }
            case _ =>
              logger.error("Invalid notification payload")
              Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
        }
      case Left(error) =>
        logger.error(s"Error during validation and extracting headers with message: ${ error.message}")
        Future.successful(error.XmlResult)
    }
  }

  //TODO response should be streamed or paginated depending on the no of notifications.
  def getNotifications(): Action[AnyContent] =
    authorisedAction(BodyParsers.parse.default) { request =>
      notificationsRepository
        .findByEori(request.eori.value)
        .map(res => Ok(Json.toJson(res)))
    }

  def getSubmissionNotifications(conversationId: String): Action[AnyContent] =
    authorisedAction(BodyParsers.parse.default) { implicit authorizedRequest =>
      notificationsRepository
        .getByEoriAndConversationId(authorizedRequest.eori.value, conversationId)
        .map(res => Ok(Json.toJson(res)))
    }

  private def getSubmissionNotificationFromRequest(
    vhnar: SubmissionNotificationApiRequest
  )(implicit request: Request[NodeSeq]): Future[Option[DeclarationNotification]] =
    submissionRepository
      .getByConversationId(vhnar.conversationId.value)
      .map { mayBeSubmission =>
        mayBeSubmission.flatMap { submission =>
          handleXmlParseToNotification(request.body.toString, vhnar.conversationId.value, submission.eori)
        }
      }

  private def handleXmlParseToNotification(xmlString: String, conversationId: String, eori: String) = {
    val parseXmlResult = Try[MetaData] {
      MetaData.fromXml(xmlString)
    }

    parseXmlResult match {
      case Success(metaData) =>
        val mrn = metaData.response.headOption.flatMap(_.declaration.flatMap(_.id))

        if (mrn.isEmpty) {
          logger.error("Unable to determine MRN during parsing notification")
        }

        val notification = DeclarationNotification(
          DateTime.now,
          conversationId,
          mrn.getOrElse("UNKNOWN"),
          eori,
          DeclarationMetadata(
            metaData.wcoDataModelVersionCode,
            metaData.wcoTypeName,
            metaData.responsibleCountryCode,
            metaData.responsibleAgencyName,
            metaData.agencyAssignedCustomizationCode,
            metaData.agencyAssignedCustomizationVersionCode
          ),
          metaData.response
        )

        Some(notification)
      case Failure(exception) =>
        logger.error(s"There is a problem during parsing notification with exception: ${exception.getMessage}")
        None
    }
  }

  private def save(notification: DeclarationNotification)(implicit hc: HeaderCarrier): Future[Result] = {
    val eori = notification.eori
    val convId = notification.conversationId

    for {
      oldNotification <- notificationsRepository
        .getByEoriAndConversationId(eori, convId)
        .map(_.sortWith((a, b) => a.isOlderThan(b)).headOption)
      notificationSaved <- notificationsRepository.save(notification)
      shouldBeUpdated = oldNotification.forall(notification.isOlderThan)
      _ <- if (shouldBeUpdated)
        submissionRepository.updateMrnAndStatus(
          notification.eori,
          notification.conversationId,
          notification.mrn,
          buildStatus(notification.response)
        )
      else Future.successful(false)
    } yield
      if (notificationSaved) {
        metrics.incrementCounter(notificationMetric)
        logger.debug("Notification saved successfully")
        Accepted
      } else {
        metrics.incrementCounter(notificationMetric)
        logger.error("There was a problem during saving notification")
        InternalServerError(NotificationFailedErrorResponse.toXml())
      }
  }

  private def buildStatus(responses: Seq[Response]): Option[String] =
    responses.map { response =>
      (response.functionCode, response.status.flatMap(_.nameCode).headOption) match {
        case ("11", Some(nameCode)) if nameCode == "39" || nameCode == "41" =>
          s"11$nameCode"
        case _ => response.functionCode
      }
    }.headOption
}
