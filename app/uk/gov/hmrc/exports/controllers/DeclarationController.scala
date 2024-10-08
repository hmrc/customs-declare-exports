/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.draftStatuses
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.services.DeclarationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeclarationController @Inject() (
  declarationService: DeclarationService,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(controllerComponents) with Logging {

  val create: Action[ExportsDeclaration] = authenticator.authorisedAction(parsingJson[ExportsDeclaration]) { request =>
    logPayload("Create Declaration Request Received", request.body)
    declarationService
      .create(ExportsDeclaration.init(request.eori, request.body, update = false))
      .map(logPayload("Create Declaration Response", _))
      .map(declaration => Created(declaration))
  }

  val update: Action[ExportsDeclaration] =
    authenticator.authorisedAction(parsingJson[ExportsDeclaration]) { request =>
      logPayload("Update Declaration Request Received", request.body)
      declarationService
        .update(ExportsDeclaration.init(request.eori, request.body, update = true))
        .map(logPayload("Update Declaration Response", _))
        .map {
          case Some(declaration) => Ok(declaration)
          case None              => NotFound
        }
    }

  def deleteById(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { request =>
    declarationService.findOne(request.eori, id).flatMap {
      case Some(declaration) if declaration.status == DeclarationStatus.COMPLETE =>
        Future.successful(BadRequest(ErrorResponse("Cannot remove a declaration once it is COMPLETE")))
      case Some(declaration) => declarationService.deleteOne(declaration).map(_ => NoContent)
      case None              => Future.successful(NoContent)
    }
  }

  def fetchPageOfDraft(page: Page, sort: DeclarationSort): Action[AnyContent] = authenticator.authorisedAction(parse.default) { request =>
    declarationService.fetchPage(DeclarationSearch(request.eori, draftStatuses), page, sort).map { case (declarations, total) =>
      Ok(Paginated(declarations.map(DraftDeclarationData(_)), page, total))
    }
  }

  def findOrCreateDraftFromParent(parentId: String, enhancedStatus: String, isAmendment: Boolean): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { request =>
      declarationService.findOrCreateDraftFromParent(request.eori, parentId, EnhancedStatus.withName(enhancedStatus), isAmendment).map {
        case Some(DeclarationService.CREATED -> declarationId) => Created(JsString(declarationId))
        case Some(DeclarationService.FOUND -> declarationId)   => Ok(JsString(declarationId))
        case _                                                 => NotFound
      }
    }

  def findById(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { request =>
    declarationService.findOne(request.eori, id).map {
      case Some(declaration) => Ok(declaration)
      case None              => NotFound
    }
  }

  def findDraftByParent(parentId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { request =>
    declarationService.findDraftByParent(request.eori, parentId).map {
      case Some(declaration) => Ok(declaration)
      case None              => NotFound
    }
  }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix: ${Json.toJson(payload)}")
    payload
  }
}
