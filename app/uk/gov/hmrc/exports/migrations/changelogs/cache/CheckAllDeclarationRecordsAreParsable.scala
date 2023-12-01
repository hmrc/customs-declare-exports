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

package uk.gov.hmrc.exports.migrations.changelogs.cache

import com.mongodb.client.MongoDatabase
import play.api.libs.json._
import uk.gov.hmrc.exports.migrations.changelogs.{DeleteRecords, MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.migrations.TimeUtils.jsonWriter
import uk.gov.hmrc.exports.models.declaration._

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class CheckAllDeclarationRecordsAreParsable extends MigrationDefinition with DeleteRecords {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-4523 Validate that all ExportsDeclaration records in mongo are parsable",
      order = -1,
      author = "Tim Wilkins",
      true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val collection = db.getCollection("declarations")

    val batchSize = 100

    implicit val reads = Json.reads[ExportsDeclaration]

    val result = collection
      .find()
      .batchSize(batchSize)
      .asScala
      .foldLeft((0, 0)) { case ((totalsCounter, errorCounter), document) =>
        Try(Json.parse(document.toJson(jsonWriter)).as[ExportsDeclaration]) match {
          case Failure(exc) =>
            val id = document.get("_id")
            logger.error(s"Error parsing document with _id($id): ${exc.getMessage}")

            removeFromCollection(collection, id)
            (totalsCounter + 1, errorCounter + 1)

          case Success(_) =>
            (totalsCounter + 1, errorCounter)
        }
      }

    logger.error(s"${result._2} ExportsDeclaration documents encountered with parsing errors out of ${result._1}.")
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}
