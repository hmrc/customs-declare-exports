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

package uk.gov.hmrc.exports.models.declaration

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus._
import uk.gov.hmrc.exports.models.{DeclarationType, Eori}

import java.time.Instant

class ExportsDeclarationSpec extends UnitSpec {

  import ExportsDeclarationSpec._

  "ExportsDeclaration" should {

    "be correctly derived on an update operation from an incoming ExportsDeclaration" in {
      ExportsDeclaration.init(Eori(eori), incomingExportsDeclaration, update = true) mustBe exportsDeclaration
    }

    "generate a new id on a create operation with an incoming ExportsDeclaration" in {
      ExportsDeclaration.init(Eori(eori), incomingExportsDeclaration.copy(id = ""), update = false).id mustNot be("")
    }

    "be set to initial state when without references" in {
      val declaration = incomingExportsDeclaration.copy(consignmentReferences = None)
      ExportsDeclaration.init(Eori(eori), declaration, true).status mustBe INITIAL
    }

    "keep the incoming ExportsDeclaration's status" in {
      val meta = incomingExportsDeclaration.declarationMeta.copy(status = AMENDMENT_DRAFT)
      val declarationRequest = incomingExportsDeclaration.copy(declarationMeta = meta)
      ExportsDeclaration.init(Eori(eori), declarationRequest, true).status mustBe AMENDMENT_DRAFT
    }
  }

  "ExportsDeclaration" must {
    "have json writes that produce object which could be parsed by first version of reads" in {
      val incomingExportsDeclaration = Json
        .parse(incomingExportsDeclarationAsString)
        .as[ExportsDeclaration]

      val declaration = ExportsDeclaration.init(Eori("GB12345678"), incomingExportsDeclaration, true)

      Json
        .toJson(declaration)
        .validate[ExportsDeclaration]
        .fold(
          error => fail(s"Could not parse - $error"),
          declaration => {
            declaration.transport.borderModeOfTransportCode mustNot be(empty)
            declaration.transport.meansOfTransportOnDepartureType mustNot be(empty)
            declaration.transport.transportPayment mustNot be(empty)
            declaration.parties.authorisationProcedureCodeChoice mustNot be(empty)
          }
        )
    }
  }
}

object ExportsDeclarationSpec {

  val eori = "eori"
  val id = "id"

  private val `type` = DeclarationType.STANDARD
  private val createdDate = Instant.MIN
  private val updatedDate = Instant.MAX
  private val additionalDeclarationType = mock[AdditionalDeclarationType]
  private val consignmentReferences = mock[ConsignmentReferences]
  private val mucr = MUCR("CZYX123A")
  private val parties = mock[Parties]
  private val locations = mock[Locations]
  private val item = mock[ExportItem]
  private val totalNumberOfItems = mock[TotalNumberOfItems]
  private val previousDocuments = mock[PreviousDocuments]
  private val natureOfTransaction = mock[NatureOfTransaction]

  val incomingExportsDeclaration: ExportsDeclaration = ExportsDeclaration(
    id = "id",
    declarationMeta = DeclarationMeta(
      parentDeclarationId = Some("parentDeclarationId"),
      parentDeclarationEnhancedStatus = None,
      status = DRAFT,
      createdDateTime = createdDate,
      updatedDateTime = updatedDate,
      summaryWasVisited = Some(true),
      readyForSubmission = Some(true),
      maxSequenceIds = Map("dummy" -> -1)
    ),
    eori = "",
    `type` = `type`,
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    linkDucrToMucr = Some(YesNoAnswer.yes),
    mucr = Some(mucr),
    transport = Transport(),
    parties = parties,
    locations = locations,
    items = Seq(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction),
    statementDescription = None
  )

  val exportsDeclaration: ExportsDeclaration = ExportsDeclaration(
    id = id,
    declarationMeta = DeclarationMeta(
      status = DRAFT,
      createdDateTime = createdDate,
      updatedDateTime = updatedDate,
      parentDeclarationId = Some("parentDeclarationId"),
      summaryWasVisited = Some(true),
      readyForSubmission = Some(true),
      maxSequenceIds = Map("dummy" -> -1)
    ),
    eori = eori,
    `type` = `type`,
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    linkDucrToMucr = Some(YesNoAnswer.yes),
    mucr = Some(mucr),
    transport = Transport(),
    parties = parties,
    locations = locations,
    items = Seq(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction),
    statementDescription = None
  )

  val incomingExportsDeclarationAsString: String =
    """{
      |  "id": "6f31582e-bfd5-4b27-90be-2dca6e236b20",
      |  "declarationMeta": {
      |    "status": "DRAFT",
      |    "createdDateTime": "2019-12-10T15:52:32.681Z",
      |    "updatedDateTime": "2019-12-10T15:53:13.697Z",
      |    "parentDeclarationId": "parentDeclarationId",
      |    "summaryWasVisited": true,
      |    "readyForSubmission": true,
      |    "maxSequenceIds": {
      |      "dummy": -1,
      |      "Containers": 1,
      |      "RoutingCountries": 1,
      |      "Seals": 0
      |    }
      |  },
      |  "eori" : "",
      |  "status": "DRAFT",
      |  "type": "STANDARD",
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
      |        "sequenceId": 1,
      |        "id": "123456",
      |        "seals": []
      |      }
      |    ],
      |    "inlandModeOfTransportCode": "1",
      |    "transportCrossingTheBorderNationality": { "countryCode": "United Kingdom" },
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
      |    "authorisationProcedureCodeChoice" : {
      |      "code" : "Code1040"
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
      |        "sequenceId": 1,
      |        "country": {
      |          "code" : "FR"
      |        }
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
      |        "combinedNomenclatureCode": "4602191000",
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
      |          "sequenceId": 1,
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
