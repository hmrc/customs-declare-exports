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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.REST.format
import uk.gov.hmrc.exports.models.{DeclarationSearch, DeclarationSort, Page}
import uk.gov.hmrc.exports.services.DeclarationService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class DeclarationController @Inject()(
  declarationService: DeclarationService,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(controllerComponents) {
  private val logger = Logger(this.getClass)

  def create(): Action[ExportsDeclarationRequest] =
    authenticator.authorisedAction(parsingJson[ExportsDeclarationRequest]) { implicit request =>
      logPayload("Create Declaration Request Received", request.body)
      declarationService
        .create(request.body.toExportsDeclaration(id = UUID.randomUUID().toString, eori = request.eori))
        .map(logPayload("Create Declaration Response", _))
        .map(declaration => Created(declaration))
    }

  def update(id: String): Action[ExportsDeclarationRequest] =
    authenticator.authorisedAction(parsingJson[ExportsDeclarationRequest]) { implicit request =>
      logPayload("Update Declaration Request Received", request.body)
      declarationService.findOne(id, request.eori).flatMap {
        case Some(declaration) if declaration.status == DeclarationStatus.COMPLETE =>
          val response = ErrorResponse("Cannot update a declaration once it is COMPLETE")
          logPayload("Update Declaration Response", response)
          Future.successful(BadRequest(response))
        case Some(_) =>
          declarationService
            .update(request.body.toExportsDeclaration(id = id, eori = request.eori))
            .map(logPayload("Update Declaration Response", _))
            .map {
              case Some(declaration) => Ok(declaration)
              case None              => NotFound
            }
        case None =>
          logPayload("Update Declaration Response", "Not Found")
          Future.successful(NotFound)
      }
    }

  def findAll(status: Option[String], pagination: Page, sort: DeclarationSort): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { implicit request =>
      val search = DeclarationSearch(eori = request.eori, status = status.map(v => Try(DeclarationStatus.withName(v))).filter(_.isSuccess).map(_.get))
      declarationService.find(search, pagination, sort).map(results => Ok(results))
    }

  def findByID(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    declarationService.findOne(id, request.eori).map {
      case Some(declaration) => Ok(declaration)
      case None              => NotFound
    }
  }

  def deleteByID(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    declarationService.findOne(id, request.eori).flatMap {
      case Some(declaration) if declaration.status == DeclarationStatus.COMPLETE =>
        Future.successful(BadRequest(ErrorResponse("Cannot remove a declaration once it is COMPLETE")))
      case Some(declaration) => declarationService.deleteOne(declaration).map(_ => NoContent)
      case None              => Future.successful(NoContent)
    }
  }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"Create Request Received: ${Json.toJson(payload)}")
    payload
  }

}
