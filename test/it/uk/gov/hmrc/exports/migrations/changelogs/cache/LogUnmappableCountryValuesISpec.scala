package uk.gov.hmrc.exports.migrations.changelogs.cache

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.services.CountriesService

class LogUnmappableCountryValuesISpec extends IntegrationTestMigrationToolSpec {

  private val countriesService = app.injector.instanceOf[CountriesService]
  override val collectionUnderTest = "declarations"
  override val changeLog = new LogUnmappableCountryValues(countriesService)

  "LogUnmappableCountryValues migration" should {

    import LogUnmappableCountryValuesISpec._

    "not alter documents" in {
      runTest(declarationBeforeMigration, declarationBeforeMigration)
    }

    "not alter documents when contents already a country code" in {
      runTest(declarationBeforeMigrationWithCountryCode, declarationBeforeMigrationWithCountryCode)
    }

    "don't rename field and log a warning about unmappable values (check manually)" in {
      runTest(declarationBeforeMigrationWithUnknownCountry, declarationBeforeMigrationWithUnknownCountry)
    }

    "not alter any documents if already migrated" in {
      runTest(declarationAfterMigration, declarationAfterMigration)
    }
  }
}

object LogUnmappableCountryValuesISpec {

  val declarationBeforeMigration =
    """{
      |  "_id": { "$oid": "63f875806832ef2e3e4ed2ca" },
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "declarationMeta": {
      |      "status": "DRAFT",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true,
      |      "maxSequenceIds": {
      |          "dummy": -1,
      |          "Containers": 1,
      |          "PackageInformation": 3
      |      }
      |  },
      |  "eori": "GB7172755049071",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "8GB123456944196-101SHIP1"
      |      },
      |      "lrn": "QSLRN450100"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "CZYX123A"
      |  },
      |  "transport": {
      |      "expressConsignment": {
      |          "answer": "Yes"
      |      },
      |      "transportPayment": {
      |          "paymentMethod": "H"
      |      },
      |      "containers": [
      |          {
      |              "sequenceId": 1,
      |              "id": "123456",
      |              "seals": []
      |          }
      |      ],
      |      "borderModeOfTransportCode": {
      |          "code": "6"
      |      },
      |      "meansOfTransportOnDepartureType": "10",
      |      "meansOfTransportOnDepartureIDNumber": "8888",
      |      "transportCrossingTheBorderNationality": {
      |          "countryName": "South Africa"
      |      },
      |      "meansOfTransportCrossingTheBorderType": "11",
      |      "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
      |  },
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "United States of America (the), Including Puerto Rico"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "declarantIsExporter": {
      |          "answer": "Yes"
      |      },
      |      "declarationAdditionalActorsData": {
      |          "actors": []
      |      },
      |      "declarationHoldersData": {
      |          "holders": [
      |              {
      |                  "authorisationTypeCode": "AEOC",
      |                  "eori": "GB717572504502801",
      |                  "eoriSource": "OtherEori"
      |              }
      |          ],
      |          "isRequired": {
      |              "answer": "Yes"
      |          }
      |      },
      |      "authorisationProcedureCodeChoice": {
      |          "code": "Code1040"
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "United Kingdom, Great Britain, Northern Ireland"
      |              }
      |          }
      |      }
      |  },
      |  "locations": {
      |      "originationCountry": {
      |          "code": "GB"
      |      },
      |      "destinationCountry": {
      |          "code": "US"
      |      },
      |      "hasRoutingCountries": false,
      |      "routingCountries": [],
      |      "goodsLocation": {
      |          "country": "GB",
      |          "typeOfLocation": "A",
      |          "qualifierOfIdentification": "U",
      |          "identificationOfLocation": "ABDABDABDGVM"
      |      },
      |      "officeOfExit": {
      |          "officeId": "GB000434"
      |      },
      |      "inlandModeOfTransportCode": {
      |          "inlandModeOfTransportCode": "3"
      |      }
      |  },
      |  "items": [
      |      {
      |          "id": "80573c51",
      |          "sequenceId": 1,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 1,
      |                  "id": "fjekdnxx",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              },
      |              {
      |                  "sequenceId": 2,
      |                  "id": "wg6bzjpz",
      |                  "typesOfPackages": "AE",
      |                  "numberOfPackages": 3,
      |                  "shippingMarks": "Shipping Mark2"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "04bff564",
      |          "sequenceId": 2,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 3,
      |                  "id": "teowcod9",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "3d4567ab",
      |          "sequenceId": 3,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "c2e4365g",
      |          "sequenceId": 4,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      }
      |  ],
      |  "totalNumberOfItems": {
      |      "totalAmountInvoiced": "567640",
      |      "totalAmountInvoicedCurrency": "GBP",
      |      "agreedExchangeRate": "Yes",
      |      "exchangeRate": "1.49",
      |      "totalPackage": "1"
      |  },
      |  "previousDocuments": {
      |      "documents": [
      |          {
      |              "documentType": "DCS",
      |              "documentReference": "9GB123456782317-BH1433A61"
      |          }
      |      ]
      |  },
      |  "natureOfTransaction": {
      |      "natureType": "1"
      |  }
      |}""".stripMargin

  val declarationBeforeMigrationWithUnknownCountry =
    """{
      |  "_id": { "$oid": "63f875806832ef2e3e4ed2ca" },
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "declarationMeta": {
      |      "status": "DRAFT",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true,
      |      "maxSequenceIds": {
      |          "dummy": -1,
      |          "Containers": 1,
      |          "PackageInformation": 3
      |      }
      |  },
      |  "eori": "GB7172755049071",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "8GB123456944196-101SHIP1"
      |      },
      |      "lrn": "QSLRN450100"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "CZYX123A"
      |  },
      |  "transport": {
      |      "expressConsignment": {
      |          "answer": "Yes"
      |      },
      |      "transportPayment": {
      |          "paymentMethod": "H"
      |      },
      |      "containers": [
      |          {
      |              "sequenceId": 1,
      |              "id": "123456",
      |              "seals": []
      |          }
      |      ],
      |      "borderModeOfTransportCode": {
      |          "code": "6"
      |      },
      |      "meansOfTransportOnDepartureType": "10",
      |      "meansOfTransportOnDepartureIDNumber": "8888",
      |      "transportCrossingTheBorderNationality": {
      |          "countryName": "Unknown country"
      |      },
      |      "meansOfTransportCrossingTheBorderType": "11",
      |      "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
      |  },
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "United States of America (the), Including Puerto Rico"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "declarantIsExporter": {
      |          "answer": "Yes"
      |      },
      |      "declarationAdditionalActorsData": {
      |          "actors": []
      |      },
      |      "declarationHoldersData": {
      |          "holders": [
      |              {
      |                  "authorisationTypeCode": "AEOC",
      |                  "eori": "GB717572504502801",
      |                  "eoriSource": "OtherEori"
      |              }
      |          ],
      |          "isRequired": {
      |              "answer": "Yes"
      |          }
      |      },
      |      "authorisationProcedureCodeChoice": {
      |          "code": "Code1040"
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "United Kingdom, Great Britain, Northern Ireland"
      |              }
      |          }
      |      }
      |  },
      |  "locations": {
      |      "originationCountry": {
      |          "code": "GB"
      |      },
      |      "destinationCountry": {
      |          "code": "US"
      |      },
      |      "hasRoutingCountries": false,
      |      "routingCountries": [],
      |      "goodsLocation": {
      |          "country": "GB",
      |          "typeOfLocation": "A",
      |          "qualifierOfIdentification": "U",
      |          "identificationOfLocation": "ABDABDABDGVM"
      |      },
      |      "officeOfExit": {
      |          "officeId": "GB000434"
      |      },
      |      "inlandModeOfTransportCode": {
      |          "inlandModeOfTransportCode": "3"
      |      }
      |  },
      |  "items": [
      |      {
      |          "id": "80573c51",
      |          "sequenceId": 1,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 1,
      |                  "id": "fjekdnxx",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              },
      |              {
      |                  "sequenceId": 2,
      |                  "id": "wg6bzjpz",
      |                  "typesOfPackages": "AE",
      |                  "numberOfPackages": 3,
      |                  "shippingMarks": "Shipping Mark2"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "04bff564",
      |          "sequenceId": 2,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 3,
      |                  "id": "teowcod9",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "3d4567ab",
      |          "sequenceId": 3,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "c2e4365g",
      |          "sequenceId": 4,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      }
      |  ],
      |  "totalNumberOfItems": {
      |      "totalAmountInvoiced": "567640",
      |      "totalAmountInvoicedCurrency": "GBP",
      |      "agreedExchangeRate": "Yes",
      |      "exchangeRate": "1.49",
      |      "totalPackage": "1"
      |  },
      |  "previousDocuments": {
      |      "documents": [
      |          {
      |              "documentType": "DCS",
      |              "documentReference": "9GB123456782317-BH1433A61"
      |          }
      |      ]
      |  },
      |  "natureOfTransaction": {
      |      "natureType": "1"
      |  }
      |}""".stripMargin

  val declarationBeforeMigrationWithCountryCode =
    """{
      |  "_id": { "$oid": "63f875806832ef2e3e4ed2ca" },
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "declarationMeta": {
      |      "status": "DRAFT",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true,
      |      "maxSequenceIds": {
      |          "dummy": -1,
      |          "Containers": 1,
      |          "PackageInformation": 3
      |      }
      |  },
      |  "eori": "GB7172755049071",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "8GB123456944196-101SHIP1"
      |      },
      |      "lrn": "QSLRN450100"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "CZYX123A"
      |  },
      |  "transport": {
      |      "expressConsignment": {
      |          "answer": "Yes"
      |      },
      |      "transportPayment": {
      |          "paymentMethod": "H"
      |      },
      |      "containers": [
      |          {
      |              "sequenceId": 1,
      |              "id": "123456",
      |              "seals": []
      |          }
      |      ],
      |      "borderModeOfTransportCode": {
      |          "code": "6"
      |      },
      |      "meansOfTransportOnDepartureType": "10",
      |      "meansOfTransportOnDepartureIDNumber": "8888",
      |      "transportCrossingTheBorderNationality": {
      |          "countryName": "ZA"
      |      },
      |      "meansOfTransportCrossingTheBorderType": "11",
      |      "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
      |  },
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "United States of America (the), Including Puerto Rico"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "declarantIsExporter": {
      |          "answer": "Yes"
      |      },
      |      "declarationAdditionalActorsData": {
      |          "actors": []
      |      },
      |      "declarationHoldersData": {
      |          "holders": [
      |              {
      |                  "authorisationTypeCode": "AEOC",
      |                  "eori": "GB717572504502801",
      |                  "eoriSource": "OtherEori"
      |              }
      |          ],
      |          "isRequired": {
      |              "answer": "Yes"
      |          }
      |      },
      |      "authorisationProcedureCodeChoice": {
      |          "code": "Code1040"
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "United Kingdom, Great Britain, Northern Ireland"
      |              }
      |          }
      |      }
      |  },
      |  "locations": {
      |      "originationCountry": {
      |          "code": "GB"
      |      },
      |      "destinationCountry": {
      |          "code": "US"
      |      },
      |      "hasRoutingCountries": false,
      |      "routingCountries": [],
      |      "goodsLocation": {
      |          "country": "GB",
      |          "typeOfLocation": "A",
      |          "qualifierOfIdentification": "U",
      |          "identificationOfLocation": "ABDABDABDGVM"
      |      },
      |      "officeOfExit": {
      |          "officeId": "GB000434"
      |      },
      |      "inlandModeOfTransportCode": {
      |          "inlandModeOfTransportCode": "3"
      |      }
      |  },
      |  "items": [
      |      {
      |          "id": "80573c51",
      |          "sequenceId": 1,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 1,
      |                  "id": "fjekdnxx",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              },
      |              {
      |                  "sequenceId": 2,
      |                  "id": "wg6bzjpz",
      |                  "typesOfPackages": "AE",
      |                  "numberOfPackages": 3,
      |                  "shippingMarks": "Shipping Mark2"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "04bff564",
      |          "sequenceId": 2,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 3,
      |                  "id": "teowcod9",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "3d4567ab",
      |          "sequenceId": 3,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "c2e4365g",
      |          "sequenceId": 4,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      }
      |  ],
      |  "totalNumberOfItems": {
      |      "totalAmountInvoiced": "567640",
      |      "totalAmountInvoicedCurrency": "GBP",
      |      "agreedExchangeRate": "Yes",
      |      "exchangeRate": "1.49",
      |      "totalPackage": "1"
      |  },
      |  "previousDocuments": {
      |      "documents": [
      |          {
      |              "documentType": "DCS",
      |              "documentReference": "9GB123456782317-BH1433A61"
      |          }
      |      ]
      |  },
      |  "natureOfTransaction": {
      |      "natureType": "1"
      |  }
      |}""".stripMargin

  val declarationAfterMigration =
    """{
      |  "_id": { "$oid": "63f875806832ef2e3e4ed2ca" },
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "declarationMeta": {
      |      "status": "DRAFT",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true,
      |      "maxSequenceIds": {
      |          "dummy": -1,
      |          "Containers": 1,
      |          "PackageInformation": 3
      |      }
      |  },
      |  "eori": "GB7172755049071",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "8GB123456944196-101SHIP1"
      |      },
      |      "lrn": "QSLRN450100"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "CZYX123A"
      |  },
      |  "transport": {
      |      "expressConsignment": {
      |          "answer": "Yes"
      |      },
      |      "transportPayment": {
      |          "paymentMethod": "H"
      |      },
      |      "containers": [
      |          {
      |              "sequenceId": 1,
      |              "id": "123456",
      |              "seals": []
      |          }
      |      ],
      |      "borderModeOfTransportCode": {
      |          "code": "6"
      |      },
      |      "meansOfTransportOnDepartureType": "10",
      |      "meansOfTransportOnDepartureIDNumber": "8888",
      |      "transportCrossingTheBorderNationality": {
      |          "countryCode": "ZA"
      |      },
      |      "meansOfTransportCrossingTheBorderType": "11",
      |      "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
      |  },
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "United States of America (the), Including Puerto Rico"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "declarantIsExporter": {
      |          "answer": "Yes"
      |      },
      |      "declarationAdditionalActorsData": {
      |          "actors": []
      |      },
      |      "declarationHoldersData": {
      |          "holders": [
      |              {
      |                  "authorisationTypeCode": "AEOC",
      |                  "eori": "GB717572504502801",
      |                  "eoriSource": "OtherEori"
      |              }
      |          ],
      |          "isRequired": {
      |              "answer": "Yes"
      |          }
      |      },
      |      "authorisationProcedureCodeChoice": {
      |          "code": "Code1040"
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "United Kingdom, Great Britain, Northern Ireland"
      |              }
      |          }
      |      }
      |  },
      |  "locations": {
      |      "originationCountry": {
      |          "code": "GB"
      |      },
      |      "destinationCountry": {
      |          "code": "US"
      |      },
      |      "hasRoutingCountries": false,
      |      "routingCountries": [],
      |      "goodsLocation": {
      |          "country": "GB",
      |          "typeOfLocation": "A",
      |          "qualifierOfIdentification": "U",
      |          "identificationOfLocation": "ABDABDABDGVM"
      |      },
      |      "officeOfExit": {
      |          "officeId": "GB000434"
      |      },
      |      "inlandModeOfTransportCode": {
      |          "inlandModeOfTransportCode": "3"
      |      }
      |  },
      |  "items": [
      |      {
      |          "id": "80573c51",
      |          "sequenceId": 1,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 1,
      |                  "id": "fjekdnxx",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              },
      |              {
      |                  "sequenceId": 2,
      |                  "id": "wg6bzjpz",
      |                  "typesOfPackages": "AE",
      |                  "numberOfPackages": 3,
      |                  "shippingMarks": "Shipping Mark2"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "04bff564",
      |          "sequenceId": 2,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 3,
      |                  "id": "teowcod9",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "3d4567ab",
      |          "sequenceId": 3,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "c2e4365g",
      |          "sequenceId": 4,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      }
      |  ],
      |  "totalNumberOfItems": {
      |      "totalAmountInvoiced": "567640",
      |      "totalAmountInvoicedCurrency": "GBP",
      |      "agreedExchangeRate": "Yes",
      |      "exchangeRate": "1.49",
      |      "totalPackage": "1"
      |  },
      |  "previousDocuments": {
      |      "documents": [
      |          {
      |              "documentType": "DCS",
      |              "documentReference": "9GB123456782317-BH1433A61"
      |          }
      |      ]
      |  },
      |  "natureOfTransaction": {
      |      "natureType": "1"
      |  }
      |}""".stripMargin

  val declarationAfterMigrationWithUnknownCountry =
    """{
      |  "_id": { "$oid": "63f875806832ef2e3e4ed2ca" },
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "declarationMeta": {
      |      "status": "DRAFT",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true,
      |      "maxSequenceIds": {
      |          "dummy": -1,
      |          "Containers": 1,
      |          "PackageInformation": 3
      |      }
      |  },
      |  "eori": "GB7172755049071",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "8GB123456944196-101SHIP1"
      |      },
      |      "lrn": "QSLRN450100"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "CZYX123A"
      |  },
      |  "transport": {
      |      "expressConsignment": {
      |          "answer": "Yes"
      |      },
      |      "transportPayment": {
      |          "paymentMethod": "H"
      |      },
      |      "containers": [
      |          {
      |              "sequenceId": 1,
      |              "id": "123456",
      |              "seals": []
      |          }
      |      ],
      |      "borderModeOfTransportCode": {
      |          "code": "6"
      |      },
      |      "meansOfTransportOnDepartureType": "10",
      |      "meansOfTransportOnDepartureIDNumber": "8888",
      |      "transportCrossingTheBorderNationality": {
      |          "countryCode": "Unknown country"
      |      },
      |      "meansOfTransportCrossingTheBorderType": "11",
      |      "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
      |  },
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "United States of America (the), Including Puerto Rico"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "declarantIsExporter": {
      |          "answer": "Yes"
      |      },
      |      "declarationAdditionalActorsData": {
      |          "actors": []
      |      },
      |      "declarationHoldersData": {
      |          "holders": [
      |              {
      |                  "authorisationTypeCode": "AEOC",
      |                  "eori": "GB717572504502801",
      |                  "eoriSource": "OtherEori"
      |              }
      |          ],
      |          "isRequired": {
      |              "answer": "Yes"
      |          }
      |      },
      |      "authorisationProcedureCodeChoice": {
      |          "code": "Code1040"
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "United Kingdom, Great Britain, Northern Ireland"
      |              }
      |          }
      |      }
      |  },
      |  "locations": {
      |      "originationCountry": {
      |          "code": "GB"
      |      },
      |      "destinationCountry": {
      |          "code": "US"
      |      },
      |      "hasRoutingCountries": false,
      |      "routingCountries": [],
      |      "goodsLocation": {
      |          "country": "GB",
      |          "typeOfLocation": "A",
      |          "qualifierOfIdentification": "U",
      |          "identificationOfLocation": "ABDABDABDGVM"
      |      },
      |      "officeOfExit": {
      |          "officeId": "GB000434"
      |      },
      |      "inlandModeOfTransportCode": {
      |          "inlandModeOfTransportCode": "3"
      |      }
      |  },
      |  "items": [
      |      {
      |          "id": "80573c51",
      |          "sequenceId": 1,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 1,
      |                  "id": "fjekdnxx",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              },
      |              {
      |                  "sequenceId": 2,
      |                  "id": "wg6bzjpz",
      |                  "typesOfPackages": "AE",
      |                  "numberOfPackages": 3,
      |                  "shippingMarks": "Shipping Mark2"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "04bff564",
      |          "sequenceId": 2,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [
      |              {
      |                  "sequenceId": 3,
      |                  "id": "teowcod9",
      |                  "typesOfPackages": "XD",
      |                  "numberOfPackages": 10,
      |                  "shippingMarks": "Shipping description"
      |              }
      |          ],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "3d4567ab",
      |          "sequenceId": 3,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "packageInformation": [],
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      },
      |      {
      |          "id": "c2e4365g",
      |          "sequenceId": 4,
      |          "procedureCodes": {
      |              "procedureCode": "1040",
      |              "additionalProcedureCodes": [
      |                  "000"
      |              ]
      |          },
      |          "statisticalValue": {
      |              "statisticalValue": "1000"
      |          },
      |          "commodityDetails": {
      |              "combinedNomenclatureCode": "4106920000",
      |              "descriptionOfGoods": "Straw for bottles"
      |          },
      |          "dangerousGoodsCode": {},
      |          "taricCodes": [],
      |          "nactCodes": [],
      |          "nactExemptionCode": {
      |              "nactCode": "VATZ"
      |          },
      |          "commodityMeasure": {
      |              "supplementaryUnits": "10",
      |              "supplementaryUnitsNotRequired": false,
      |              "netMass": "500",
      |              "grossMass": "700"
      |          },
      |          "additionalInformation": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "items": [
      |                  {
      |                      "code": "00400",
      |                      "description": "EXPORTER"
      |                  }
      |              ]
      |          },
      |          "additionalDocuments": {
      |              "isRequired": {
      |                  "answer": "Yes"
      |              },
      |              "documents": [
      |                  {
      |                      "documentTypeCode": "C501",
      |                      "documentIdentifier": "GBAEOC717572504502801"
      |                  }
      |              ]
      |          },
      |          "isLicenceRequired": true
      |      }
      |  ],
      |  "totalNumberOfItems": {
      |      "totalAmountInvoiced": "567640",
      |      "totalAmountInvoicedCurrency": "GBP",
      |      "agreedExchangeRate": "Yes",
      |      "exchangeRate": "1.49",
      |      "totalPackage": "1"
      |  },
      |  "previousDocuments": {
      |      "documents": [
      |          {
      |              "documentType": "DCS",
      |              "documentReference": "9GB123456782317-BH1433A61"
      |          }
      |      ]
      |  },
      |  "natureOfTransaction": {
      |      "natureType": "1"
      |  }
      |}""".stripMargin

}
