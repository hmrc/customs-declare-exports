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

import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, eq => feq, equal, not}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

class AddAssociatedSubmissionIdToDeclarations extends MigrationDefinition with Logging {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-5583 Add AssociatedSubmissionId to declaration meta data", order = 24, author = "Tim Wilkins")

  private val declarationMeta = "declarationMeta"
  private val status = "status"
  private val newFieldName = "associatedSubmissionId"
  private val parentDeclarationId = "parentDeclarationId"
  private val parentDeclarationEnhancedStatus = "parentDeclarationEnhancedStatus"
  private val batchSize = 100

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")
    implicit val declarationCollection = db.getCollection("declarations")
    implicit val declarationIdToSubmissionIdMap = mutable.Map.empty[String, String]

    submissionCollection
      .find()
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val submissionId = document.get("uuid").toString
        val actions = Option(document.getList("actions", classOf[Document])).map(_.asScala.toList).getOrElse(List.empty[Document])
        actions.map { action =>
          val maybeDecId = Option(action.get("decId"))
          maybeDecId.map(decId => declarationIdToSubmissionIdMap += (decId.toString -> submissionId))
        }
      }

    val filter = not(feq("declarationMeta.status", "DRAFT"))

    declarationCollection
      .find(filter)
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val decId = document.get("id").toString
        val declarationMetaDoc = document.get(declarationMeta, classOf[Document])
        val declarationStatus = declarationMetaDoc.get(status).toString
        val maybeParentDecId = Option(declarationMetaDoc.get(parentDeclarationId)).map(_.toString)
        val maybeParentDeclarationEnhancedStatus = Option(declarationMetaDoc.get(parentDeclarationEnhancedStatus)).map(_.toString)

        declarationStatus match {
          case "COMPLETE" =>
            populateNewField(document, declarationMetaDoc, decId)
          case "AMENDMENT_DRAFT" =>
            maybeParentDecId
              .fold(logger.warn(s"Amendment draft Declaration ${decId} has no parentDeclarationId!")) { parentDecId =>
                populateNewField(document, declarationMetaDoc, parentDecId)
              }
          case other =>
            logger.info(
              s"Ignoring declarationId $decId who's status is $other, has parentDecId == $maybeParentDecId, parentDeclarationEnhancedStatus == $maybeParentDeclarationEnhancedStatus"
            )
        }
      }

    logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
  }

  private def populateNewField(declarationDoc: Document, declarationMetaDoc: Document, associatedDecId: String)(
    implicit declarationIdToSubmissionIdMap: mutable.Map[String, String],
    declarationCollection: MongoCollection[Document]
  ) =
    declarationIdToSubmissionIdMap.get(associatedDecId) match {
      case None =>
        logger.warn(s"Declaration ${associatedDecId} has no matching submission!")

      case Some(associatedSubmissionId) =>
        val declarationMetaUpdated = declarationMetaDoc.append(newFieldName, associatedSubmissionId)
        declarationDoc.replace("declarationMeta", declarationMetaUpdated)

        val eori = declarationDoc.get("eori")
        val decId = declarationDoc.get("id").toString
        val filter = and(equal("eori", eori), equal("id", decId))
        declarationCollection.replaceOne(filter, declarationDoc)
    }
}
