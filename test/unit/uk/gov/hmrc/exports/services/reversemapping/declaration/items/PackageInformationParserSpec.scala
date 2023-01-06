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

package uk.gov.hmrc.exports.services.reversemapping.declaration.items

import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.PackageInformation
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.{Elem, NodeSeq}

class PackageInformationParserSpec extends UnitSpec {

  private val packageInformationParser = new PackageInformationParser()
  private implicit val context = MappingContext(eori)

  "PackageInformationParser on parse" should {

    "return Right with an empty List" when {
      "no '/ GovernmentAgencyGoodsItem / Packaging' elements are present" in {
        val xml = inputXml()
        packageInformationParser.parse(xml).toOption.get mustBe Seq.empty
      }
    }

    "return Right with a List of 'None' values" when {
      "only empty '/ GovernmentAgencyGoodsItem / Packaging' elements are present" in {
        val xml = inputXml(List(Packaging(), Packaging()))
        packageInformationParser.parse(xml).toOption.get mustBe List(None, None)
      }
    }

    "return Right with a List of PackageInformation instances" when {

      "a single '/ GovernmentAgencyGoodsItem / Packaging' element is fully present" in {
        val xml = inputXml(List(Packaging(Some("TypeCode"), Some("3"), Some("MarksNumbersID"))))

        packageInformationParser.parse(xml).toOption.get.head.get must matchPattern {
          case PackageInformation(_, Some("TypeCode"), Some(3), Some("MarksNumbersID")) =>
        }
      }

      "multiple '/ GovernmentAgencyGoodsItem / Packaging' elements are fully present" in {
        val xml = inputXml(
          List(Packaging(Some("TypeCode1"), Some("1"), Some("MarksNumbersID1")), Packaging(Some("TypeCode2"), Some("2"), Some("MarksNumbersID2")))
        )

        val listOfPackageInformation = packageInformationParser.parse(xml).toOption.get

        listOfPackageInformation.size mustBe 2
        listOfPackageInformation.head.get must matchPattern { case PackageInformation(_, Some("TypeCode1"), Some(1), Some("MarksNumbersID1")) =>
        }
        listOfPackageInformation.last.get must matchPattern { case PackageInformation(_, Some("TypeCode2"), Some(2), Some("MarksNumbersID2")) =>
        }
      }

      "a '/ GovernmentAgencyGoodsItem / Packaging' element is present" that {

        "only includes a '/ TypeCode' element" in {
          val xml = inputXml(List(Packaging(typeCode = Some("TypeCode"))))

          packageInformationParser.parse(xml).toOption.get.head.get must matchPattern { case PackageInformation(_, Some("TypeCode"), None, None) =>
          }
        }

        "only includes a '/ QuantityQuantity' element" in {
          val xml = inputXml(List(Packaging(quantityQuantity = Some("3"))))

          packageInformationParser.parse(xml).toOption.get.head.get must matchPattern { case PackageInformation(_, None, Some(3), None) =>
          }
        }

        "only includes a '/ MarksNumbersID' element" in {
          val xml = inputXml(List(Packaging(marksNumbersID = Some("MarksNumbersID"))))

          packageInformationParser.parse(xml).toOption.get.head.get must matchPattern {
            case PackageInformation(_, None, None, Some("MarksNumbersID")) =>
          }
        }

        "only includes '/ TypeCode' and '/ QuantityQuantity' elements" in {
          val xml = inputXml(List(Packaging(typeCode = Some("TypeCode"), quantityQuantity = Some("3"))))

          packageInformationParser.parse(xml).toOption.get.head.get must matchPattern { case PackageInformation(_, Some("TypeCode"), Some(3), None) =>
          }
        }

        "only includes '/ TypeCode' and '/ MarksNumbersID' elements" in {
          val xml = inputXml(List(Packaging(typeCode = Some("TypeCode"), marksNumbersID = Some("MarksNumbersID"))))

          packageInformationParser.parse(xml).toOption.get.head.get must matchPattern {
            case PackageInformation(_, Some("TypeCode"), None, Some("MarksNumbersID")) =>
          }
        }

        "only includes '/ QuantityQuantity' and '/ MarksNumbersID' elements" in {
          val xml = inputXml(List(Packaging(quantityQuantity = Some("3"), marksNumbersID = Some("MarksNumbersID"))))

          packageInformationParser.parse(xml).toOption.get.head.get must matchPattern {
            case PackageInformation(_, None, Some(3), Some("MarksNumbersID")) =>
          }
        }
      }
    }

    "return Left with XmlParserError" when {
      "the value of '/ GovernmentAgencyGoodsItem / Packaging / QuantityQuantity' element is not numeric" in {
        val xml = inputXml(List(Packaging(quantityQuantity = Some("abcxyz"))))

        val result = packageInformationParser.parse(xml)
        result.isLeft mustBe true
        result.left.value must include("abcxyz")
      }
    }
  }

  private case class Packaging(typeCode: Option[String] = None, quantityQuantity: Option[String] = None, marksNumbersID: Option[String] = None)

  private def inputXml(listOfPackaging: List[Packaging] = List.empty): Elem =
    <ns3:GovernmentAgencyGoodsItem>

      {
      listOfPackaging.map { packaging =>
        <ns3:Packaging>

            {
          packaging.typeCode.map { TypeCode =>
            <ns3:TypeCode>{TypeCode}</ns3:TypeCode>
          }.getOrElse(NodeSeq.Empty)
        }

            {
          packaging.quantityQuantity.map { QuantityQuantity =>
            <ns3:QuantityQuantity>{QuantityQuantity}</ns3:QuantityQuantity>
          }.getOrElse(NodeSeq.Empty)
        }

            {
          packaging.marksNumbersID.map { MarksNumbersID =>
            <ns3:MarksNumbersID>{MarksNumbersID}</ns3:MarksNumbersID>
          }.getOrElse(NodeSeq.Empty)
        }

          </ns3:Packaging>
      }
    }

    </ns3:GovernmentAgencyGoodsItem>
}
