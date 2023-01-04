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

package uk.gov.hmrc.exports.migrations.repositories

import org.bson.Document
import uk.gov.hmrc.exports.migrations.changelogs.MigrationDefinition
import uk.gov.hmrc.exports.migrations.repositories.ChangeEntry._

import java.time.Instant

object ChangeEntry {
  private[migrations] val KeyChangeId: String = "changeId"
  private[migrations] val KeyAuthor: String = "author"
  private[migrations] val KeyTimestamp: String = "timestamp"
  private[migrations] val KeyChangeLogClass: String = "changeLogClass"

  def apply(migrationDefinition: MigrationDefinition): ChangeEntry = ChangeEntry(
    changeId = migrationDefinition.migrationInformation.id,
    author = migrationDefinition.migrationInformation.author,
    timestamp = Instant.now,
    changeLogClass = migrationDefinition.getClass.getSimpleName
  )
}

case class ChangeEntry(changeId: String, author: String, timestamp: Instant, changeLogClass: String) {

  private[migrations] def buildFullDBObject: Document = {
    val entry: Document = new Document
    entry
      .append(KeyChangeId, this.changeId)
      .append(KeyAuthor, this.author)
      .append(KeyTimestamp, this.timestamp)
      .append(KeyChangeLogClass, this.changeLogClass)
  }
}
