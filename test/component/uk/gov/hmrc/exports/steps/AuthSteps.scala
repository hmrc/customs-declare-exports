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

import component.uk.gov.hmrc.exports.syntax.{Postcondition, Precondition, ScenarioContext}
import util.AuthService
import util.testdata.ExportsTestData.declarantEori

object `Authorized user` extends Precondition with AuthService {
  def name = "Authorized user"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val eori = declarantEori
    authServiceAuthorizesWithEoriAndNoRetrievals(eori = eori)
    context.updated(eori)
  }
}

object `User has been authorized` extends Postcondition with AuthService {
  override def execute(context: ScenarioContext): ScenarioContext = {
    verifyAuthServiceCalledForNonCsp()
    context
  }

  override def name: String = "User has been authorized"
}

object `Unauthorized user` extends Precondition with AuthService {
  override def execute(context: ScenarioContext): ScenarioContext = {
    stubAllUnauthorized()
    context
  }

  override def name: String = "Unauthorized user"
}
