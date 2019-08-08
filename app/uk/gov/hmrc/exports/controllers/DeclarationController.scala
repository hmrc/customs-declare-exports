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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.DeclarationService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DeclarationController @Inject()(
  declarationService: DeclarationService,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
) extends RESTController {

  def post(): Action[ExportsDeclaration] = authenticator.authorisedAction(parse.json[ExportsDeclaration]) { implicit request =>
    declarationService
      .save(request.body)
      .map(declaration => Created(Json.toJson(declaration)))
  }

}
