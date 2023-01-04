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
import org.bson.Document
import org.mongodb.scala.model.Filters.{equal, exists}
import org.mongodb.scala.model.Updates.{combine, set, unset}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.jdk.CollectionConverters._

class RemoveMeansOfTransportCrossingTheBorderNationality extends MigrationDefinition with Logging {

  private val docId = "id"
  private val transportObject = "transport"
  private val oldField = "meansOfTransportCrossingTheBorderNationality"
  private val newSubObject = "transportCrossingTheBorderNationality"
  private val newField = "countryName"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-3836 Remove $oldField field. Transfer values to transportCrossingTheBorderNationality field if empty.",
      order = 15,
      author = "Tom Robinson",
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val declarationsCollection = db.getCollection("declarations")
    val filterForOldField = exists(s"$transportObject.$oldField")

    getDocumentsToUpdate.toList.map { document =>
      val transportDocument = document.get(transportObject, classOf[Document])
      val oldFieldValue = transportDocument.get(oldField)

      val isNewFieldAbsent = !transportDocument.containsKey(newSubObject)

      val documentId = document.get(docId)
      val docFilter = equal(docId, documentId)

      val updates =
        if (isNewFieldAbsent) combine(set(s"$transportObject.$newSubObject.$newField", oldFieldValue), unset(s"$transportObject.$oldField"))
        else unset(s"$transportObject.$oldField")

      declarationsCollection.updateOne(docFilter, updates)
    }

    def getDocumentsToUpdate: Iterable[Document] =
      declarationsCollection
        .find(filterForOldField)
        .asScala

    logger.info(s"Applying '${migrationInformation.id}' db migration complete.")
  }
}
