/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.config.AppConfig.JobConfig
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, JobRunRepository}
import uk.gov.hmrc.exports.util.TimeUtils.instant

import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class PurgeDraftDeclarationsJobSpec extends UnitSpec {

  private val zone = ZoneOffset.UTC
  private val clock: Clock = Clock.fixed(instant(), zone)
  private val appConfig = mock[AppConfig]
  private val declarationRepository = mock[DeclarationRepository]
  private val jobRunRepository = mock[JobRunRepository]

  private val job = new PurgeDraftDeclarationsJob(appConfig, declarationRepository, jobRunRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    given(appConfig.clock) willReturn clock
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(appConfig, declarationRepository, jobRunRepository)
  }

  "PurgeDraftDeclarationsJob" should {

    "Configure 'Name'" in {
      job.name mustBe "PurgeDraftDeclarations"
    }

    "Configure 'firstRunTime'" in {
      val runTime = LocalTime.of(14, 0)
      given(appConfig.purgeDraftDeclarations).willReturn(JobConfig(runTime, 1.day))

      job.firstRunTime mustBe defined
      job.firstRunTime.get mustBe runTime
    }

    "Configure 'interval'" in {
      given(appConfig.purgeDraftDeclarations).willReturn(JobConfig(LocalTime.MIDNIGHT, 1.day))

      job.interval mustBe 1.day
    }

  }
  "PurgeDraftDeclarationsJob.execute" should {

    "purge expired draft declarations" in {
      given(appConfig.draftTimeToLive).willReturn(5.days)
      given(declarationRepository.deleteExpiredDraft(any[Instant])).willReturn(Future.successful(0L))
      given(jobRunRepository.isFirstRunInTheDay(job.name)).willReturn(Future.successful(true))

      await(job.execute())

      verify(declarationRepository).deleteExpiredDraft(any[Instant])
    }

    "not run 'declarationRepository.deleteExpiredDraft'" when {
      "it was already executed in the day" in {
        given(jobRunRepository.isFirstRunInTheDay(job.name)).willReturn(Future.successful(false))

        await(job.execute())

        verify(declarationRepository, never).deleteExpiredDraft(any[Instant])
      }
    }
  }
}
