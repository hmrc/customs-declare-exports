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

package uk.gov.hmrc.exports.migrations

import org.bson.json.{Converter, JsonWriterSettings, StrictJsonWriter}
import uk.gov.hmrc.exports.util.TimeUtils.{defaultTimeZone, instant}

import java.lang
import java.time.Instant
import java.time.format.DateTimeFormatter

class TimeUtils {

  private[migrations] def currentTimePlusMillis(millis: Long): Instant = instant().plusMillis(millis)

  private[migrations] def minutesToMillis(minutes: Long): Long = minutes * 60 * 1000

  private[migrations] def millisToMinutes(millis: Long): Long = millis / (60 * 1000)
}

object TimeUtils {

  private val converter: Converter[lang.Long] = new Converter[lang.Long] {
    val formatter = DateTimeFormatter.ISO_INSTANT.withZone(defaultTimeZone)

    override def convert(value: lang.Long, writer: StrictJsonWriter): Unit = {
      val s = formatter.format(Instant.ofEpochMilli(value))
      writer.writeString(s)
    }
  }

  val jsonWriter = JsonWriterSettings
    .builder()
    .dateTimeConverter(converter)
    .build()
}
