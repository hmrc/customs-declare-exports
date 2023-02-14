package uk.gov.hmrc.exports.migrations.changelogs.cache

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.cache.AddSequencingOfMultipleItemsISpec._

class AddSequencingOfMultipleItemsISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "declarations"
  override val changeLog = new AddSequencingOfMultipleItems

  "AddSequencingOfMultipleItems migration" should {

    "correctly migrate declarations adding a map of sequenceIds for Containers, Seals and Routing Countries " in {
      runTest(declarationBeforeMigration, declarationAfterMigration)
    }

    "not change declarations already migrated" in {
      runTest(declarationAfterMigration, declarationAfterMigration)
    }
  }
}

object AddSequencingOfMultipleItemsISpec {

  val declarationBeforeMigration: String =
    """{
      |  "_id": { "$oid": "63cfe1c711be294a0ee1b11e" },
      |  "id": "b75149fc-1363-4d5d-ab53-a99218c535bc",
      |  "declarationMeta": {
      |      "status": "COMPLETE",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true
      |  },
      |  "eori": "GB7172755067703",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "5GB123456789000-123ABC456DEFIIIII"
      |      },
      |      "lrn": "BMUUVH6FFZRLKANUDGSAOZ",
      |      "mrn": "37GBEATM4TR72U156"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "GBRSUOG-805833"
      |  },
      |  "transport": {
      |      "containers": [
      |          {
      |              "id": "container1",
      |              "seals": [
      |                  {
      |                      "id": "seal1"
      |                  },
      |                  {
      |                      "id": "seal2"
      |                  }
      |              ]
      |          },
      |          {
      |              "id": "container2",
      |              "seals": [
      |                  {
      |                      "id": "seal3"
      |                  },
      |                  {
      |                      "id": "seal4"
      |                  }
      |              ]
      |          }
      |      ]
      |  },
      |  "parties": {},
      |  "locations": {
      |      "hasRoutingCountries": true,
      |      "routingCountries": [
      |          {
      |              "code": "AM"
      |          },
      |          {
      |              "code": "IT"
      |          }
      |      ]
      |  },
      |  "items": []
      |}""".stripMargin

  val declarationAfterMigration: String =
    """{
      |  "_id": { "$oid": "63cfe1c711be294a0ee1b11e" },
      |  "id": "b75149fc-1363-4d5d-ab53-a99218c535bc",
      |  "declarationMeta": {
      |      "status": "COMPLETE",
      |      "createdDateTime": { "$date": "2023-01-01T00:00:00.111Z" },
      |      "updatedDateTime": { "$date": "2023-02-02T00:00:00.222Z" },
      |      "summaryWasVisited": true,
      |      "readyForSubmission": true,
      |      "maxSequenceIds": {
      |          "Containers": 2,
      |          "RoutingCountries": 2,
      |          "Seals": 4
      |      }
      |  },
      |  "eori": "GB7172755067703",
      |  "type": "STANDARD",
      |  "additionalDeclarationType": "D",
      |  "consignmentReferences": {
      |      "ducr": {
      |          "ducr": "5GB123456789000-123ABC456DEFIIIII"
      |      },
      |      "lrn": "BMUUVH6FFZRLKANUDGSAOZ",
      |      "mrn": "37GBEATM4TR72U156"
      |  },
      |  "linkDucrToMucr": {
      |      "answer": "Yes"
      |  },
      |  "mucr": {
      |      "mucr": "GBRSUOG-805833"
      |  },
      |  "transport": {
      |      "containers": [
      |          {
      |              "sequenceId": 1,
      |              "id": "container1",
      |              "seals": [
      |                  {
      |                      "sequenceId": 1,
      |                      "id": "seal1"
      |                  },
      |                  {
      |                      "sequenceId": 2,
      |                      "id": "seal2"
      |                  }
      |              ]
      |          },
      |          {
      |              "sequenceId": 2,
      |              "id": "container2",
      |              "seals": [
      |                  {
      |                      "sequenceId": 3,
      |                      "id": "seal3"
      |                  },
      |                  {
      |                      "sequenceId": 4,
      |                      "id": "seal4"
      |                  }
      |              ]
      |          }
      |      ]
      |  },
      |  "parties": {},
      |  "locations": {
      |      "hasRoutingCountries": true,
      |      "routingCountries": [
      |          {
      |              "sequenceId": 1,
      |              "country": {
      |                  "code": "AM"
      |              }
      |          },
      |          {
      |              "sequenceId": 2,
      |              "country": {
      |                  "code": "IT"
      |              }
      |          }
      |      ]
      |  },
      |  "items": []
      |}""".stripMargin
}
