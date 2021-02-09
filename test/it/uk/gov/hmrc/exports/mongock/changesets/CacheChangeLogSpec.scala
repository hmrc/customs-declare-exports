package uk.gov.hmrc.exports.mongock.changesets

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.{MongoClient, MongoClientURI}
import org.bson.Document
import org.mockito.Mockito
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.generators.IdGenerator
import uk.gov.hmrc.exports.mongock.changesets.CacheChangeLogSpec._

class CacheChangeLogSpec extends UnitSpec with GuiceOneServerPerSuite with IntegrationPatience {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(mongoConfiguration)
      .build()

  private val MongoURI = mongoConfiguration.get[String]("mongodb.uri")
  private val DatabaseName = TestMongoDB.DatabaseName
  private val CollectionName = "declarations"

  private implicit val mongoDatabase: MongoDatabase = {
    val uri = new MongoClientURI(MongoURI.replaceAllLiterally("sslEnabled", "ssl"))
    val client = new MongoClient(uri)

    client.getDatabase(DatabaseName)
  }

  private val changeLog = new CacheChangeLog()

  private val idGenerator = mock[IdGenerator[String]]
  changeLog.setIdGenerator(idGenerator)

  override def beforeEach(): Unit = {
    super.beforeEach()
    mongoDatabase.getCollection(CollectionName).drop()
    Mockito.when(idGenerator.generateId()).thenReturn(testId)
  }

  override def afterEach(): Unit = {
    mongoDatabase.getCollection(CollectionName).drop()
    super.afterEach()
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = mongoDatabase.getCollection(CollectionName)

  "CacheChangeLog" should {

    "correctly migrate data" when {

      "running ChangeSet no. 001" in {

        runTest(testDataBeforeChangeSet_1, testDataAfterChangeSet_1)(changeLog.dbBaseline)
      }

      "running ChangeSet no. 002" in {

        runTest(testDataBeforeChangeSet_2, testDataAfterChangeSet_2)(changeLog.updateAllCountriesNameToCodesForLocationPage)
      }

      "running ChangeSet no. 003" in {

        runTest(testDataBeforeChangeSet_3, testDataAfterChangeSet_3)(changeLog.changeOriginationCountryStructure)
      }

      "running ChangeSet no. 004" in {

        runTest(testDataBeforeChangeSet_4, testDataAfterChangeSet_4)(changeLog.changeDestinationCountryStructure)
      }

      "running ChangeSet no. 005" in {

        runTest(testDataBeforeChangeSet_5, testDataAfterChangeSet_5)(changeLog.changeRoutingCountriesStructure)
      }

      "running ChangeSet no. 006" in {

        runTest(testDataBeforeChangeSet_6, testDataAfterChangeSet_6)(changeLog.updateTransportBorderModeOfTransportCode)
      }

      "running ChangeSet no. 007" in {

        runTest(testDataBeforeChangeSet_7, testDataAfterChangeSet_7)(changeLog.addIdFieldToPackageInformation)
      }
    }

    "not change data already migrated" when {

      "running ChangeSet no. 001" in {

        runTest(testDataAfterChangeSet_1, testDataAfterChangeSet_1)(changeLog.dbBaseline)
      }

      "running ChangeSet no. 002" in {

        runTest(testDataAfterChangeSet_2, testDataAfterChangeSet_2)(changeLog.updateAllCountriesNameToCodesForLocationPage)
      }

      "running ChangeSet no. 003" in {

        runTest(testDataAfterChangeSet_3, testDataAfterChangeSet_3)(changeLog.changeOriginationCountryStructure)
      }

      "running ChangeSet no. 004" in {

        runTest(testDataAfterChangeSet_4, testDataAfterChangeSet_4)(changeLog.changeDestinationCountryStructure)
      }

      "running ChangeSet no. 005" in {

        runTest(testDataAfterChangeSet_5, testDataAfterChangeSet_5)(changeLog.changeRoutingCountriesStructure)
      }

      "running ChangeSet no. 006" in {

        runTest(testDataAfterChangeSet_6, testDataAfterChangeSet_6)(changeLog.updateTransportBorderModeOfTransportCode)
      }

      "running ChangeSet no. 007" in {

        runTest(testDataAfterChangeSet_7, testDataAfterChangeSet_7)(changeLog.addIdFieldToPackageInformation)
      }
    }
  }

  private def runTest(inputDataJson: String, expectedDataJson: String)(test: MongoDatabase => Unit)(implicit mongoDatabase: MongoDatabase): Unit = {
    getDeclarationsCollection(mongoDatabase).insertOne(Document.parse(inputDataJson))

    test(mongoDatabase)

    val result: Document = getDeclarationsCollection(mongoDatabase).find().first()
    val expectedResult: String = expectedDataJson

    compareJson(result.toJson, expectedResult)
  }

  private def compareJson(actual: String, expected: String): Unit = {
    val mapper = new ObjectMapper

    val jsonActual = mapper.readTree(actual)
    val jsonExpected = mapper.readTree(expected)

    jsonActual mustBe jsonExpected
  }

}

object CacheChangeLogSpec {

  private val testId: String = "1234567890"

  val testDataBeforeChangeSet_1: String = """{
      |    "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |    "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |    "eori": "GB21885355487467",
      |    "status": "COMPLETE",
      |    "createdDateTime": {
      |        "$date": 1585653005745
      |    },
      |    "updatedDateTime": {
      |        "$date": 1585653072661
      |    },
      |    "type": "STANDARD",
      |    "dispatchLocation": {
      |        "dispatchLocation": "EX"
      |    },
      |    "additionalDeclarationType": "D",
      |    "consignmentReferences": {
      |        "ducr": {
      |            "ducr": "8GB123456486556-101SHIP1"
      |        },
      |        "lrn": "QSLRN5914100"
      |    },
      |    "transport": {
      |        "transportPayment": {
      |            "paymentMethod": "H"
      |        },
      |        "containers": [
      |            {
      |                "id": "123456",
      |                "seals": []
      |            }
      |        ],
      |        "borderModeOfTransportCode": "1",
      |        "meansOfTransportOnDepartureType": "11",
      |        "meansOfTransportOnDepartureIDNumber": "123456754323356",
      |        "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |        "meansOfTransportCrossingTheBorderType": "11",
      |        "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
      |    },
      |    "parties": {
      |        "exporterDetails": {
      |            "details": {
      |                "eori": "GB717572504502801"
      |            }
      |        },
      |        "consigneeDetails": {
      |            "details": {
      |                "address": {
      |                    "fullName": "Bags Export",
      |                    "addressLine": "1 Bags Avenue",
      |                    "townOrCity": "New York",
      |                    "postCode": "10001",
      |                    "country": "United States of America"
      |                }
      |            }
      |        },
      |        "declarantDetails": {
      |            "details": {
      |                "eori": "GB717572504502811"
      |            }
      |        },
      |        "representativeDetails": {},
      |        "declarationAdditionalActorsData": {
      |            "actors": []
      |        },
      |        "declarationHoldersData": {
      |            "holders": [
      |                {
      |                    "authorisationTypeCode": "AEOC",
      |                    "eori": "GB717572504502811"
      |                }
      |            ]
      |        },
      |        "carrierDetails": {
      |            "details": {
      |                "address": {
      |                    "fullName": "XYZ Carrier",
      |                    "addressLine": "School Road",
      |                    "townOrCity": "London",
      |                    "postCode": "WS1 2AB",
      |                    "country": "United Kingdom"
      |                }
      |            }
      |        }
      |    },
      |    "locations": {
      |        "originationCountry": "AF",
      |        "destinationCountry": "AL",
      |        "hasRoutingCountries": true,
      |        "routingCountries": [
      |            "DZ", "AL"
      |        ],
      |        "goodsLocation": {
      |            "country": "Afghanistan",
      |            "typeOfLocation": "A",
      |            "qualifierOfIdentification": "U",
      |            "identificationOfLocation": "FXTFXTFXT",
      |            "additionalIdentifier": "123"
      |        },
      |        "officeOfExit": {
      |            "officeId": "GB000434",
      |            "circumstancesCode": "No"
      |        },
      |        "supervisingCustomsOffice": {
      |            "supervisingCustomsOffice": "GBLBA001"
      |        },
      |        "warehouseIdentification": {},
      |        "inlandModeOfTransportCode": {
      |            "inlandModeOfTransportCode": "1"
      |        }
      |    },
      |    "items": [
      |        {
      |            "id": "8af3g619",
      |            "sequenceId": 1,
      |            "procedureCodes": {
      |                "procedureCode": "1040",
      |                "additionalProcedureCodes": [
      |                    "000"
      |                ]
      |            },
      |            "fiscalInformation": {
      |                "onwardSupplyRelief": "No"
      |            },
      |            "statisticalValue": {
      |                "statisticalValue": "1000"
      |            },
      |            "commodityDetails": {
      |                "combinedNomenclatureCode": "46021910",
      |                "descriptionOfGoods": "Straw for bottles"
      |            },
      |            "dangerousGoodsCode": {},
      |            "cusCode": {},
      |            "taricCodes": [],
      |            "nactCodes": [],
      |            "packageInformation": [
      |                {
      |                    "typesOfPackages": "PK",
      |                    "numberOfPackages": 10,
      |                    "shippingMarks": "Shipping description"
      |                }
      |            ],
      |            "commodityMeasure": {
      |                "supplementaryUnits": "10",
      |                "netMass": "500",
      |                "grossMass": "700"
      |            },
      |            "additionalInformation": {
      |                "items": [
      |                    {
      |                        "code": "00400",
      |                        "description": "EXPORTER"
      |                    }
      |                ]
      |            },
      |            "documentsProducedData": {
      |                "documents": [
      |                    {
      |                        "documentTypeCode": "C501",
      |                        "documentIdentifier": "GBAEOC717572504502811"
      |                    }
      |                ]
      |            }
      |        }
      |    ],
      |    "totalNumberOfItems": {
      |        "totalAmountInvoiced": "56764",
      |        "exchangeRate": "1.49",
      |        "totalPackage": "1"
      |    },
      |    "previousDocuments": {
      |        "documents": []
      |    },
      |    "natureOfTransaction": {
      |        "natureType": "1"
      |    }
      |}
      |""".stripMargin
  val testDataAfterChangeSet_1: String = testDataBeforeChangeSet_1

  val testDataBeforeChangeSet_2: String = testDataAfterChangeSet_1
  val testDataAfterChangeSet_2: String = """{
      |  "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "eori": "GB21885355487467",
      |  "status": "COMPLETE",
      |  "createdDateTime": {
      |    "$date": 1585653005745
      |  },
      |  "updatedDateTime": {
      |    "$date": 1585653072661
      |  },
      |  "type": "STANDARD",
      |  "dispatchLocation": {
      |    "dispatchLocation": "EX"
      |  },
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |    "ducr": {
      |      "ducr": "8GB123456486556-101SHIP1"
      |    },
      |    "lrn": "QSLRN5914100"
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
      |    "borderModeOfTransportCode": "1",
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "123456754323356",
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
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
      |          "postCode": "10001",
      |          "country": "United States of America"
      |        }
      |      }
      |    },
      |    "declarantDetails": {
      |      "details": {
      |        "eori": "GB717572504502811"
      |      }
      |    },
      |    "representativeDetails": {},
      |    "declarationAdditionalActorsData": {
      |      "actors": []
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
      |    "originationCountry": "AF",
      |    "destinationCountry": "AL",
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      "DZ", "AL"
      |    ],
      |    "goodsLocation": {
      |      "country": "AF",
      |      "typeOfLocation": "A",
      |      "qualifierOfIdentification": "U",
      |      "identificationOfLocation": "FXTFXTFXT",
      |      "additionalIdentifier": "123"
      |    },
      |    "officeOfExit": {
      |      "officeId": "GB000434",
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
      |      "id": "8af3g619",
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
      |      "dangerousGoodsCode": {},
      |      "cusCode": {},
      |      "taricCodes": [],
      |      "nactCodes": [],
      |      "packageInformation": [
      |        {
      |          "typesOfPackages": "PK",
      |          "numberOfPackages": 10,
      |          "shippingMarks": "Shipping description"
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
      |    "documents": []
      |  },
      |  "natureOfTransaction": {
      |    "natureType": "1"
      |  }
      |}
      |""".stripMargin

  val testDataBeforeChangeSet_3: String = testDataAfterChangeSet_2
  val testDataAfterChangeSet_3: String = """{
      |  "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "eori": "GB21885355487467",
      |  "status": "COMPLETE",
      |  "createdDateTime": {
      |    "$date": 1585653005745
      |  },
      |  "updatedDateTime": {
      |    "$date": 1585653072661
      |  },
      |  "type": "STANDARD",
      |  "dispatchLocation": {
      |    "dispatchLocation": "EX"
      |  },
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |    "ducr": {
      |      "ducr": "8GB123456486556-101SHIP1"
      |    },
      |    "lrn": "QSLRN5914100"
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
      |    "borderModeOfTransportCode": "1",
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "123456754323356",
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
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
      |          "postCode": "10001",
      |          "country": "United States of America"
      |        }
      |      }
      |    },
      |    "declarantDetails": {
      |      "details": {
      |        "eori": "GB717572504502811"
      |      }
      |    },
      |    "representativeDetails": {},
      |    "declarationAdditionalActorsData": {
      |      "actors": []
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
      |    "destinationCountry": "AL",
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      "DZ",
      |      "AL"
      |    ],
      |    "goodsLocation": {
      |      "country": "AF",
      |      "typeOfLocation": "A",
      |      "qualifierOfIdentification": "U",
      |      "identificationOfLocation": "FXTFXTFXT",
      |      "additionalIdentifier": "123"
      |    },
      |    "officeOfExit": {
      |      "officeId": "GB000434",
      |      "circumstancesCode": "No"
      |    },
      |    "supervisingCustomsOffice": {
      |      "supervisingCustomsOffice": "GBLBA001"
      |    },
      |    "warehouseIdentification": {},
      |    "inlandModeOfTransportCode": {
      |      "inlandModeOfTransportCode": "1"
      |    },
      |    "originationCountry": {
      |      "code": "AF"
      |    }
      |  },
      |  "items": [
      |    {
      |      "id": "8af3g619",
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
      |      "dangerousGoodsCode": {},
      |      "cusCode": {},
      |      "taricCodes": [],
      |      "nactCodes": [],
      |      "packageInformation": [
      |        {
      |          "typesOfPackages": "PK",
      |          "numberOfPackages": 10,
      |          "shippingMarks": "Shipping description"
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
      |    "documents": []
      |  },
      |  "natureOfTransaction": {
      |    "natureType": "1"
      |  }
      |}""".stripMargin

  val testDataBeforeChangeSet_4: String = testDataAfterChangeSet_3
  val testDataAfterChangeSet_4: String = """{
      |  "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "eori": "GB21885355487467",
      |  "status": "COMPLETE",
      |  "createdDateTime": {
      |    "$date": 1585653005745
      |  },
      |  "updatedDateTime": {
      |    "$date": 1585653072661
      |  },
      |  "type": "STANDARD",
      |  "dispatchLocation": {
      |    "dispatchLocation": "EX"
      |  },
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |    "ducr": {
      |      "ducr": "8GB123456486556-101SHIP1"
      |    },
      |    "lrn": "QSLRN5914100"
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
      |    "borderModeOfTransportCode": "1",
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "123456754323356",
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
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
      |          "postCode": "10001",
      |          "country": "United States of America"
      |        }
      |      }
      |    },
      |    "declarantDetails": {
      |      "details": {
      |        "eori": "GB717572504502811"
      |      }
      |    },
      |    "representativeDetails": {},
      |    "declarationAdditionalActorsData": {
      |      "actors": []
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
      |    "destinationCountry": {
      |      "code": "AL"
      |    },
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      "DZ",
      |      "AL"
      |    ],
      |    "goodsLocation": {
      |      "country": "AF",
      |      "typeOfLocation": "A",
      |      "qualifierOfIdentification": "U",
      |      "identificationOfLocation": "FXTFXTFXT",
      |      "additionalIdentifier": "123"
      |    },
      |    "officeOfExit": {
      |      "officeId": "GB000434",
      |      "circumstancesCode": "No"
      |    },
      |    "supervisingCustomsOffice": {
      |      "supervisingCustomsOffice": "GBLBA001"
      |    },
      |    "warehouseIdentification": {},
      |    "inlandModeOfTransportCode": {
      |      "inlandModeOfTransportCode": "1"
      |    },
      |    "originationCountry": {
      |      "code": "AF"
      |    }
      |  },
      |  "items": [
      |    {
      |      "id": "8af3g619",
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
      |      "dangerousGoodsCode": {},
      |      "cusCode": {},
      |      "taricCodes": [],
      |      "nactCodes": [],
      |      "packageInformation": [
      |        {
      |          "typesOfPackages": "PK",
      |          "numberOfPackages": 10,
      |          "shippingMarks": "Shipping description"
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
      |    "documents": []
      |  },
      |  "natureOfTransaction": {
      |    "natureType": "1"
      |  }
      |}""".stripMargin

  val testDataBeforeChangeSet_5: String = testDataAfterChangeSet_4
  val testDataAfterChangeSet_5: String = """{
      |  "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "eori": "GB21885355487467",
      |  "status": "COMPLETE",
      |  "createdDateTime": {
      |    "$date": 1585653005745
      |  },
      |  "updatedDateTime": {
      |    "$date": 1585653072661
      |  },
      |  "type": "STANDARD",
      |  "dispatchLocation": {
      |    "dispatchLocation": "EX"
      |  },
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |    "ducr": {
      |      "ducr": "8GB123456486556-101SHIP1"
      |    },
      |    "lrn": "QSLRN5914100"
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
      |    "borderModeOfTransportCode": "1",
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "123456754323356",
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
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
      |          "postCode": "10001",
      |          "country": "United States of America"
      |        }
      |      }
      |    },
      |    "declarantDetails": {
      |      "details": {
      |        "eori": "GB717572504502811"
      |      }
      |    },
      |    "representativeDetails": {},
      |    "declarationAdditionalActorsData": {
      |      "actors": []
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
      |    "destinationCountry": {
      |      "code": "AL"
      |    },
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      { "code": "DZ" },
      |      { "code": "AL" }
      |    ],
      |    "goodsLocation": {
      |      "country": "AF",
      |      "typeOfLocation": "A",
      |      "qualifierOfIdentification": "U",
      |      "identificationOfLocation": "FXTFXTFXT",
      |      "additionalIdentifier": "123"
      |    },
      |    "officeOfExit": {
      |      "officeId": "GB000434",
      |      "circumstancesCode": "No"
      |    },
      |    "supervisingCustomsOffice": {
      |      "supervisingCustomsOffice": "GBLBA001"
      |    },
      |    "warehouseIdentification": {},
      |    "inlandModeOfTransportCode": {
      |      "inlandModeOfTransportCode": "1"
      |    },
      |    "originationCountry": {
      |      "code": "AF"
      |    }
      |  },
      |  "items": [
      |    {
      |      "id": "8af3g619",
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
      |      "dangerousGoodsCode": {},
      |      "cusCode": {},
      |      "taricCodes": [],
      |      "nactCodes": [],
      |      "packageInformation": [
      |        {
      |          "typesOfPackages": "PK",
      |          "numberOfPackages": 10,
      |          "shippingMarks": "Shipping description"
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
      |    "documents": []
      |  },
      |  "natureOfTransaction": {
      |    "natureType": "1"
      |  }
      |}""".stripMargin

  val testDataBeforeChangeSet_6: String = testDataAfterChangeSet_5
  val testDataAfterChangeSet_6: String = """{
      |  "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
      |  "eori": "GB21885355487467",
      |  "status": "COMPLETE",
      |  "createdDateTime": {
      |    "$date": 1585653005745
      |  },
      |  "updatedDateTime": {
      |    "$date": 1585653072661
      |  },
      |  "type": "STANDARD",
      |  "dispatchLocation": {
      |    "dispatchLocation": "EX"
      |  },
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |    "ducr": {
      |      "ducr": "8GB123456486556-101SHIP1"
      |    },
      |    "lrn": "QSLRN5914100"
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
      |    "borderModeOfTransportCode": { "code": "1" },
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "123456754323356",
      |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
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
      |          "postCode": "10001",
      |          "country": "United States of America"
      |        }
      |      }
      |    },
      |    "declarantDetails": {
      |      "details": {
      |        "eori": "GB717572504502811"
      |      }
      |    },
      |    "representativeDetails": {},
      |    "declarationAdditionalActorsData": {
      |      "actors": []
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
      |    "destinationCountry": {
      |      "code": "AL"
      |    },
      |    "hasRoutingCountries": true,
      |    "routingCountries": [
      |      { "code": "DZ" },
      |      { "code": "AL" }
      |    ],
      |    "goodsLocation": {
      |      "country": "AF",
      |      "typeOfLocation": "A",
      |      "qualifierOfIdentification": "U",
      |      "identificationOfLocation": "FXTFXTFXT",
      |      "additionalIdentifier": "123"
      |    },
      |    "officeOfExit": {
      |      "officeId": "GB000434",
      |      "circumstancesCode": "No"
      |    },
      |    "supervisingCustomsOffice": {
      |      "supervisingCustomsOffice": "GBLBA001"
      |    },
      |    "warehouseIdentification": {},
      |    "inlandModeOfTransportCode": {
      |      "inlandModeOfTransportCode": "1"
      |    },
      |    "originationCountry": {
      |      "code": "AF"
      |    }
      |  },
      |  "items": [
      |    {
      |      "id": "8af3g619",
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
      |      "dangerousGoodsCode": {},
      |      "cusCode": {},
      |      "taricCodes": [],
      |      "nactCodes": [],
      |      "packageInformation": [
      |        {
      |          "typesOfPackages": "PK",
      |          "numberOfPackages": 10,
      |          "shippingMarks": "Shipping description"
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
      |    "documents": []
      |  },
      |  "natureOfTransaction": {
      |    "natureType": "1"
      |  }
      |}""".stripMargin

  val testDataBeforeChangeSet_7: String = testDataAfterChangeSet_6
  val testDataAfterChangeSet_7: String = s"""{
                                           |  "_id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
                                           |  "id": "3414ac6d-13ba-415c-9ac0-f5ffb3fc2fed",
                                           |  "eori": "GB21885355487467",
                                           |  "status": "COMPLETE",
                                           |  "createdDateTime": {
                                           |    "$$date": 1585653005745
                                           |  },
                                           |  "updatedDateTime": {
                                           |    "$$date": 1585653072661
                                           |  },
                                           |  "type": "STANDARD",
                                           |  "dispatchLocation": {
                                           |    "dispatchLocation": "EX"
                                           |  },
                                           |  "additionalDeclarationType": "D",
                                           |  "consignmentReferences": {
                                           |    "ducr": {
                                           |      "ducr": "8GB123456486556-101SHIP1"
                                           |    },
                                           |    "lrn": "QSLRN5914100"
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
                                           |    "borderModeOfTransportCode": { "code": "1" },
                                           |    "meansOfTransportOnDepartureType": "11",
                                           |    "meansOfTransportOnDepartureIDNumber": "123456754323356",
                                           |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
                                           |    "meansOfTransportCrossingTheBorderType": "11",
                                           |    "meansOfTransportCrossingTheBorderIDNumber": "Superfast Hawk Millenium"
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
                                           |          "postCode": "10001",
                                           |          "country": "United States of America"
                                           |        }
                                           |      }
                                           |    },
                                           |    "declarantDetails": {
                                           |      "details": {
                                           |        "eori": "GB717572504502811"
                                           |      }
                                           |    },
                                           |    "representativeDetails": {},
                                           |    "declarationAdditionalActorsData": {
                                           |      "actors": []
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
                                           |    "destinationCountry": {
                                           |      "code": "AL"
                                           |    },
                                           |    "hasRoutingCountries": true,
                                           |    "routingCountries": [
                                           |      { "code": "DZ" },
                                           |      { "code": "AL" }
                                           |    ],
                                           |    "goodsLocation": {
                                           |      "country": "AF",
                                           |      "typeOfLocation": "A",
                                           |      "qualifierOfIdentification": "U",
                                           |      "identificationOfLocation": "FXTFXTFXT",
                                           |      "additionalIdentifier": "123"
                                           |    },
                                           |    "officeOfExit": {
                                           |      "officeId": "GB000434",
                                           |      "circumstancesCode": "No"
                                           |    },
                                           |    "supervisingCustomsOffice": {
                                           |      "supervisingCustomsOffice": "GBLBA001"
                                           |    },
                                           |    "warehouseIdentification": {},
                                           |    "inlandModeOfTransportCode": {
                                           |      "inlandModeOfTransportCode": "1"
                                           |    },
                                           |    "originationCountry": {
                                           |      "code": "AF"
                                           |    }
                                           |  },
                                           |  "items": [
                                           |    {
                                           |      "id": "8af3g619",
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
                                           |      "dangerousGoodsCode": {},
                                           |      "cusCode": {},
                                           |      "taricCodes": [],
                                           |      "nactCodes": [],
                                           |      "packageInformation": [
                                           |        {
                                           |          "id": "$testId",
                                           |          "typesOfPackages": "PK",
                                           |          "numberOfPackages": 10,
                                           |          "shippingMarks": "Shipping description"
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
                                           |    "documents": []
                                           |  },
                                           |  "natureOfTransaction": {
                                           |    "natureType": "1"
                                           |  }
                                           |}""".stripMargin

}
