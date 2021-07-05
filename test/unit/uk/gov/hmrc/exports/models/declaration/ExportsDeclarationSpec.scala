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

package uk.gov.hmrc.exports.models.declaration

import java.time.Instant

import org.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers.yes
import uk.gov.hmrc.exports.models.{DeclarationType, Eori}

class ExportsDeclarationSpec extends UnitSpec {

  import ExportsDeclarationSpec._

  "ExportsDeclaration" should {
    "be correctly derived from ExportsDeclarationRequest" in {
      ExportsDeclaration(id, Eori(eori), exportsDeclarationRequest) mustBe exportsDeclaration
    }

    "be set to initial state when without references" in {
      ExportsDeclaration(id, Eori(eori), exportsDeclarationRequest.copy(consignmentReferences = None)).status mustBe DeclarationStatus.INITIAL
    }
  }

  "ExportsDeclaration" must {
    "have json writes that produce object which could be parsed by first version of reads" in {
      val exportsDeclarationRequest = Json
        .parse(exportsDeclarationRequestAsString)
        .as(ExportsDeclarationRequest.format)

      val declaration = ExportsDeclaration("1", Eori("GB12345678"), exportsDeclarationRequest)

      val json = Json.toJson(declaration)(ExportsDeclaration.REST.writes)

      json
        .validate(ExportsDeclarationRequest.format)
        .fold(error => fail(s"Could not parse - $error"), declaration => {
          declaration.transport.borderModeOfTransportCode mustNot be(empty)
          declaration.transport.meansOfTransportOnDepartureType mustNot be(empty)
          declaration.transport.transportPayment mustNot be(empty)
        })
    }
  }
}

object ExportsDeclarationSpec {

  val eori = "eori"
  val id = "id"

  private val `type` = DeclarationType.STANDARD
  private val createdDate = Instant.MIN
  private val updatedDate = Instant.MAX
  private val sourceId = "source-id"
  private val dispatchLocation = mock[DispatchLocation]
  private val additionalDeclarationType = mock[AdditionalDeclarationType]
  private val consignmentReferences = mock[ConsignmentReferences]
  private val mucr = MUCR("CZYX123A")
  private val transport = mock[Transport]
  private val parties = mock[Parties]
  private val locations = mock[Locations]
  private val item = mock[ExportItem]
  private val totalNumberOfItems = mock[TotalNumberOfItems]
  private val previousDocuments = mock[PreviousDocuments]
  private val natureOfTransaction = mock[NatureOfTransaction]

  val exportsDeclarationRequest = ExportsDeclarationRequest(
    createdDateTime = createdDate,
    updatedDateTime = updatedDate,
    sourceId = Some(sourceId),
    `type` = `type`,
    dispatchLocation = Some(dispatchLocation),
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    linkDucrToMucr = Some(YesNoAnswer(yes)),
    mucr = Some(mucr),
    transport = transport,
    parties = parties,
    locations = locations,
    items = Seq(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction)
  )

  val exportsDeclaration = ExportsDeclaration(
    id = id,
    eori = eori,
    status = DeclarationStatus.DRAFT,
    createdDateTime = createdDate,
    updatedDateTime = updatedDate,
    sourceId = Some(sourceId),
    `type` = `type`,
    dispatchLocation = Some(dispatchLocation),
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    linkDucrToMucr = Some(YesNoAnswer(yes)),
    mucr = Some(mucr),
    transport = transport,
    parties = parties,
    locations = locations,
    items = Seq(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction)
  )

  val exportsDeclarationRequestAsString: String =
    """{
      |  "id": "6f31582e-bfd5-4b27-90be-2dca6e236b20",
      |  "eori": "GB7172755078551",
      |  "status": "DRAFT",
      |  "createdDateTime": "2019-12-10T15:52:32.681Z",
      |  "updatedDateTime": "2019-12-10T15:53:13.697Z",
      |  "type": "STANDARD",
      |  "dispatchLocation": {
      |    "dispatchLocation": "EX"
      |  },
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |    "ducr": {
      |      "ducr": "8GB123451068100-101SHIP1"
      |    },
      |    "lrn": "QSLRN7285100"
      |  },
      |  "linkDucrToMucr": {
      |    "answer": "Yes"
      |  },
      |  "mucr": {
      |    "mucr": "CZYX123A"
      |  },
      |  "transport": {
      |    "expressConsignment": {
      |      "answer": "Yes"
      |    },
      |    "transportPayment": {
      |      "paymentMethod": "H"
      |    },
      |    "containers": [
      |      {
      |        "id": "123456",
      |        "seals": []
      |      }
      |    ],
      |    "inlandModeOfTransportCode": "1",
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Boaty McBoatface",
      |    "borderModeOfTransportCode": {
      |       "code": "1"
      |    },
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "SHIP1"
      |  },
      |  "parties": {
      |    "exporterDetails": {
      |      "details": {
      |        "eori": "GB717572504502801"
      |      }
      |    },
      |    "consigneeDetails": {
      |      "details": {
      |        "address": {
      |          "fullName": "Bags Export",
      |          "addressLine": "1 Bags Avenue",
      |          "townOrCity": "New York",
      |          "postCode": "NA",
      |          "country": "United States of America"
      |        }
      |      }
      |    },
      |    "declarantDetails": {
      |      "details": {
      |        "eori": "GB717572504502811"
      |      }
      |    },
      |    "representativeDetails": {
      |      "details": {
      |        "eori": "GB717572504502809"
      |      },
      |      "statusCode": "3"
      |    },
      |    "declarationHoldersData": {
      |      "holders": [
      |        {
      |          "authorisationTypeCode": "AEOC",
      |          "eori": "GB717572504502811"
      |        }
      |      ]
      |    },
      |    "carrierDetails": {
      |      "details": {
      |        "address": {
      |          "fullName": "XYZ Carrier",
      |          "addressLine": "School Road",
      |          "townOrCity": "London",
      |          "postCode": "WS1 2AB",
      |          "country": "United Kingdom"
      |        }
      |      }
      |    }
      |  },
      |  "locations": {
      |    "originationCountry": {
      |      "code" : "GB"
      |    },
      |    "destinationCountry": {
      |      "code" : "DE"
      |    },
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      {
      |        "code" : "FR"
      |      }
      |    ],
      |    "goodsLocation": {
      |      "country": "United Kingdom",
      |      "typeOfLocation": "A",
      |      "qualifierOfIdentification": "U",
      |      "identificationOfLocation": "FXTFXTFXT",
      |      "additionalIdentifier": "123"
      |    },
      |    "officeOfExit": {
      |      "officeId": "GB000054",
      |      "isUkOfficeOfExit": "Yes"
      |    },
      |    "supervisingCustomsOffice": {
      |      "supervisingCustomsOffice": "GBLBA001"
      |    },
      |    "warehouseIdentification": {}
      |  },
      |  "items": [
      |    {
      |      "id": "b12agbfd",
      |      "sequenceId": 1,
      |      "procedureCodes": {
      |        "procedureCode": "1040",
      |        "additionalProcedureCodes": [
      |          "000"
      |        ]
      |      },
      |      "fiscalInformation": {
      |        "onwardSupplyRelief": "No"
      |      },
      |      "statisticalValue": {
      |        "statisticalValue": "1000"
      |      },
      |      "commodityDetails": {
      |        "combinedNomenclatureCode": "46021910",
      |        "descriptionOfGoods": "Straw for bottles"
      |      },
      |      "dangerousGoodsCode": {
      |        "dangerousGoodsCode": "1234"
      |      },
      |      "cusCode": {
      |        "cusCode": "12345678"
      |      },
      |      "taricCodes": [],
      |      "nactCodes": [],
      |      "packageInformation": [
      |        {
      |          "id": "12345678",
      |          "typesOfPackages": "PK",
      |          "numberOfPackages": 10,
      |          "shippingMarks": "RICH123"
      |        }
      |      ],
      |      "commodityMeasure": {
      |        "supplementaryUnits": "10",
      |        "netMass": "500",
      |        "grossMass": "700"
      |      },
      |      "additionalInformation": {
      |        "items": [
      |          {
      |            "code": "00400",
      |            "description": "EXPORTER"
      |          }
      |        ]
      |      },
      |      "additionalDocuments": {
      |        "isRequired" : {
      |            "answer" : "Yes"
      |        },
      |        "documents": [
      |          {
      |            "documentTypeCode": "C501",
      |            "documentIdentifier": "GBAEOC717572504502811"
      |          }
      |        ]
      |      }
      |    }
      |  ],
      |  "totalNumberOfItems": {
      |    "totalAmountInvoiced": "56764",
      |    "exchangeRate": "1.49",
      |    "totalPackage": "1"
      |  },
      |  "previousDocuments": {
      |    "documents": [
      |      {
      |        "documentCategory": "Y",
      |        "documentType": "IF3",
      |        "documentReference": "101SHIP2"
      |      }
      |    ]
      |  },
      |  "natureOfTransaction": {
      |    "natureType": "1"
      |  }
      |}""".stripMargin
}
