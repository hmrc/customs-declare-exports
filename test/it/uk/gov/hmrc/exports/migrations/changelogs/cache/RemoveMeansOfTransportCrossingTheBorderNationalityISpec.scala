package uk.gov.hmrc.exports.migrations.changelogs.cache

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.MigrationDefinition
import uk.gov.hmrc.exports.migrations.changelogs.cache.RemoveMeansOfTransportCrossingTheBorderNationalityISpec.{
  declarationAfterMigration,
  declarationBeforeMigration,
  declarationWithNewFieldAndOld
}

class RemoveMeansOfTransportCrossingTheBorderNationalityISpec extends IntegrationTestMigrationToolSpec {
  override val collectionUnderTest: String = "declarations"
  override val changeLog: MigrationDefinition = new RemoveMeansOfTransportCrossingTheBorderNationality()

  "RemoveMeansOfTransportCrossingTheBorderNationality" should {

    "correctly migrate declarations adding new field with the value from the old field (and removing old field)" in {
      runTest(declarationBeforeMigration, declarationAfterMigration)
    }

    "not change declarations already migrated" in {
      runTest(declarationAfterMigration, declarationAfterMigration)
    }

    "remove old field when both new and old are present" in {
      runTest(declarationWithNewFieldAndOld, declarationAfterMigration)
    }
  }
}

object RemoveMeansOfTransportCrossingTheBorderNationalityISpec {
  val declarationBeforeMigration: String =
    """{
      |  "_id": "60ded91dd3b4338579ff253c",
      |  "id": "1fb39320-8d0d-4521-ba52-ffc835026e0e",
      |  "eori": "GB239355053000",
      |  "type": "SIMPLIFIED",
      |  "additionalDeclarationType": "F",
      |  "transport": {
      |    "containers": [],
      |    "borderModeOfTransportCode": {
      |      "code": "1"
      |    },
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "BLUE MASTER II",
      |    "meansOfTransportCrossingTheBorderNationality": "Marshall Islands (the)",
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "BLUE MASTER II"
      |  }
      |}""".stripMargin

  val declarationAfterMigration: String =
    """{
      |  "_id": "60ded91dd3b4338579ff253c",
      |  "id": "1fb39320-8d0d-4521-ba52-ffc835026e0e",
      |  "eori": "GB239355053000",
      |  "type": "SIMPLIFIED",
      |  "additionalDeclarationType": "F",
      |  "transport": {
      |    "containers": [],
      |    "borderModeOfTransportCode": {
      |      "code": "1"
      |    },
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "BLUE MASTER II",
      |    "transportCrossingTheBorderNationality": { "countryCode": "Marshall Islands (the)" },
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "BLUE MASTER II"
      |  }
      |}""".stripMargin

  val declarationWithNewFieldAndOld: String =
    """{
      |  "_id": "60ded91dd3b4338579ff253c",
      |  "id": "1fb39320-8d0d-4521-ba52-ffc835026e0e",
      |  "eori": "GB239355053000",
      |  "type": "SIMPLIFIED",
      |  "additionalDeclarationType": "F",
      |  "transport": {
      |    "containers": [],
      |    "borderModeOfTransportCode": {
      |      "code": "1"
      |    },
      |    "meansOfTransportOnDepartureType": "11",
      |    "meansOfTransportOnDepartureIDNumber": "BLUE MASTER II",
      |    "meansOfTransportCrossingTheBorderNationality": "Marshall Islands (the)",
      |    "transportCrossingTheBorderNationality": { "countryCode": "Marshall Islands (the)" },
      |    "meansOfTransportCrossingTheBorderType": "11",
      |    "meansOfTransportCrossingTheBorderIDNumber": "BLUE MASTER II"
      |  }
      |}""".stripMargin
}
