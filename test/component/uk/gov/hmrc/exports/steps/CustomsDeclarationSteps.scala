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

package component.uk.gov.hmrc.exports.steps

import java.util.UUID

import component.uk.gov.hmrc.exports.syntax.{Postcondition, Precondition, ScenarioContext}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.gov.hmrc.exports.models.Eori
import util.CustomsDeclarationsAPIConfig
import util.stubs.CustomsDeclarationsAPIService

object `Customs declaration is fully operational` extends Precondition with CustomsDeclarationsAPIService {

  def name = "Customs declaration is fully operational"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val conversationId = UUID.randomUUID().toString
    startSubmissionService(conversationId = conversationId)
    context.updated(conversationId)
  }
}

class CustomsDeclarationResponse(status: Int) extends Precondition with CustomsDeclarationsAPIService {
  override def name: String = s"Customs declaration response is $status"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val conversationId = UUID.randomUUID().toString
    startSubmissionService(status, conversationId)
    context.updated(conversationId)
  }
}

object `Customs declaration response` {
  def is(status: Int) = new CustomsDeclarationResponse(status)
}

object `Declaration is submitted to customs-declarations`
    extends Postcondition with CustomsDeclarationsAPIService with Eventually with IntegrationPatience {
  override def execute(context: ScenarioContext): ScenarioContext = {
    val eori = context.get[Eori]
    eventually(verifyDecServiceWasCalledCorrectly(eori.value, CustomsDeclarationsAPIConfig.apiVersion))
    context
  }

  def name = "Declaration is submitted to customs-declarations"
}

object `No submission is posted on customs-declarations` extends Postcondition with CustomsDeclarationsAPIService {
  override def name: String = "No submission is posted on customs-declarations"

  override def execute(context: ScenarioContext): ScenarioContext = {
    verifyDecServiceWasNotCalled()
    context
  }
}

object `Customs declaration does not response` extends Precondition with CustomsDeclarationsAPIService {
  override def name: String = "Customs declaration does not response"

  override def execute(context: ScenarioContext): ScenarioContext = {
    startSubmissionService(conversationId = UUID.randomUUID().toString, delay = 5000)
    context
  }
}
