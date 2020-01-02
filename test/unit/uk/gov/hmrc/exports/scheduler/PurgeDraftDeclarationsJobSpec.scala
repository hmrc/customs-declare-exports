/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.uk.gov.hmrc.exports.scheduler

import java.time._

import org.mockito.BDDMockito.given
import org.mockito.Mockito.{reset, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.config.{AppConfig, JobConfig}
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.scheduler.PurgeDraftDeclarationsJob
import unit.uk.gov.hmrc.exports.base.UnitSpec

import scala.concurrent.Future
import scala.concurrent.duration._

class PurgeDraftDeclarationsJobSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val zone = ZoneOffset.UTC
  private val clock: Clock = Clock.fixed(Instant.now(), zone)
  private val appConfig = mock[AppConfig]
  private val declarationRepository = mock[DeclarationRepository]

  override def afterEach(): Unit = {
    super.afterEach()
    reset(appConfig, declarationRepository)
    given(appConfig.clock) willReturn clock
  }

  "Scheduled Job" should {

    "Configure 'Name'" in {
      newJob.name shouldBe "PurgeDraftDeclarations"
    }

    "Configure 'firstRunTime'" in {
      val runTime = LocalTime.of(14, 0)
      given(appConfig.purgeDraftDeclarations).willReturn(JobConfig(runTime, 1.day))

      newJob.firstRunTime shouldBe runTime
    }

    "Configure 'interval'" in {
      given(appConfig.purgeDraftDeclarations).willReturn(JobConfig(LocalTime.MIDNIGHT, 1.day))

      newJob.interval shouldBe 1.day
    }

  }

  "Scheduled Job 'Execute'" should {
    "Purge expired draft declarations" in {

      val expireAfter: FiniteDuration = 5 days
      val expiryDate = Instant.now(appConfig.clock).minusSeconds(expireAfter.toSeconds)

      given(appConfig.draftTimeToLive).willReturn(expireAfter)
      given(declarationRepository.deleteExpiredDraft(expiryDate)).willReturn(Future.successful(0))

      await(newJob.execute())

      verify(declarationRepository).deleteExpiredDraft(expiryDate)
    }
  }

  private def newJob: PurgeDraftDeclarationsJob = new PurgeDraftDeclarationsJob(appConfig, declarationRepository)

}
