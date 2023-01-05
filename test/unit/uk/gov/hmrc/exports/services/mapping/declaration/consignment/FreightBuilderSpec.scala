/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping.declaration.consignment

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.TransportPayment._
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class FreightBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "FreightBuilder" should {

    "build then add" when {
      "no transport details" in {
        // Given
        val model: ExportsDeclaration = aDeclaration()
        val consignment = new Declaration.Consignment()

        // When
        new FreightBuilder().buildThenAdd(model, consignment)

        // Then
        Option(consignment.getFreight) mustBe None
      }

      "payment method is populated with a value that should be sent to DMS" in {
        // Given
        val model = aDeclaration(withTransportPayment(cash))
        val consignment = new Declaration.Consignment()

        // When
        new FreightBuilder().buildThenAdd(model, consignment)

        // Then
        consignment.getFreight.getPaymentMethodCode.getValue mustBe cash
      }

      "payment method is populated with a value that should not be sent to DMS" in {
        // Given
        val model = aDeclaration(withTransportPayment(notAvailable))
        val consignment = new Declaration.Consignment()

        // When
        new FreightBuilder().buildThenAdd(model, consignment)

        // Then
        Option(consignment.getFreight) mustBe None
      }
    }
  }
}
