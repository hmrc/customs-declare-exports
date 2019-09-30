package util

import java.time.Instant
import java.util.UUID

import org.scalatest.WordSpec
import play.api.libs.json.Json
import reactivemongo.api.{MongoConnection, MongoDriver}
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import util.testdata.ExportsDeclarationBuilder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SeedMongoSpec extends WordSpec {
  "Seed Mongo" should {
    "have correct model of json declaration" in {
      SeedMongo.json.as[ExportsDeclaration]
    }
  }
}

object SeedMongo extends App with ExportsDeclarationBuilder {

  import scala.concurrent.ExecutionContext.Implicits._

  val mongoUri = "mongodb://localhost:27017/customs-declare-exports"

  val driver = MongoDriver()

  val parsedUri = MongoConnection.parseURI(mongoUri)

  val json = Json.parse(s"""{
       |  "id": "93fdb451-552f-4aff-9dd0-e0b7b6c88f02",
       |  "eori": "GB7172755072243",
       |  "status": "COMPLETE",
       |  "createdDateTime": {
       |    "$$date": ${Instant.parse("2019-09-30T11:46:42.740Z").toEpochMilli}
       |  },
       |  "updatedDateTime": {
       |    "$$date": ${Instant.parse("2019-09-30T11:48:19.315Z").toEpochMilli}
       |  },
       |  "choice": "STD",
       |  "dispatchLocation": {
       |    "dispatchLocation": "EX"
       |  },
       |  "additionalDeclarationType": {
       |    "additionalDeclarationType": "D"
       |  },
       |  "consignmentReferences": {
       |    "ducr": {
       |      "ducr": "8GB123453469100-101SHIP1"
       |    },
       |    "lrn": "JasTest4"
       |  },
       |  "borderTransport": {
       |    "borderModeOfTransportCode": "1",
       |    "meansOfTransportOnDepartureType": "11",
       |    "meansOfTransportOnDepartureIDNumber": "SHIP1"
       |  },
       |  "transportDetails": {
       |    "meansOfTransportCrossingTheBorderNationality": "United Kingdom",
       |    "container": true,
       |    "meansOfTransportCrossingTheBorderType": "11",
       |    "meansOfTransportCrossingTheBorderIDNumber": "BOAT1",
       |    "paymentMethod": "H"
       |  },
       |  "containerData": {
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
       |    "destinationCountries": {
       |      "countryOfDispatch": "GB",
       |      "countriesOfRouting": [
       |        "GB"
       |      ],
       |      "countryOfDestination": "US"
       |    },
       |    "goodsLocation": {
       |      "country": "Angola including Cabinda",
       |      "typeOfLocation": "B",
       |      "qualifierOfIdentification": "Y",
       |      "identificationOfLocation": "FXT"
       |    },
       |    "warehouseIdentification": {
       |      "supervisingCustomsOffice": "GBLBA001",
       |      "inlandModeOfTransportCode": "1"
       |    },
       |    "officeOfExit": {
       |      "officeId": "GB000054",
       |      "presentationOfficeId": "GBLBA003",
       |      "circumstancesCode": "No"
       |    }
       |  },
       |  "items": [
       |    {
       |      "id": "ec7c4cb3",
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
       |      "itemType": {
       |        "combinedNomenclatureCode": "46021910",
       |        "taricAdditionalCode": [],
       |        "nationalAdditionalCode": [],
       |        "descriptionOfGoods": "Straw for bottles",
       |        "statisticalValue": "1000"
       |      },
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
       |            "documentIdentifierAndPart": {
       |              "documentIdentifier": "GBAEOC71757250450281",
       |              "documentPart": "1"
       |            }
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
       |}""".stripMargin)

  val declaration = json.as[ExportsDeclaration]

  val random = new scala.util.Random()

  val target = 20000

  def generateEori =
    "GB" + Math.abs(random.nextLong()).toString

  import reactivemongo.play.json.collection.JSONCollection

  def randomStatus =
    if (random.nextDouble() > 0.1) {
      DeclarationStatus.COMPLETE
    } else {
      DeclarationStatus.DRAFT
    }

  val job = parsedUri
    .map(driver.connection(_, true))
    .map { t =>
      val connection = t.get
      connection.database("customs-declare-exports").map(db => db.collection[JSONCollection]("declarations")).map {
        collection =>
          var now = 0
          while (now < target) {
            val count = random.nextInt(1000)
            val eori = generateEori
            val declarations = Range(0, count).map { _ =>
              declaration.copy(id = UUID.randomUUID.toString, eori = eori, status = randomStatus)
            }
            Await.ready(collection.insert.many(declarations), Duration.Inf)
            now += count
            println(s"Inserted $now - $count for $eori")
          }
          now
      }
    }
    .get

  {
    Await.ready(job, Duration.Inf)
    driver.close()
  }

}
