/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers.actions

import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.exports.models.{AuthorizedSubmissionRequest, Eori, ErrorResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// TODO: This needs to be separated from BackendController and be injected instead of inheriting in other controllers
@Singleton
class Authenticator @Inject() (override val authConnector: AuthConnector, cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthorisedFunctions with Logging {

  def authorisedAction[A](bodyParser: BodyParser[A])(body: AuthorizedSubmissionRequest[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>
      authorisedWithEori.flatMap {
        case Right(authorisedRequest) => body(authorisedRequest)

        case Left(error) =>
          logger.error(s"Problems with Authorisation: ${error.message}")
          Future.successful(error.XmlResult)
      }
    }

  private def authorisedWithEori[A](implicit hc: HeaderCarrier, request: Request[A]): Future[Either[ErrorResponse, AuthorizedSubmissionRequest[A]]] =
    authorised(Enrolment("HMRC-CUS-ORG")).retrieve(allEnrolments) { enrolments =>
      hasEnrolment(enrolments) match {
        case Some(eori) => Future.successful(Right(AuthorizedSubmissionRequest(Eori(eori.value), request)))

        case _ =>
          logger.error("Unauthorised access. User without eori.")
          Future.successful(Left(ErrorResponse.errorUnauthorized))
      }
    } recover {
      case error: InsufficientEnrolments =>
        logger.error(s"Unauthorised access for ${request.uri} with error ${error.reason}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for exports"))

      case error: AuthorisationException =>
        logger.error(s"Unauthorised Exception for ${request.uri} with error ${error.reason}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for exports"))

      case ex: Throwable =>
        logger.error("Internal server error is " + ex.getMessage)
        Left(ErrorResponse.errorInternalServerError)
    }

  private def hasEnrolment(allEnrolments: Enrolments): Option[EnrolmentIdentifier] =
    allEnrolments
      .getEnrolment("HMRC-CUS-ORG")
      .flatMap(_.getIdentifier("EORINumber"))
}
