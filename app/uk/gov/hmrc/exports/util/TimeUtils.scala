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

package uk.gov.hmrc.exports.util

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

object TimeUtils {

  val defaultTimeZone: ZoneId = ZoneId.of("UTC")

  def now(): ZonedDateTime = ZonedDateTime.now(defaultTimeZone)

  // Remove the microseconds to avoid a String comparison mismatch.
  def instant(): Instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
}
