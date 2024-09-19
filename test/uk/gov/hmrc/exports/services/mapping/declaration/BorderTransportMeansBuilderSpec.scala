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
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class BorderTransportMeansBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "BorderTransportMeansBuilder" should {

    "build then add" when {

      "no border transport or transport details" in {
        val model = aDeclaration(withoutBorderTransport, withoutDepartureTransport())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        Option(declaration.getBorderTransportMeans) mustBe None
      }

      "no border transport, only departure transport" in {
        val declaration = new Declaration()

        val model = aDeclaration(
          withoutBorderTransport,
          withDepartureTransport(meansOfTransportOnDepartureType = "type", meansOfTransportOnDepartureIDNumber = "id")
        )
        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getID.getValue must be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue must be("type")
        Option(declaration.getBorderTransportMeans.getRegistrationNationalityCode) mustBe None
      }

      "no departure transport, only border transport" in {
        val model = aDeclaration(
          withBorderTransport(meansOfTransportCrossingTheBorderType = Some("type"), meansOfTransportCrossingTheBorderIDNumber = Some("id")),
          withTransportCountry(Some("GB")),
          withoutDepartureTransport()
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getID.getValue must be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue must be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("GB")
      }

      "departure transport and border transport nationality" in {
        val declaration = new Declaration()

        val model = aDeclaration(
          withTransportCountry(Some("GB")),
          withDepartureTransport(meansOfTransportOnDepartureType = "type", meansOfTransportOnDepartureIDNumber = "id")
        )
        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getID.getValue must be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue must be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("GB")
      }

      "unknown border transport nationality" in {
        val model = aDeclaration(withTransportCountry(None))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("GB")
      }

      "departure transport ModeOfTransportCode only" in {
        val model = aDeclaration(withoutBorderTransport, withDepartureTransport(ModeOfTransportCode.Maritime))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getModeCode.getValue must be("1")
      }

      "departure transport ModeOfTransportCode is Empty" in {
        val model = aDeclaration(withoutBorderTransport, withDepartureTransport(ModeOfTransportCode.Empty))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        Option(declaration.getBorderTransportMeans.getModeCode) mustBe None
      }

      "fully populated" in {
        val model = aDeclaration(
          withBorderTransport(meansOfTransportCrossingTheBorderType = Some("type"), meansOfTransportCrossingTheBorderIDNumber = Some("id")),
          withTransportCountry(Some("GB")),
          withDepartureTransport(borderModeOfTransportCode = ModeOfTransportCode.Road)
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getID.getValue must be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue must be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("GB")
        declaration.getBorderTransportMeans.getModeCode.getValue must be("3")
      }
    }
  }

  private def builder = new BorderTransportMeansBuilder()
}
