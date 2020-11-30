/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.migrations.changelogs.notification

import java.util

import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, exists, or, eq => feq}
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates.{combine, set, unset}
import play.api.Logger
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.collection.JavaConverters._

class MakeParsedDetailsOptional extends MigrationDefinition {

  private val logger = Logger(this.getClass)

  private val INDEX_ID = "_id"
  private val MRN = "mrn"
  private val DATE_TIME_ISSUED = "dateTimeIssued"
  private val STATUS = "status"
  private val ERRORS = "errors"
  private val DETAILS = "details"

  private val collectionName = "notifications"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = "CEDS-2800 Make the parsed detail fields mrn, dateTimeIssued, status & error optional",
      order = 1,
      author = "Tim Wilkins",
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val query = or(exists(MRN), exists(DATE_TIME_ISSUED), exists(STATUS), exists(ERRORS))
    val queryBatchSize = 10
    val updateBatchSize = 100

    def createDetailsSubDoc(document: Document) = {
      val mrn = document.get(MRN).asInstanceOf[String]
      val dateTimeIssued = document.get(DATE_TIME_ISSUED).asInstanceOf[String]
      val status = document.get(STATUS).asInstanceOf[String]
      val errors = document.get(ERRORS, classOf[util.List[Document]])

      new Document()
        .append(MRN, mrn)
        .append(DATE_TIME_ISSUED, dateTimeIssued)
        .append(STATUS, status)
        .append(ERRORS, errors)
    }

    getDocumentsToUpdate(db, query, queryBatchSize).map { document =>
      val documentId = document.get(INDEX_ID)

      val detailsSubDoc = createDetailsSubDoc(document)
      logger.info(s"Creating new sub-document: $detailsSubDoc for documentId=$documentId")

      val filter = and(feq(INDEX_ID, documentId))
      val update = combine(unset(MRN), unset(DATE_TIME_ISSUED), unset(STATUS), unset(ERRORS), set(DETAILS, detailsSubDoc))
      logger.info(s"[filter: $filter] [update: $update]")

      new UpdateOneModel[Document](filter, update)
    }.grouped(updateBatchSize).zipWithIndex.foreach {
      case (requests, idx) =>
        logger.info(s"Updating batch no. $idx...")

        db.getCollection(collectionName).bulkWrite(seqAsJavaList(requests))
        logger.info(s"Updated batch no. $idx")
    }

    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def getDocumentsToUpdate(db: MongoDatabase, filter: Bson, queryBatchSize: Int) =
    asScalaIterator(
      db.getCollection(collectionName)
        .find(filter)
        .batchSize(queryBatchSize)
        .iterator
    )
}
