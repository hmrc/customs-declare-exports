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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class CurrencyExchangeBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  private def builder = new CurrencyExchangeBuilder()

  "CurrencyExchangeBuilder" should {

    "build then add" when {
      "no Total Number Of Items" in {
        // Given
        val model = aDeclaration(withoutTotalNumberOfItems())
        val declaration = new Declaration()
        // When
        builder.buildThenAdd(model, declaration)
        // Then
        declaration.getCurrencyExchange mustBe empty
      }

      "exchange rate is empty" in {
        // Given
        val model = aDeclaration(withTotalNumberOfItems(exchangeRate = None))
        val declaration = new Declaration()
        // When
        builder.buildThenAdd(model, declaration)
        // Then
        declaration.getCurrencyExchange mustBe empty
      }

      "exchange rate is populated" in {
        // Given
        val model = aDeclaration(withTotalNumberOfItems(exchangeRate = Some("123")))
        val declaration = new Declaration()
        // When
        builder.buildThenAdd(model, declaration)
        // Then
        declaration.getCurrencyExchange must have(size(1))
        declaration.getCurrencyExchange.get(0).getCurrencyTypeCode mustBe null
        declaration.getCurrencyExchange.get(0).getRateNumeric.intValue() mustBe 123
      }
    }

    "exchange rate value" should {
      "be prefixed with a zero if first char is a period" in {
        val model = aDeclaration(withTotalNumberOfItems(exchangeRate = Some(".12")))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getCurrencyExchange must have(size(1))
        declaration.getCurrencyExchange.get(0).getRateNumeric.toString mustBe "0.12"
      }

      "have the trailing period removed if no digits appear after it" in {
        val model = aDeclaration(withTotalNumberOfItems(exchangeRate = Some("12.")))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getCurrencyExchange must have(size(1))
        declaration.getCurrencyExchange.get(0).getRateNumeric.toString mustBe "12"
      }

      "have any commas removed if present" in {
        val model = aDeclaration(withTotalNumberOfItems(exchangeRate = Some("12,000,000.10")))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getCurrencyExchange must have(size(1))
        declaration.getCurrencyExchange.get(0).getRateNumeric.toString mustBe "12000000.10"
      }
    }
  }
}
