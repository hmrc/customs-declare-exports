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

package uk.gov.hmrc.exports.migrations.changelogs.emaildetails

import com.mongodb.client.MongoDatabase
import org.mongodb.scala.model.Filters.exists
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates.rename
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

class RenameSendEmailDetailsToItem extends MigrationDefinition with Logging {

  private val collectionName = "sendEmailWorkItems"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = "CEDS-3825 Rename sendEmailWorkItems.sendEmailDetails to sendEmailWorkItems.item",
      order = 12,
      author = "Lucio Biondi",
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val collection = db.getCollection(collectionName)

    val result = collection.updateMany(exists("sendEmailDetails"), rename("sendEmailDetails", "item"), new UpdateOptions().upsert(false))

    logger.info(s"Updated ${result.getModifiedCount} documents")
    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }
}
