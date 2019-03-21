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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.exports.models.{AuthorizedSubmissionRequest, Eori, ErrorResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExportController @Inject()(override val authConnector: AuthConnector)(
    implicit ec: ExecutionContext)
    extends BaseController
    with AuthorisedFunctions {

  private def hasEnrolment(
      allEnrolments: Enrolments): Option[EnrolmentIdentifier] =
    allEnrolments
      .getEnrolment("HMRC-CUS-ORG")
      .flatMap(_.getIdentifier("EORINumber"))

  def authorisedWithEori[A](implicit hc: HeaderCarrier, request: Request[A])
    : Future[Either[ErrorResponse, AuthorizedSubmissionRequest[A]]] = {
    authorised(Enrolment("HMRC-CUS-ORG")).retrieve(allEnrolments) {
      enrolments =>
        hasEnrolment(enrolments) match {
          case Some(eori) =>
            Future.successful(
              Right(AuthorizedSubmissionRequest(Eori(eori.value), request)))
          case _ => Future.successful(Left(ErrorResponse.ErrorUnauthorized))
        }
    } recover {
      case _: InsufficientEnrolments =>
        Logger.warn(s"Unauthorised access for ${request.uri}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for imports"))
      case e: AuthorisationException =>
        Logger.warn(s"Unauthorised Exception for ${request.uri} ${e.reason}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for imports"))
      case ex: Throwable =>
        Logger.error("Internal server error is " + ex.getMessage)
        Left(ErrorResponse.ErrorInternalServerError)
    }
  }
  def authorisedAction[A](bodyParser: BodyParser[A])(
      body: AuthorizedSubmissionRequest[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>
      authorisedWithEori.flatMap {
        case Right(authorisedRequest) =>
          Logger.info(s"Authorised request for ${authorisedRequest.eori.value}")
          body(authorisedRequest)
        case Left(error) =>
          Logger.error("Problems with Authorisation")
          Future.successful(error.XmlResult)
      }
    }

}
