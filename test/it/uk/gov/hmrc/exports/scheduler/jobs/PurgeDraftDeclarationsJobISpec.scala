package uk.gov.hmrc.exports.scheduler.jobs

import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.IntegrationTestPurgeSubmissionsToolSpec
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.scheduler.jobs.PurgeDraftDeclarationsJobISpec.uuids
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, TimeUtils}

import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class PurgeDraftDeclarationsJobISpec extends IntegrationTestPurgeSubmissionsToolSpec with ExportsDeclarationBuilder {

  private val testJob: PurgeDraftDeclarationsJob = instanceOf[PurgeDraftDeclarationsJob]

  private val beyondExpiryDateTime = TimeUtils.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(testJob.expireDuration.toSeconds).toInstant
  private val recentlyUpdatedDateTime = TimeUtils.now().minusDays(1).toInstant

  "PurgeDraftDeclarationsJob" should {

    "remove DRAFT and AMENDMENT_DRAFT records" when {
      "'declarationMeta.updatedDateTime' is 30 days ago" in {
        val declarations: List[ExportsDeclaration] = List(
          aDeclaration(withId(uuids(0)), withStatus(DRAFT), withUpdatedDateTime(beyondExpiryDateTime), withEori(eori)),
          aDeclaration(withId(uuids(1)), withStatus(AMENDMENT_DRAFT), withUpdatedDateTime(beyondExpiryDateTime), withEori(eori))
        )

        whenReady {
          for {
            _ <- declarationRepository.bulkInsert(declarations)
            _ <- testJob.execute()
            dec <- declarationRepository.findAll()
          } yield dec.size
        }(decCount => decCount mustBe 0)
      }
    }

    "don't remove recently updated records" in {
      val declarations: List[ExportsDeclaration] = List(
        aDeclaration(withId(uuids(0)), withStatus(DRAFT), withUpdatedDateTime(recentlyUpdatedDateTime), withEori(eori)),
        aDeclaration(withId(uuids(1)), withStatus(AMENDMENT_DRAFT), withUpdatedDateTime(recentlyUpdatedDateTime), withEori(eori))
      )

      whenReady {
        for {
          _ <- declarationRepository.bulkInsert(declarations)
          _ <- testJob.execute()
          dec <- declarationRepository.findAll()
        } yield dec.size
      }(decCount => decCount mustBe 2)
    }

    "don't remove records older than the expiry but neither DRAFT nor AMENDMENT_DRAFT" in {
      val declarations: List[ExportsDeclaration] = List(
        aDeclaration(withId(uuids(0)), withStatus(INITIAL), withUpdatedDateTime(beyondExpiryDateTime), withEori(eori)),
        aDeclaration(withId(uuids(1)), withStatus(COMPLETE), withUpdatedDateTime(beyondExpiryDateTime), withEori(eori))
      )

      whenReady {
        for {
          _ <- declarationRepository.bulkInsert(declarations)
          _ <- testJob.execute()
          dec <- declarationRepository.findAll()
        } yield dec.size
      }(decCount => decCount mustBe 2)
    }
  }
}

object PurgeDraftDeclarationsJobISpec {
  private val uuids = Seq("1TEST-SA7hb-rLAZo0a8", "2TEST-SA7hb-rLAZo0a8")
}
