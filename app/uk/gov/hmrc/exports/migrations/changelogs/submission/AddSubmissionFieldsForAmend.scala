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
import org.mongodb.scala.model.Filters.{and, equal, exists, not}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.jdk.CollectionConverters._

class AddSubmissionFieldsForAmend extends MigrationDefinition with Logging {

  private val latestDecId = "latestDecId"
  private val latestVersionNo = "latestVersionNo"
  private val blockAmendments = "blockAmendments"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-4364 Add new fields to the Submission model for amended Declarations.",
      order = 17,
      author = "Lucio Biondi",
      // Can be removed (default is false) once this migration is deployed to all envs (production & integration included)
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")

    val batchSize = 100

    submissionCollection
      .find(not(exists(latestDecId)))
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val uuid = document.get("uuid")
        document
          .append(latestDecId, uuid)
          .append(latestVersionNo, 1)
          .append(blockAmendments, false)

        val eori = document.get("eori")
        val filter = and(equal("eori", eori), equal("uuid", uuid))

        submissionCollection.replaceOne(filter, document)
      }
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}
