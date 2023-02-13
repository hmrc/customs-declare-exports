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
import com.mongodb.client.model.Updates.{combine, set}
import org.mongodb.scala.model.{Filters, UpdateOptions}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.declaration.submissions._

import java.util
import scala.jdk.CollectionConverters._

class AddActionFieldsForAmend extends MigrationDefinition with Logging {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-4446 Add new fields to the Action model for amended Declarations.",
      order = 18,
      author = "Darryl Murphy",
      runAlways = true
    )
  private val uuid = "uuid"
  private val latestDecId = "latestDecId"
  private val latestVersionNo = "latestVersionNo"

  override def migrationFunction(db: MongoDatabase): Unit = {

    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")

    val updated = submissionCollection
      .find()
      .asScala
      .map { document =>
        val submissionId = document.getString(uuid)
        val submissionLatestDecId = document.getString(latestDecId)
        val submissionLatestVersionNo = document.getInteger(latestVersionNo)

        submissionCollection
          .updateOne(
            Filters.eq(uuid, submissionId),
            combine(
              set("actions.$[submissionItem].decId", submissionId),
              set("actions.$[submissionItem].versionNo", 1),
              set("actions.$[cancellationItem].decId", submissionLatestDecId),
              set("actions.$[cancellationItem].versionNo", submissionLatestVersionNo)
            ),
            UpdateOptions()
              .arrayFilters(
                util.Arrays
                  .asList(
                    Filters.and(
                      Filters.eq("submissionItem.requestType", SubmissionRequest.toString),
                      Filters.exists("submissionItem.versionNo", false),
                      Filters.exists("submissionItem.decId", false)
                    ),
                    Filters.and(
                      Filters.eq("cancellationItem.requestType", CancellationRequest.toString),
                      Filters.exists("cancellationItem.versionNo", false),
                      Filters.exists("cancellationItem.decId", false)
                    )
                  )
              )
          )
          .getModifiedCount

      }
      .sum

    logger.info(s"Finished applying '${migrationInformation.id}' db migration with $updated updates.")

  }

}
