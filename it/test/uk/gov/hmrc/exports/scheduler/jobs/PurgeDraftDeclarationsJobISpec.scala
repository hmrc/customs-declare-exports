/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
