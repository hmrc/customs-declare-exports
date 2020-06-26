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

package uk.gov.hmrc.exports.migrations

import java.util.Date

class TimeUtils {

  /**
    * @param millis milliseconds to add to the Date
    * @return current date plus milliseconds passed as parameter
    */
  private[migrations] def currentTimePlusMillis(millis: Long): Date =
    new Date(System.currentTimeMillis + millis)

  /**
    * @return current Date
    */
  private[migrations] def currentTime: Date =
    new Date(System.currentTimeMillis)

  /**
    * Converts minutes to milliseconds
    *
    * @param minutes minutes to be converted
    * @return equivalent to the minutes passed in milliseconds
    */
  private[migrations] def minutesToMillis(minutes: Long): Long =
    minutes * 60 * 1000

  private[migrations] def millisToMinutes(minutes: Long): Long =
    minutes / (60 * 1000)

}
