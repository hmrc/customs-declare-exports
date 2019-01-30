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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.exports.models.AuthorizedRequest
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExportController @Inject()(override val authConnector: AuthConnector)(implicit ec: ExecutionContext)
    extends BaseController with AuthorisedFunctions {

  def authorizedWithEori[A](
    callback: (AuthorizedRequest[A] => Future[Result])
  )(implicit request: Request[A]): Future[Result] =
    authorised(Enrolment("HMRC-CUS-ORG")).retrieve(allEnrolments) { enrolments =>
      getEoriFromEnrolments(enrolments) match {
        case Some(eori) if eori.nonEmpty => callback(AuthorizedRequest(request, eori))
        case _                           => throw InsufficientEnrolments()
      }
    } recoverWith {
      handleFailure
    }

  private def getEoriFromEnrolments(enrolments: Enrolments): Option[String] =
    enrolments.getEnrolment("HMRC-CUS-ORG").flatMap(_.getIdentifier("EORINumber")).map(_.value)

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Future[Result]] =
    PartialFunction[Throwable, Future[Result]] {
      case _: InsufficientEnrolments =>
        Logger.warn(s"Unauthorised access for ${request.uri}")
        Future.successful(Unauthorized(Json.toJson("Unauthorized for exports")))
      case _: AuthorisationException =>
        Logger.warn(s"Unauthorised Exception for ${request.uri}")
        Future.successful(Unauthorized(Json.toJson("Unauthorized for exports")))
      case ex: Throwable =>
        Logger.error("Internal server error is " + ex.getMessage)
        Future.successful(InternalServerError(Json.toJson("InternalServerError")))
    }
}
