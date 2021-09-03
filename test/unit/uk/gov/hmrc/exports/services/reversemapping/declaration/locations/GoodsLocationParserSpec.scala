/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.reversemapping.declaration.locations

import org.scalatest.EitherValues
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.GoodsLocation
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.GoodsLocationParserSpec._

import scala.xml.{Elem, NodeSeq}

class GoodsLocationParserSpec extends UnitSpec with EitherValues {

  private val parser = new GoodsLocationParser

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "GoodsLocationParser on parse" should {

    "return Right with None" when {

      "no GoodsLocation elements are present" in {

        val result = parser.parse(inputXml())

        result.isRight mustBe true
        result.value mustBe None
      }

      "GoodsLocation element is present but it has no elements" in {

        val result = parser.parse(missingAllMandatoryElements)

        result.isRight mustBe true
        result.value mustBe None
      }

      "GoodsLocation element is present but its elements are empty" in {

        val result = parser.parse(inputXml(Some(GoodsLocation("", "", "", None))))

        result.isRight mustBe true
        result.value mustBe None
      }
    }

    "return Right with GoodsLocation" when {

      "GoodsLocation element is present with 'Name' element" in {

        val testGoodsLocation =
          GoodsLocation(country = "GB", typeOfLocation = "ToL", qualifierOfIdentification = "QoI", identificationOfLocation = Some("IdOfLocation"))

        val result = parser.parse(inputXml(Some(testGoodsLocation)))

        result.isRight mustBe true
        result.value mustBe defined
        result.value.get.country mustBe "GB"
        result.value.get.typeOfLocation mustBe "ToL"
        result.value.get.qualifierOfIdentification mustBe "QoI"
        result.value.get.identificationOfLocation mustBe defined
        result.value.get.identificationOfLocation.get mustBe "IdOfLocation"
      }

      "GoodsLocation element is present with NO 'Name' element" in {

        val testGoodsLocation =
          GoodsLocation(country = "GB", typeOfLocation = "ToL", qualifierOfIdentification = "QoI", identificationOfLocation = None)

        val result = parser.parse(inputXml(Some(testGoodsLocation)))

        result.isRight mustBe true
        result.value mustBe defined
        result.value.get.country mustBe "GB"
        result.value.get.typeOfLocation mustBe "ToL"
        result.value.get.qualifierOfIdentification mustBe "QoI"
        result.value.get.identificationOfLocation mustNot be(defined)
      }
    }

    "return Right with incomplete GoodsLocation" when {

      "GoodsLocation element is present with NO 'Address / CountryCode' element" in {

        val result = parser.parse(missingAddressCountryCodeElementXml)

        result.isRight mustBe true
        result.value mustBe defined
        result.value.get.country mustBe ""
        result.value.get.typeOfLocation mustBe "A"
        result.value.get.qualifierOfIdentification mustBe "U"
        result.value.get.identificationOfLocation mustBe defined
        result.value.get.identificationOfLocation.get mustBe "FXTFXTFXT"
      }

      "GoodsLocation element is present with NO 'Address / TypeCode' element" in {

        val result = parser.parse(missingAddressTypeCodeElementXml)

        result.isRight mustBe true
        result.value mustBe defined
        result.value.get.country mustBe "GB"
        result.value.get.typeOfLocation mustBe "A"
        result.value.get.qualifierOfIdentification mustBe ""
        result.value.get.identificationOfLocation mustBe defined
        result.value.get.identificationOfLocation.get mustBe "FXTFXTFXT"
      }

      "GoodsLocation element is present with NO 'TypeCode' element" in {

        val result = parser.parse(missingTypeCodeElementXml)

        result.isRight mustBe true
        result.value mustBe defined
        result.value.get.country mustBe "GB"
        result.value.get.typeOfLocation mustBe ""
        result.value.get.qualifierOfIdentification mustBe "U"
        result.value.get.identificationOfLocation mustBe defined
        result.value.get.identificationOfLocation.get mustBe "FXTFXTFXT"
      }
    }
  }

}

object GoodsLocationParserSpec {

  private def inputXml(maybeGoodsLocation: Option[GoodsLocation] = None): Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            { maybeGoodsLocation.map { goodsLocation =>
            <ns3:GoodsLocation>
              { goodsLocation.identificationOfLocation.map { idOfLocation =>
              <ns3:Name>{idOfLocation}</ns3:Name>
            }.getOrElse(NodeSeq.Empty) }

              <ns3:TypeCode>{goodsLocation.typeOfLocation}</ns3:TypeCode>
              <ns3:Address>
                <ns3:TypeCode>{goodsLocation.qualifierOfIdentification}</ns3:TypeCode>
                <ns3:CountryCode>{goodsLocation.country}</ns3:CountryCode>
              </ns3:Address>
            </ns3:GoodsLocation>
          }.getOrElse(NodeSeq.Empty) }
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingAllMandatoryElements: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:GoodsLocation>
            </ns3:GoodsLocation>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingAddressCountryCodeElementXml: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:GoodsLocation>
              <ns3:Name>FXTFXTFXT</ns3:Name>
              <ns3:TypeCode>A</ns3:TypeCode>
              <ns3:Address>
                <ns3:TypeCode>U</ns3:TypeCode>
              </ns3:Address>
            </ns3:GoodsLocation>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingAddressTypeCodeElementXml: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:GoodsLocation>
              <ns3:Name>FXTFXTFXT</ns3:Name>
              <ns3:TypeCode>A</ns3:TypeCode>
              <ns3:Address>
                <ns3:CountryCode>GB</ns3:CountryCode>
              </ns3:Address>
            </ns3:GoodsLocation>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingTypeCodeElementXml: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:GoodsLocation>
              <ns3:Name>FXTFXTFXT</ns3:Name>
              <ns3:Address>
                <ns3:TypeCode>U</ns3:TypeCode>
                <ns3:CountryCode>GB</ns3:CountryCode>
              </ns3:Address>
            </ns3:GoodsLocation>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>
}
