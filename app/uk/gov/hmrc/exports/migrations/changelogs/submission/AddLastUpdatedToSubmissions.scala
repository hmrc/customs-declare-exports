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
import org.mongodb.scala.model.Filters._
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.util.TimeUtils.instant

import scala.jdk.CollectionConverters._

class AddLastUpdatedToSubmissions extends MigrationDefinition with Logging {

  private val lastUpdated = "lastUpdated"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-5446 Add a 'lastUpdated' field to those Submissions where it is missing", order = 23, author = "Lucio Biondi")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")

    val batchSize = 100

    submissionCollection
      .find(not(exists(lastUpdated)))
      .batchSize(batchSize)
      .asScala
      .map { document =>
        document.append(lastUpdated, instant())

        val eori = document.get("eori")
        val uuid = document.get("uuid")
        val filter = and(equal("eori", eori), equal("uuid", uuid))

        submissionCollection.replaceOne(filter, document)
      }
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}
