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

package unit.uk.gov.hmrc.exports.models.declaration

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration

class ExportsDeclarationSpec extends WordSpec with MustMatchers {

  "Exports Declaration Spec" must {

    import ExportsDeclaration.REST._

    "have json format that parse declaration in vesion 1" in {
      Json
        .parse(ExportsDeclarationSpec.declarationVersion1)
        .validate[ExportsDeclaration]
        .fold(error => fail(s"Could not parse - $error"), declaration => {
          declaration.borderTransport mustNot be(empty)
          declaration.departureTransport mustNot be(empty)
          declaration.transportInformation mustNot be(empty)
        })
    }

    "have json format that parse declaration in version 2" in {
      Json
        .parse(ExportsDeclarationSpec.declarationVersion2)
        .validate[ExportsDeclaration]
        .fold(error => fail(s"Could not parse - $error"), declaration => {
          declaration.borderTransport mustNot be(empty)
          declaration.departureTransport mustNot be(empty)
          declaration.transportInformation mustNot be(empty)
        })
    }

    "have json writes that produce object which could be parsed by first version of reads" in {
      val declaration = Json
        .parse(ExportsDeclarationSpec.declarationVersion2)
        .as[ExportsDeclaration]

      val json = Json.toJson(declaration)

      json
        .validate(ExportsDeclaration.REST.readsVersion1)
        .fold(error => fail(s"Could not parse - $error"), declaration => {
          declaration.borderTransport mustNot be(empty)
          declaration.departureTransport mustNot be(empty)
          declaration.transportInformation mustNot be(empty)
        })
    }
  }
}

object ExportsDeclarationSpec {
  val declarationVersion1: String =
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
      |  "departureTransport": {
      |    "borderModeOfTransportCode": "1",
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "SHIP1"
      |  },
      |  "borderTransport": {
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Boaty McBoatface"
      |  },
      |  "transportInformation": {
      |    "transportPayment": {
      |      "paymentMethod": "H"
      |    },
      |    "containers": [
      |      {
      |        "id": "123456",
      |        "seals": []
      |      }
      |    ]
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
      |    "originationCountry": "GB",
      |    "destinationCountry": "DE",
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      "FR"
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
      |      "circumstancesCode": "No"
      |    },
      |    "supervisingCustomsOffice": {
      |      "supervisingCustomsOffice": "GBLBA001"
      |    },
      |    "warehouseIdentification": {},
      |    "inlandModeOfTransportCode": {
      |      "inlandModeOfTransportCode": "1"
      |    }
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
      |      "documentsProducedData": {
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

  val declarationVersion2: String =
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
      |  "transport": {
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
      |    "borderModeOfTransportCode": "1",
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
      |    "originationCountry": "GB",
      |    "destinationCountry": "DE",
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      "FR"
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
      |      "circumstancesCode": "No"
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
      |      "documentsProducedData": {
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
