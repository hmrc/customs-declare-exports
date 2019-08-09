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

package util.testdata

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration

//noinspection ScalaStyle

trait ExportsDeclarationBuilder {

  protected val DUCR = "5GB123456789000-123ABC456DEFIIIII"
  protected val LRN = "FG7676767889"

  private def uuid: String = UUID.randomUUID().toString

  private val modelWithDefaults: ExportsDeclaration = ExportsDeclaration(
    id = uuid,
    createdDateTime = LocalDateTime.of(2019, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC),
    updatedDateTime = LocalDateTime.of(2019, 2, 2, 0, 0, 0).toInstant(ZoneOffset.UTC),
    choice = "choice",
    eori = "eori"
  )

  private type ExportsDeclarationModifier = ExportsDeclaration => ExportsDeclaration

  def aDeclaration(modifiers: ExportsDeclarationModifier*): ExportsDeclaration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  // ************************************************* Builders ********************************************************

  def withId(id: String): ExportsDeclarationModifier = _.copy(id = id)

  def withEori(eori: String): ExportsDeclarationModifier = _.copy(eori = eori)

  def withChoice(choice: String): ExportsDeclarationModifier = _.copy(choice = choice)

}
