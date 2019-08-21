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

package uk.gov.hmrc.exports.controllers.actions

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.exports.models.{AuthorizedSubmissionRequest, Eori, ErrorResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class Authenticator @Inject()(override val authConnector: AuthConnector, parsers: PlayBodyParsers)(
  implicit override val executionContext: ExecutionContext
) extends ActionRefiner[Request, AuthorizedSubmissionRequest] with AuthorisedFunctions
    with ActionBuilder[AuthorizedSubmissionRequest, AnyContent] {

  private val logger = Logger(this.getClass)

  override def parser: BodyParser[AnyContent] = parsers.default

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorizedSubmissionRequest[A]]] = {
    val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    authorisedWithEori(hc, request)
  }

  private def authorisedWithEori[A](
    hc: HeaderCarrier,
    request: Request[A]
  ): Future[Either[Result, AuthorizedSubmissionRequest[A]]] =
    authorised(Enrolment("HMRC-CUS-ORG")).retrieve(allEnrolments) { enrolments =>
      hasEnrolment(enrolments) match {
        case Some(eori) => Future.successful(Right(AuthorizedSubmissionRequest(Eori(eori.value), request)))
        case _ =>
          logger.error("Unauthorised access. User without eori.")
          Future.successful(Left(ErrorResponse.errorUnauthorized.XmlResult))
      }
    }(hc, executionContext) recover {
      case error: InsufficientEnrolments =>
        logger.error(s"Unauthorised access for ${request.uri} with error ${error.reason}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for exports").XmlResult)
      case error: AuthorisationException =>
        logger.error(s"Unauthorised Exception for ${request.uri} with error ${error.reason}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for exports").XmlResult)
      case NonFatal(exception) =>
        logger.error("Internal server error is " + exception.getMessage)
        Left(ErrorResponse.errorInternalServerError.XmlResult)
    }

  private def hasEnrolment(allEnrolments: Enrolments): Option[EnrolmentIdentifier] =
    allEnrolments.getEnrolment("HMRC-CUS-ORG").flatMap(_.getIdentifier("EORINumber"))
}
