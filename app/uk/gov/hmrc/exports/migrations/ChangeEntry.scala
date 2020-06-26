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

import org.bson.Document
import uk.gov.hmrc.exports.migrations.ChangeEntry._
import uk.gov.hmrc.exports.migrations.changelogs.MigrationInformation

object ChangeEntry {
  private[migrations] val KEY_CHANGEID: String = "changeId"
  private[migrations] val KEY_AUTHOR: String = "author"
  private[migrations] val KEY_TIMESTAMP: String = "timestamp"
  private[migrations] val KEY_CHANGELOGCLASS: String = "changeLogClass"

  def apply(migrationInformation: MigrationInformation): ChangeEntry = ChangeEntry(
    changeId = migrationInformation.id,
    author = migrationInformation.author,
    timestamp = new Date(),
    changeLogClass = migrationInformation.getClass.getSimpleName
  )
}

case class ChangeEntry(changeId: String, author: String, timestamp: Date, changeLogClass: String) {

  private[migrations] def buildFullDBObject: Document = {
    val entry: Document = new Document
    entry
      .append(KEY_CHANGEID, this.changeId)
      .append(KEY_AUTHOR, this.author)
      .append(KEY_TIMESTAMP, this.timestamp)
      .append(KEY_CHANGELOGCLASS, this.changeLogClass)
  }
}
