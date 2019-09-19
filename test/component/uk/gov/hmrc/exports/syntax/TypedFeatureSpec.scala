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

package component.uk.gov.hmrc.exports.syntax

import org.scalactic.source.Position
import org.scalatest.FeatureSpec
import org.scalatest.concurrent.PatienceConfiguration

import scala.reflect.ClassTag

abstract class TypedFeatureSpec extends FeatureSpec {

  class ScenarioBuilder(name: String) {
    def apply[A](x: ScenarioBuilder => ThenBuilder) =
      scenario(name) {
        val outcome = x(this)
        val preconditionContext = prepareContext(new ScenarioContext(Map.empty))

        val actionContext = outcome.precondition.foldLeft(preconditionContext) {
          case (context, (conjuction, precondition)) => conjuction.execute(precondition, info, context)
        }
        val postconditionContext = outcome.actions.foldLeft(actionContext) {
          case (context, (conjuction, action)) => conjuction.execute(action, info, context)
        }
        outcome.postconditions.foldLeft(postconditionContext) {
          case (context, (conjuction, postcondition)) => conjuction.execute(postcondition, info, context)
        }
      }

    def Given[A <: Precondition](pre: A)(implicit position: Position, tag: ClassTag[A]): GivenBuilder =
      new GivenBuilder(Seq(GivenConjuction(tag.toString()) -> pre))
  }

  def Scenario(name: String): ScenarioBuilder = new ScenarioBuilder(name)

  def prepareContext(contest: ScenarioContext): ScenarioContext

}
