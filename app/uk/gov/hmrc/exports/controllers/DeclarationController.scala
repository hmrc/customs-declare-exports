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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.services.DeclarationService

import scala.concurrent.ExecutionContext

@Singleton
class DeclarationController @Inject()(
  declarationService: DeclarationService,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext) extends RESTController {

  def post(): Action[ExportsDeclarationRequest] =
    authenticator.authorisedAction(parse.json[ExportsDeclarationRequest]) { implicit request =>
      declarationService
        .save(request.body.toExportsDeclaration(id = UUID.randomUUID().toString, eori = request.eori))
        .map(declaration => Created(Json.toJson(declaration)))
    }

}
