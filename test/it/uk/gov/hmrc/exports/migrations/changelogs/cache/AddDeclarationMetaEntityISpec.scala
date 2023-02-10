package uk.gov.hmrc.exports.migrations.changelogs.cache

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.cache.AddDeclarationMetaEntityISpec._
class AddDeclarationMetaEntityISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "declarations"
  override val changeLog = new AddDeclarationMetaEntity

  "AddDeclarationMetaEntity migration" should {

    "correctly migrate declarations with all to-be declarationMeta fields present" in {
      runTest(declarationBeforeMigrationWithAllFields, declarationAfterMigrationWithAllFields)
    }

    "correctly migrate declarations with optional to-be declarationMeta fields not present" in {
      runTest(declarationBeforeMigrationWithNoOptionalFields, declarationAfterMigrationWithNoOptionalFields)
    }

    "not migrate already migrated declarations" in {
      runTest(declarationAfterMigrationWithNoOptionalFields, declarationAfterMigrationWithNoOptionalFields)
      runTest(declarationAfterMigrationWithAllFields, declarationAfterMigrationWithAllFields)
    }
  }

}

object AddDeclarationMetaEntityISpec {

  val declarationBeforeMigrationWithAllFields: String =
    """{
      |    "_id": {    "$oid": "63cfe1c711be294a0ee1b11e"  },
      |    "id": "3d76872e-15ba-4851-9726-a5f90517cf13",
      |    "parentDeclarationId": "11111111",
      |    "parentDeclarationEnhancedStatus": "ERRORS",
      |    "eori": "GB7172755040794",
      |    "status": "COMPLETE",
      |    "createdDateTime": {    "$date": "2023-01-24T13:48:55.05Z"  },
      |    "updatedDateTime": {    "$date": "2023-01-24T13:49:28.119Z"  },
      |    "type": "STANDARD",
      |    "additionalDeclarationType": "D",
      |    "consignmentReferences": {},
      |    "linkDucrToMucr": {},
      |    "mucr": {},
      |    "transport": {},
      |    "parties": {},
      |    "locations": {},
      |    "items": [],
      |    "totalNumberOfItems": {},
      |    "previousDocuments": {},
      |    "natureOfTransaction": {},
      |    "summaryWasVisited": true,
      |    "readyForSubmission": true
      |    }""".stripMargin

  val declarationBeforeMigrationWithNoOptionalFields: String =
    """{
      |    "_id": {    "$oid": "63cfe1c711be294a0ee1b11e"  },
      |    "id": "3d76872e-15ba-4851-9726-a5f90517cf13",
      |    "eori": "GB7172755040794",
      |    "status": "COMPLETE",
      |    "createdDateTime": {    "$date": "2023-01-24T13:48:55.05Z"  },
      |    "updatedDateTime": {    "$date": "2023-01-24T13:49:28.119Z"  },
      |    "type": "STANDARD",
      |    "additionalDeclarationType": "D",
      |    "consignmentReferences": {},
      |    "linkDucrToMucr": {},
      |    "mucr": {},
      |    "transport": {},
      |    "parties": {},
      |    "locations": {},
      |    "items": [],
      |    "totalNumberOfItems": {},
      |    "previousDocuments": {},
      |    "natureOfTransaction": {}
      |    }""".stripMargin

  val declarationAfterMigrationWithAllFields: String =
    """{
      |    "_id": {    "$oid": "63cfe1c711be294a0ee1b11e"  },
      |    "id": "3d76872e-15ba-4851-9726-a5f90517cf13",
      |    "declarationMeta": {
      |       "parentDeclarationId": "11111111",
      |       "parentDeclarationEnhancedStatus": "ERRORS",
      |       "status": "COMPLETE",
      |       "createdDateTime": {    "$date": "2023-01-24T13:48:55.05Z"  },
      |       "updatedDateTime": {    "$date": "2023-01-24T13:49:28.119Z"  },
      |       "summaryWasVisited": true,
      |       "readyForSubmission": true
      |    },
      |    "eori": "GB7172755040794",
      |    "type": "STANDARD",
      |    "additionalDeclarationType": "D",
      |    "consignmentReferences": {},
      |    "linkDucrToMucr": {},
      |    "mucr": {},
      |    "transport": {},
      |    "parties": {},
      |    "locations": {},
      |    "items": [],
      |    "totalNumberOfItems": {},
      |    "previousDocuments": {},
      |    "natureOfTransaction": {}
      |    }""".stripMargin

  val declarationAfterMigrationWithNoOptionalFields: String =
    """{
      |    "_id": {    "$oid": "63cfe1c711be294a0ee1b11e"  },
      |    "id": "3d76872e-15ba-4851-9726-a5f90517cf13",
      |    "declarationMeta": {
      |       "status": "COMPLETE",
      |       "createdDateTime": {    "$date": "2023-01-24T13:48:55.05Z"  },
      |       "updatedDateTime": {    "$date": "2023-01-24T13:49:28.119Z"  }
      |    },
      |    "eori": "GB7172755040794",
      |    "type": "STANDARD",
      |    "additionalDeclarationType": "D",
      |    "consignmentReferences": {},
      |    "linkDucrToMucr": {},
      |    "mucr": {},
      |    "transport": {},
      |    "parties": {},
      |    "locations": {},
      |    "items": [],
      |    "totalNumberOfItems": {},
      |    "previousDocuments": {},
      |    "natureOfTransaction": {}
      |    }""".stripMargin
}
