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

package uk.gov.hmrc.exports.repositories

import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.util.TimeUtils.{defaultTimeZone, instant}

import java.time.temporal.{ChronoField, ChronoUnit}

class JobRunRepositoryISpec extends IntegrationTestSpec {

  private val jobName: String = "PurgeAncientSubmissions"

  private val repository = instanceOf[JobRunRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  "JobRunRepository.isFirstRunInTheDay" should {

    "return true" when {

      "the collection contains NO 'JobRun' documents" in {
        repository.isFirstRunInTheDay(jobName).futureValue mustBe true
        repository.count().futureValue mustBe 1
      }

      "the last run of the job took place on the previous day" in {
        val now = instant()
        repository.insertOne(JobRun(jobName, now.minus(1, ChronoUnit.DAYS))).futureValue
        repository.isFirstRunInTheDay(jobName).futureValue mustBe true
        val jobRuns = repository.findAll().futureValue
        jobRuns.size mustBe 1
        jobRuns.head.job mustBe jobName
        val dayOfMonth = now.atZone(defaultTimeZone).get(ChronoField.DAY_OF_MONTH)
        jobRuns.head.lastRun.atZone(defaultTimeZone).get(ChronoField.DAY_OF_MONTH) mustBe dayOfMonth
      }
    }

    "return false" when {
      "the last run of the job took place on the same day" in {
        val now = instant()
        repository.insertOne(JobRun(jobName, now)).futureValue
        repository.isFirstRunInTheDay(jobName).futureValue mustBe false
      }
    }
  }
}
