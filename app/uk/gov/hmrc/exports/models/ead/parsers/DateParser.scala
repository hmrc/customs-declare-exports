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

package uk.gov.hmrc.exports.models.ead.parsers

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

object DateParser {

  val formatter304: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssz")
  val zoneUTC: ZoneId = ZoneId.of("UTC")

  def zonedDateTime(datetime: String): ZonedDateTime = ZonedDateTime.of(LocalDateTime.parse(datetime, formatter304), zoneUTC)

  def optionZonedDateTime(datetime: String): Option[ZonedDateTime] = if (datetime.nonEmpty) Some(zonedDateTime(datetime)) else None
}
