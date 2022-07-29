/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logging
import play.api.libs.json.{JsString, Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.REST.writes
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.models.{DeclarationSearch, DeclarationSort, Page}
import uk.gov.hmrc.exports.services.DeclarationService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class DeclarationController @Inject() (
  declarationService: DeclarationService,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(controllerComponents) with Logging {

  def create(): Action[ExportsDeclarationRequest] =
    authenticator.authorisedAction(parsingJson[ExportsDeclarationRequest]) { implicit request =>
      logPayload("Create Declaration Request Received", request.body)
      declarationService
        .create(ExportsDeclaration(UUID.randomUUID.toString, request.eori, request.body))
        .map(logPayload("Create Declaration Response", _))
        .map(declaration => Created(declaration))
    }

  def findOrCreateDraftFromParent(parentId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    declarationService.findOrCreateDraftFromParent(request.eori, parentId).map { result =>
      result match {
        case (DeclarationService.CREATED, id) => Created(JsString(id))
        /* FOUND */
        case (_, id) => Ok(JsString(id))
      }
    }
  }

  def findAll(status: Option[String], pagination: Page, sort: DeclarationSort): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { implicit request =>
      val search =
        DeclarationSearch(eori = request.eori, status = status.map(str => Try(DeclarationStatus.withName(str))).filter(_.isSuccess).map(_.get))
      declarationService.find(search, pagination, sort).map(results => Ok(results))
    }

  def findById(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    declarationService.findOne(request.eori, id).map {
      case Some(declaration) => Ok(declaration)
      case None              => NotFound
    }
  }

  def deleteById(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    declarationService.findOne(request.eori, id).flatMap {
      case Some(declaration) if declaration.status == DeclarationStatus.COMPLETE =>
        Future.successful(BadRequest(ErrorResponse("Cannot remove a declaration once it is COMPLETE")))
      case Some(declaration) => declarationService.deleteOne(declaration).map(_ => NoContent)
      case None              => Future.successful(NoContent)
    }
  }

  def update(id: String): Action[ExportsDeclarationRequest] =
    authenticator.authorisedAction(parsingJson[ExportsDeclarationRequest]) { implicit request =>
      logPayload("Update Declaration Request Received", request.body)
      declarationService
        .update(ExportsDeclaration(id, request.eori, request.body))
        .map(logPayload("Update Declaration Response", _))
        .map {
          case Some(declaration) => Ok(declaration)
          case None              => NotFound
        }
    }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix: ${Json.toJson(payload)}")
    payload
  }
}
