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

package uk.gov.hmrc.exports.syntax

import org.scalactic.source.Position

class ThenBuilder(
  val precondition: Seq[(Conjuction, Precondition)],
  val actions: Seq[(Conjuction, Action)],
  val postconditions: Seq[(Conjuction, Postcondition)]
) {
  def And(postcondition: Postcondition)(implicit position: Position): ThenBuilder = {
    val entry: (Conjuction, Postcondition) = AndConjuction() -> postcondition
    new ThenBuilder(precondition, actions, postconditions :+ entry)
  }
}

class WhenBuilder(val preconditions: Seq[(Conjuction, Precondition)], val actions: Seq[(Conjuction, Action)]) {
  def And(when: Action)(implicit position: Position): WhenBuilder =
    new WhenBuilder(preconditions, actions :+ (AndConjuction() -> when))
  def Then(postcondition: Postcondition)(implicit position: Position) =
    new ThenBuilder(preconditions, actions, Seq(ThenConjuction() -> postcondition))
}

class GivenBuilder(val preconditions: Seq[(Conjuction, Precondition)]) {
  def And(pre: Precondition)(implicit position: Position) = {
    val entry = AndConjuction() -> pre
    new GivenBuilder(preconditions :+ entry)
  }
  def When(when: Action)(implicit position: Position) = {
    val entry: (Conjuction, Action) = WhenConjuction() -> when
    new WhenBuilder(preconditions, Seq(entry))
  }
}
