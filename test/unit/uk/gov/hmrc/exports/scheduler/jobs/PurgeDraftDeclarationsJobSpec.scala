/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.BDDMockito.given
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.config.AppConfig.JobConfig
import uk.gov.hmrc.exports.repositories.DeclarationRepository

import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class PurgeDraftDeclarationsJobSpec extends UnitSpec {

  private val zone = ZoneOffset.UTC
  private val clock: Clock = Clock.fixed(Instant.now(), zone)
  private val appConfig = mock[AppConfig]
  private val declarationRepository = mock[DeclarationRepository]

  override def afterEach(): Unit = {
    super.afterEach()
    reset(appConfig, declarationRepository)
    given(appConfig.clock) willReturn clock
  }

  "PurgeDraftDeclarationsJob" should {

    "Configure 'Name'" in {
      newJob.name mustBe "PurgeDraftDeclarations"
    }

    "Configure 'firstRunTime'" in {
      val runTime = LocalTime.of(14, 0)
      given(appConfig.purgeDraftDeclarations).willReturn(JobConfig(runTime, 1.day))

      newJob.firstRunTime mustBe defined
      newJob.firstRunTime.get mustBe runTime
    }

    "Configure 'interval'" in {
      given(appConfig.purgeDraftDeclarations).willReturn(JobConfig(LocalTime.MIDNIGHT, 1.day))

      newJob.interval mustBe 1.day
    }

  }

  "Scheduled Job 'Execute'" should {
    "Purge expired draft declarations" in {

      val expireAfter: FiniteDuration = 5.days
      val expiryDate = Instant.now(appConfig.clock).minusSeconds(expireAfter.toSeconds)

      given(appConfig.draftTimeToLive).willReturn(expireAfter)
      given(declarationRepository.deleteExpiredDraft(expiryDate)).willReturn(Future.successful(0L))

      await(newJob.execute())

      verify(declarationRepository).deleteExpiredDraft(expiryDate)
    }
  }

  private def newJob: PurgeDraftDeclarationsJob = new PurgeDraftDeclarationsJob(appConfig, declarationRepository)

}
