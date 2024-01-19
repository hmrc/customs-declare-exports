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
import org.mongodb.scala.bson.BsonDocument
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.config.AppConfig.JobConfig
import uk.gov.hmrc.exports.repositories.{JobRunRepository, PurgeSubmissionsTransactionalOps, SubmissionRepository}

import java.time.LocalTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class PurgeAncientSubmissionsJobSpec extends UnitSpec {

  private val appConfig = mock[AppConfig]
  private val submissionRepository = mock[SubmissionRepository]
  private val transactionalOps = mock[PurgeSubmissionsTransactionalOps]
  private val jobRunRepository = mock[JobRunRepository]

  private val job = new PurgeAncientSubmissionsJob(appConfig, submissionRepository, transactionalOps, jobRunRepository)

  override def afterEach(): Unit = {
    super.afterEach()
    reset(appConfig, submissionRepository, transactionalOps, jobRunRepository)
  }

  "PurgeAncientSubmissionsJob" should {

    "Configure 'Name'" in {
      job.name mustBe "PurgeAncientSubmissions"
    }

    "Configure 'firstRunTime'" in {
      val runTime = LocalTime.of(14, 0)
      given(appConfig.purgeAncientSubmissions).willReturn(JobConfig(runTime, 1.day))

      job.firstRunTime mustBe defined
      job.firstRunTime.get mustBe runTime
    }

    "Configure 'interval'" in {
      given(appConfig.purgeAncientSubmissions).willReturn(JobConfig(LocalTime.MIDNIGHT, 1.day))

      job.interval mustBe 1.day
    }

  }

  "PurgeAncientSubmissionsJob.execute" should {
    "not run 'submissionRepository.findAll'" when {
      "it was already executed in the day" in {
        given(jobRunRepository.isFirstRunInTheDay(job.name)).willReturn(Future.successful(false))

        await(job.execute())

        verify(submissionRepository, never).findAll(any[BsonDocument])
      }
    }
  }
}
