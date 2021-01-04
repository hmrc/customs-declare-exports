/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.scheduler

import java.time.{Instant, LocalDate, LocalTime}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.exports.config.AppConfig

import scala.concurrent.duration.FiniteDuration

@Singleton
class SchedulerDateUtil @Inject()(appConfig: AppConfig) {

  private lazy val clock = appConfig.clock

  def nextRun(offset: LocalTime, interval: FiniteDuration): Instant = {
    val time = LocalTime.now(clock).withNano(0)

    val offsetSeconds: Int = offset.toSecondOfDay
    val currentSeconds: Int = time.toSecondOfDay

    val intervalSeconds: Long = interval.toSeconds
    val deltaSeconds: Int = offsetSeconds - currentSeconds
    val intervalRemainder: Long = deltaSeconds % intervalSeconds

    val intervalRemaining: Long = if (intervalRemainder < 0) intervalRemainder + intervalSeconds else intervalRemainder
    LocalDate
      .now(clock)
      .atTime(time)
      .plusSeconds(intervalRemaining)
      .atZone(clock.getZone)
      .toInstant
  }

}
