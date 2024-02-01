package uk.gov.hmrc.exports.migrations.changelogs.submission

import testdata.TestDataHelper.isoDate
import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.submission.RemoveBlockAmendmentsFieldISpec.{submissionAfterMigration, submissionBeforeMigration}

class RemoveBlockAmendmentsFieldISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "submissions"
  override val changeLog = new RemoveBlockAmendmentsField()

  "AddSubmissionFieldsForAmend" should {

    "not update a Submission document" when {
      "the document has a 'blockAmendments' field" in {
        runTest(submissionBeforeMigration, submissionAfterMigration)
      }
    }

    "update a Submission document" when {
      "the document does not have 'blockAmendments' field" in {
        runTest(submissionAfterMigration, submissionAfterMigration)
      }
    }
  }
}

object RemoveBlockAmendmentsFieldISpec {

  val lastUpdated = isoDate

  val submissionBeforeMigration =
    s"""{
      |  "_id" : "63d66e937810526f3351847d",
      |  "uuid" : "b140390f-56d4-4302-887f-5971886cb0e7",
      |  "eori" : "LU167499736454300",
      |  "lrn" : "MNllQR6rcV",
      |  "ducr" : "9CF857491229489-1S228",
      |  "actions" : [
      |    {
      |      "id" : "7ed4d825-8cc6-4d9f-abc2-05fa92d65e85",
      |      "requestType" : "SubmissionRequest",
      |      "requestTimestamp" : "2022-05-11T09:43:41.962Z[UTC]",
      |      "notifications" : [
      |        {
      |          "notificationId" : "b389d173-f5d0-44a8-9307-670890d32625",
      |          "dateTimeIssued" : "2022-06-17T10:10:36Z[UTC]",
      |          "enhancedStatus" : "ERRORS"
      |        }
      |      ]
      |    }
      |  ],
      |  "mrn" : "20GBFYLCAYVUPGJPJPYF",
      |  "lastUpdated" : "$lastUpdated",
      |  "enhancedStatusLastUpdated" : "2020-12-01T17:33:31Z[UTC]",
      |  "latestEnhancedStatus" : "ERRORS",
      |  "latestDecId" : "b140390f-56d4-4302-887f-5971886cb0e7",
      |  "latestVersionNo" : 1,
      |  "blockAmendments" : false
      |}""".stripMargin

  val submissionAfterMigration =
    s"""{
      |  "_id" : "63d66e937810526f3351847d",
      |  "uuid" : "b140390f-56d4-4302-887f-5971886cb0e7",
      |  "eori" : "LU167499736454300",
      |  "lrn" : "MNllQR6rcV",
      |  "ducr" : "9CF857491229489-1S228",
      |  "actions" : [
      |    {
      |      "id" : "7ed4d825-8cc6-4d9f-abc2-05fa92d65e85",
      |      "requestType" : "SubmissionRequest",
      |      "requestTimestamp" : "2022-05-11T09:43:41.962Z[UTC]",
      |      "notifications" : [
      |        {
      |          "notificationId" : "b389d173-f5d0-44a8-9307-670890d32625",
      |          "dateTimeIssued" : "2022-06-17T10:10:36Z[UTC]",
      |          "enhancedStatus" : "ERRORS"
      |        }
      |      ]
      |    }
      |  ],
      |  "mrn" : "20GBFYLCAYVUPGJPJPYF",
      |  "lastUpdated" : "$lastUpdated",
      |  "enhancedStatusLastUpdated" : "2020-12-01T17:33:31Z[UTC]",
      |  "latestEnhancedStatus" : "ERRORS",
      |  "latestDecId" : "b140390f-56d4-4302-887f-5971886cb0e7",
      |  "latestVersionNo" : 1
      |}""".stripMargin
}
