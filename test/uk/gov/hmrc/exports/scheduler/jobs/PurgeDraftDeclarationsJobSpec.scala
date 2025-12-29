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

import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.config.AppConfig.JobConfig
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, JobRunRepository}
import uk.gov.hmrc.exports.util.TimeUtils.instant
import org.mockito.Mockito.{times, verify, when}
import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

class PurgeDraftDeclarationsJobSpec extends UnitSpec {

  private val zone = ZoneOffset.UTC
  private val clock: Clock = Clock.fixed(instant(), zone)
  private val appConfig = mock[AppConfig]
  private val declarationRepository = mock[DeclarationRepository]
  private val jobRunRepository = mock[JobRunRepository]

  private val job = new PurgeDraftDeclarationsJob(appConfig, declarationRepository, jobRunRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(appConfig.clock) thenReturn clock
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
      when(appConfig.purgeDraftDeclarations).thenReturn(JobConfig(runTime, 1.day))

      job.firstRunTime mustBe defined
      job.firstRunTime.get mustBe runTime
    }

    "Configure 'interval'" in {
      when(appConfig.purgeDraftDeclarations).thenReturn(JobConfig(LocalTime.MIDNIGHT, 1.day))

      job.interval mustBe 1.day
    }

  }
  "PurgeDraftDeclarationsJob.execute" should {

    "purge expired draft declarations" in {
      when(appConfig.draftTimeToLive).thenReturn(5.days)
      when(declarationRepository.deleteExpiredDraft(any[Instant])).thenReturn(Future.successful(0L))
      when(jobRunRepository.isFirstRunInTheDay(job.name)).thenReturn(Future.successful(true))

      await(job.execute())

      verify(declarationRepository).deleteExpiredDraft(any[Instant])
    }

    "not run 'declarationRepository.deleteExpiredDraft'" when {
      "it was already executed in the day" in {
        when(jobRunRepository.isFirstRunInTheDay(job.name)).thenReturn(Future.successful(false))

        await(job.execute())

        verify(declarationRepository, never).deleteExpiredDraft(any[Instant])
      }
    }
  }
}
