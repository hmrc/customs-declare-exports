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
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.migrations.TimeUtils.jsonWriter
import uk.gov.hmrc.exports.models.declaration.submissions.Submission

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class CheckAllSubmissionRecordsAreParsable extends MigrationDefinition with Logging {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-4523 Validate that all Submission records in mongo are parsable", order = 25, author = "Tim Wilkins")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val collection = db.getCollection("submissions")

    val batchSize = 100

    implicit val reads = Submission.format

    val result = collection
      .find()
      .batchSize(batchSize)
      .asScala
      .foldLeft((0,0)) { case ((totalsCounter, errorCounter), document) =>
      Try(Json.parse(document.toJson(jsonWriter)).as[Submission]) match {
        case Failure(exc) =>
          val id = document.get("_id")
          val error = exc.getMessage
          logger.error(s"Error parsing document with _id($id): $error")
          (totalsCounter + 1, errorCounter + 1)

        case Success(_) =>
          (totalsCounter + 1, errorCounter)
      }
    }

    logger.error(s"${result._2} Submission documents encountered with parsing errors out of ${result._1}.")
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}


