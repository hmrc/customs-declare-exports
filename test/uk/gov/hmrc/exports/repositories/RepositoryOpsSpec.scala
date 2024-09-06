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

package uk.gov.hmrc.exports.repositories

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.repositories.RepositoryOps.{mongoDate, mongoDateInQuery}

import java.time.Instant
import java.time.temporal.ChronoUnit

class RepositoryOpsSpec extends UnitSpec {

  "removeFinalZeroes" when {

    "called via mongoDate" should {
      "return the expected value" when {

        "provided with a date without milliseconds" in {
          val now = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          mongoDate(now) mustBe s"""{"$$date":"${now.toString}"}"""
        }

        "provided with a date with milliseconds multiples of 10" in {
          val millis = 30L
          val now = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          mongoDate(now.plusMillis(millis)) mustBe s"""{"$$date":"${now.toString.dropRight(1) + ".03Z"}"}"""
        }

        "provided with a date with milliseconds multiples of 100" in {
          val millis = 300L
          val now = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          mongoDate(now.plusMillis(millis)) mustBe s"""{"$$date":"${now.toString.dropRight(1) + ".3Z"}"}"""
        }
      }
    }

    "called via mongoDateInQuery" should {
      "return the expected value" when {

        "provided with a date without milliseconds" in {
          val now = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          mongoDateInQuery(now) mustBe s"""{"$$eq":ISODate("${now.toString}")}"""
        }

        "provided with a date with milliseconds multiples of 10" in {
          val millis = 30L
          val now = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          mongoDateInQuery(now.plusMillis(millis)) mustBe s"""{"$$eq":ISODate("${now.toString.dropRight(1) + ".03Z"}")}"""
        }

        "provided with a date with milliseconds multiples of 100" in {
          val millis = 300L
          val now = Instant.now.truncatedTo(ChronoUnit.SECONDS)
          mongoDateInQuery(now.plusMillis(millis)) mustBe s"""{"$$eq":ISODate("${now.toString.dropRight(1) + ".3Z"}")}"""
        }
      }
    }
  }
}
