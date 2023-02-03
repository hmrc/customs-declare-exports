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
import org.mongodb.scala.model.Filters.{equal, exists, not}
import org.mongodb.scala.model.Updates.{combine, set, unset}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.jdk.CollectionConverters.IterableHasAsScala

class AddDeclarationMetaEntity extends MigrationDefinition with Logging {

  private val docId = "id"
  private val declarationMeta = "declarationMeta"
  private val parentDeclarationId = "parentDeclarationId"
  private val parentDeclarationEnhancedStatus = "parentDeclarationEnhancedStatus"
  private val status = "status"
  private val createdDateTime = "createdDateTime"
  private val updatedDateTime = "updatedDateTime"
  private val summaryWasVisited = "summaryWasVisited"
  private val readyForSubmission = "readyForSubmission"
  private val allFields =
    List(parentDeclarationId, parentDeclarationEnhancedStatus, status, createdDateTime, updatedDateTime, summaryWasVisited, readyForSubmission)

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-4371 Add DeclarationMeta entity. Move several declaration metadata fields inside it.",
      order = 16,
      author = "Tom Robinson",
      runAlways = false
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val declarationsCollection = db.getCollection("declarations")
    val filterForAbsenceOfDeclarationMeta = not(exists(declarationMeta))

    getDocumentsToUpdate.foreach { document =>
      def addToDocumentIfPresent(newMetaDoc: Document, field: String): Document = {
        val fieldValue = Option(document.get(field))
        fieldValue.fold(newMetaDoc)(value => newMetaDoc.append(field, value))
      }

      val declarationMetaDoc: Document = allFields.map(field => addToDocumentIfPresent(_, field)).foldLeft(new Document())((doc, f) => f(doc))

      val update = combine(allFields.map(unset) :+ set(declarationMeta, declarationMetaDoc): _*)

      val documentId = document.get(docId)
      val docFilter = equal(docId, documentId)

      declarationsCollection.updateOne(docFilter, update)
    }

    def getDocumentsToUpdate: Iterable[Document] =
      declarationsCollection
        .find(filterForAbsenceOfDeclarationMeta)
        .asScala
        .toList
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}
