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

package uk.gov.hmrc.exports.migrations.changelogs.submission

import com.mongodb.client.MongoDatabase
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.{exists, not}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.jdk.CollectionConverters.SeqHasAsJava

class AddSubmissionFieldsForAmend extends MigrationDefinition with Logging {

  private val uuid = "uuid"
  private val latestDecId = "latestDecId"
  private val latestVersionNo = "latestVersionNo"
  private val blockAmendments = "blockAmendments"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-4364 Add new fields to the Submission model for amended Declarations.",
      order = 17,
      author = "Lucio Biondi",
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")

    val filter = not(exists(latestDecId))
    val update =
      s"""{ "$$set": {
         | "$latestDecId": "$$$uuid",
         | "$latestVersionNo": NumberInt(1),
         | "$blockAmendments": false
         |} }""".stripMargin

    submissionCollection.updateMany(filter, List(BsonDocument(update)).asJava)
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}
