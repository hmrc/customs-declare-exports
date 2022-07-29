package uk.gov.hmrc.exports.scheduler.jobs

import com.mongodb.client.{MongoClients, MongoDatabase}
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.migrations.ExportsMigrationTool
import uk.gov.hmrc.exports.migrations.changelogs.submission.PurgeAncientSubmissions

class PurgeAncientSubmissionsJobSpec extends IntegrationTestSpec {

  private val testJob = instanceOf[PurgeAncientSubmissionsJob]

  private val mongoClient = MongoClients.create()

  val database: MongoDatabase = mongoClient.getDatabase("test-customs-declare-exports")

  "PurgeAncientSubmissionsJob" should {

    "print from submissions, decs, notifications" in {
      testJob.executePurge()
    }

  }

}
