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
import org.scalatest.Informer

trait Conjuction {
  def execute(step: Step, info: Informer, context: ScenarioContext): ScenarioContext
}

case class GivenConjuction(name: String)(implicit position: Position) extends Conjuction {
  override def execute(step: Step, info: Informer, context: ScenarioContext): ScenarioContext = {
    info(s"Given: ${step.name}")(position)
    step.execute(context)
  }
}
case class WhenConjuction(implicit position: Position) extends Conjuction {
  override def execute(step: Step, info: Informer, context: ScenarioContext): ScenarioContext = {
    info(s"When: ${step.name}")(position)
    step.execute(context)
  }
}
case class ThenConjuction(implicit position: Position) extends Conjuction {
  override def execute(step: Step, info: Informer, context: ScenarioContext): ScenarioContext = {
    info(s"Then: ${step.name}")(position)
    step.execute(context)
  }
}
case class AndConjuction(implicit position: Position) extends Conjuction {
  override def execute(step: Step, info: Informer, context: ScenarioContext): ScenarioContext = {
    info(s"And: ${step.name}")(position)
    step.execute(context)
  }
}
