package uk.gov.hmrc.exports.scheduler.jobs

import com.kenshoo.play.metrics.PlayModule
import com.mongodb.client.MongoCollection
import org.bson.Document
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.base.IntegrationTestPurgeSubmissionsToolSpec
import uk.gov.hmrc.exports.mongo.ExportsClient
import uk.gov.hmrc.mongo.MongoComponent

import scala.collection.JavaConverters._

class PurgeAncientSubmissionsJobSpec extends IntegrationTestPurgeSubmissionsToolSpec {

  import PurgeAncientSubmissionsJobSpec._

  implicit lazy val application: Application = GuiceApplicationBuilder()
    .disable[PlayModule]
    .overrides(bind[ExportsClient].toInstance(testExportsClient))
    .configure(("mongodb.transactional.operations", false))
    .build

  private val testJob = application.injector.instanceOf[PurgeAncientSubmissionsJob]

  private def prepareCollection(collection: MongoCollection[Document], records: List[Document]): Boolean =
    removeAll(collection).isValidLong && collection.insertMany(records.asJava).wasAcknowledged

  "PurgeAncientSubmissionsJob" should {

    "remove all records" when {
      "'submission.statusLastUpdated' is 180 days ago" in {

        val collection: MongoCollection[Document] = testJob.db.getCollection("submissions")

        prepareCollection(collection, submissions) mustBe true

        whenReady(testJob.execute()) { _ =>
          collection.countDocuments() mustBe 0
        }

      }

    }

    "remove zero records" ignore {
      "'submission.statusLastUpdated' is less than than 180 days ago" in {

        testJob.execute()
      }

      "'submission.latestEnhancedStatus' is other" when {
        "'submission.statusLastUpdated' is 180 days ago or older" in {

          testJob.execute()
        }
        "'submission.statusLastUpdated' is less than than 180 days ago" in {

          testJob.execute()
        }
      }

    }

  }

}

object PurgeAncientSubmissionsJobSpec {

  private val actionId = "8a5ef91c-a62a-4337-b51a-750b175fe6d1"
  private val unparsedNotificationId = "c5429490-8688-48ec-bdca-8d6f48c5ad5f"

  private val uuid = "TEST-SA7hb-rLAZo0a8"

  val enhancedStatusLastUpdatedOlderThan = "2021-08-02T13:20:06Z[UTC]"
  val enhancedStatusLastUpdatedRecent = "2022-08-02T13:20:06Z[UTC]"

  val GOODS_HAVE_EXITED = "GOODS_HAVE_EXITED"
  val DECLARATION_HANDLED_EXTERNALLY = "DECLARATION_HANDLED_EXTERNALLY"
  val CANCELLED = "CANCELLED"
  val REJECTED = "REJECTED"

  val submissions: List[Document] = List(submission(latestEnhancedStatus = GOODS_HAVE_EXITED, actionId = actionId, uuid = uuid))

  def declaration(uuid: String): Document = Document.parse(s"""
      |{
      |    "_id" : ObjectId("62e923c75b03a31168606642"),
      |    "id" : "$uuid",
      |    "eori" : "XL165944621471200",
      |    "status" : "COMPLETE",
      |    "createdDateTime" : ISODate("2022-01-29T19:56:51.958Z"),
      |    "updatedDateTime" : ISODate("2022-01-29T19:57:36.556Z"),
      |    "type" : "STANDARD",
      |    "additionalDeclarationType" : "D",
      |    "consignmentReferences" : {
      |        "ducr" : {
      |            "ducr" : "6TS321341891866-112L6H21L"
      |        },
      |        "lrn" : "XzmBvLMY6ZfrZL9lxu"
      |    },
      |    "linkDucrToMucr" : {
      |        "answer" : "Yes"
      |    },
      |    "mucr" : {
      |        "mucr" : "CZYX123A"
      |    },
      |    "transport" : {
      |        "expressConsignment" : {
      |            "answer" : "Yes"
      |        },
      |        "transportPayment" : {
      |            "paymentMethod" : "H"
      |        },
      |        "containers" : [
      |            {
      |                "id" : "123456",
      |                "seals" : []
      |            }
      |        ],
      |        "borderModeOfTransportCode" : {
      |            "code" : "1"
      |        },
      |        "meansOfTransportOnDepartureType" : "11",
      |        "meansOfTransportOnDepartureIDNumber" : "SHIP1",
      |        "meansOfTransportCrossingTheBorderNationality" : "United Kingdom, Great Britain, Northern Ireland",
      |        "meansOfTransportCrossingTheBorderType" : "11",
      |        "meansOfTransportCrossingTheBorderIDNumber" : "Superfast Hawk Millenium"
      |    },
      |    "parties" : {
      |        "consigneeDetails" : {
      |            "details" : {
      |                "address" : {
      |                    "fullName" : "Bags Export",
      |                    "addressLine" : "1 Bags Avenue",
      |                    "townOrCity" : "New York",
      |                    "postCode" : "10001",
      |                    "country" : "United States of America (the), Including Puerto Rico"
      |                }
      |            }
      |        },
      |        "declarantDetails" : {
      |            "details" : {
      |                "eori" : "GB7172755076834"
      |            }
      |        },
      |        "declarantIsExporter" : {
      |            "answer" : "Yes"
      |        },
      |        "declarationAdditionalActorsData" : {
      |            "actors" : []
      |        },
      |        "declarationHoldersData" : {
      |            "holders" : [
      |                {
      |                    "authorisationTypeCode" : "AEOC",
      |                    "eori" : "GB717572504502801",
      |                    "eoriSource" : "OtherEori"
      |                }
      |            ],
      |            "isRequired" : {
      |                "answer" : "Yes"
      |            }
      |        },
      |        "authorisationProcedureCodeChoice" : {
      |            "code" : "Code1040"
      |        },
      |        "carrierDetails" : {
      |            "details" : {
      |                "address" : {
      |                    "fullName" : "XYZ Carrier",
      |                    "addressLine" : "School Road",
      |                    "townOrCity" : "London",
      |                    "postCode" : "WS1 2AB",
      |                    "country" : "United Kingdom, Great Britain, Northern Ireland"
      |                }
      |            }
      |        }
      |    },
      |    "locations" : {
      |        "originationCountry" : {
      |            "code" : "GB"
      |        },
      |        "destinationCountry" : {
      |            "code" : "US"
      |        },
      |        "hasRoutingCountries" : false,
      |        "countryOfRouting" : {
      |            "isRoutingCountry" : "No"
      |        },
      |        "routingCountries" : [],
      |        "goodsLocation" : {
      |            "country" : "GB",
      |            "typeOfLocation" : "A",
      |            "qualifierOfIdentification" : "U",
      |            "identificationOfLocation" : "FXTFXTFXT"
      |        },
      |        "officeOfExit" : {
      |            "officeId" : "GB000434"
      |        },
      |        "inlandOrBorder" : {
      |            "location" : "Border"
      |        }
      |    },
      |    "items" : [
      |        {
      |            "id" : "beb7c1ba",
      |            "sequenceId" : 1,
      |            "procedureCodes" : {
      |                "procedureCode" : "1040",
      |                "additionalProcedureCodes" : [
      |                    "000"
      |                ]
      |            },
      |            "statisticalValue" : {
      |                "statisticalValue" : "1000"
      |            },
      |            "commodityDetails" : {
      |                "combinedNomenclatureCode" : "4602191000",
      |                "descriptionOfGoods" : "Straw for bottles"
      |            },
      |            "dangerousGoodsCode" : {},
      |            "taricCodes" : [],
      |            "nactCodes" : [],
      |            "packageInformation" : [
      |                {
      |                    "id" : "u4ixyusu",
      |                    "typesOfPackages" : "XD",
      |                    "numberOfPackages" : 10,
      |                    "shippingMarks" : "Shipping description"
      |                }
      |            ],
      |            "commodityMeasure" : {
      |                "supplementaryUnits" : "1000",
      |                "supplementaryUnitsNotRequired" : false,
      |                "netMass" : "500",
      |                "grossMass" : "700"
      |            },
      |            "additionalInformation" : {
      |                "isRequired" : {
      |                    "answer" : "Yes"
      |                },
      |                "items" : [
      |                    {
      |                        "code" : "00400",
      |                        "description" : "EXPORTER"
      |                    }
      |                ]
      |            },
      |            "additionalDocuments" : {
      |                "documents" : [
      |                    {
      |                        "documentTypeCode" : "C501",
      |                        "documentIdentifier" : "GBAEOC717572504502801"
      |                    }
      |                ]
      |            }
      |        }
      |    ],
      |    "readyForSubmission" : true,
      |    "totalNumberOfItems" : {
      |        "totalAmountInvoiced" : "567640",
      |        "totalAmountInvoicedCurrency" : "GBP",
      |        "invoiceOrContractRateOption" : "Yes",
      |        "exchangeRate" : "1.49",
      |        "totalPackage" : "1"
      |    },
      |    "invoiceAndExchangeRateChoice" : {
      |        "invoiceOrContractRate" : "Yes"
      |    },
      |    "previousDocuments" : {
      |        "documents" : [
      |            {
      |                "documentType" : "DCS",
      |                "documentReference" : "9GB123456782317-BH1433A61"
      |            }
      |        ]
      |    },
      |    "natureOfTransaction" : {
      |        "natureType" : "1"
      |    }
      |}
      |""".stripMargin)

  def submission(
    latestEnhancedStatus: String = GOODS_HAVE_EXITED,
    enhancedStatusLastUpdated: String = enhancedStatusLastUpdatedOlderThan,
    actionId: String,
    uuid: String
  ): Document = Document.parse(s"""
      |{
      |    "uuid" : "$uuid",
      |    "eori" : "XL165944621471200",
      |    "lrn" : "XzmBvLMY6ZfrZL9lxu",
      |    "ducr" : "6TS321341891866-112L6H21L",
      |    "actions" : [
      |        {
      |            "id" : "$actionId",
      |            "requestType" : "SubmissionRequest",
      |            "requestTimestamp" : "2022-08-02T13:17:04.102Z[UTC]",
      |            "notifications" : [
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_HAVE_EXITED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:19:06Z[UTC]",
      |                    "enhancedStatus" : "CLEARED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:18:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_ARRIVED_MESSAGE"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:17:06Z[UTC]",
      |                    "enhancedStatus" : "RECEIVED"
      |                }
      |            ]
      |        }
      |    ],
      |    "enhancedStatusLastUpdated" : "$enhancedStatusLastUpdated",
      |    "latestEnhancedStatus" : "$latestEnhancedStatus",
      |    "mrn" : "22GB1168DI14797408"
      |}
      |""".stripMargin)

  def notifications(unparsedNotificationId: String, actionId: String): Document = Document.parse(s"""
      |{
      |    "_id" : ObjectId("62e923d234cc427d04be8b9d"),
      |    "unparsedNotificationId" : "$unparsedNotificationId",
      |    "actionId" : "$actionId",
      |    "details" : {
      |        "mrn" : "22GB1168DI14797408",
      |        "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |        "status" : "GOODS_HAVE_EXITED_THE_COMMUNITY",
      |        "errors" : []
      |    }
      |}
      |""".stripMargin)
}
