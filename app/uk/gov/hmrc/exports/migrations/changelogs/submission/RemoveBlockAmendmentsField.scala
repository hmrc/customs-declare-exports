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
import org.mongodb.scala.model.Filters.{and, equal, exists}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.jdk.CollectionConverters.IterableHasAsScala

class RemoveBlockAmendmentsField extends MigrationDefinition with Logging {

  private val blockAmendments = "blockAmendments"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-4548 Remove redundant blockAmendments field.",
      order = 21,
      author = "Tom Robinson",
      // Can be removed (default is false) once this migration is deployed to all envs (production & integration included)
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")

    val batchSize = 100

    submissionCollection
      .find(exists(blockAmendments))
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val uuid = document.get("uuid")
        val eori = document.get("eori")
        val filter = and(equal("eori", eori), equal("uuid", uuid))

        document.remove(blockAmendments)

        submissionCollection.replaceOne(filter, document)
      }

    logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
  }
}
