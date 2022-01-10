/*
 * Copyright 2022 HM Revenue & Customs
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

import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, exists, not, eq => feq}
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates.set
import play.api.Logger
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.collection.JavaConverters._

class MakeTransportPaymentMethodNotOptional extends MigrationDefinition {

  private val logger = Logger(this.getClass)

  private val INDEX_ID = "id"
  private val INDEX_EORI = "eori"
  private val TRANSPORT = "transport"
  private val TRANSPORT_PAYMENT = "transportPayment"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = "CEDS-3477 Change /transport/transportPayment/paymentMethod", order = 9, author = "Tim Wilkins", runAlways = true)

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val queryBatchSize = 10
    val updateBatchSize = 100

    asScalaIterator(
      getDeclarationsCollection(db)
        .find(and(exists("transport.transportPayment"), not(exists("transport.transportPayment.paymentMethod"))))
        .batchSize(queryBatchSize)
        .iterator
    ).map { document =>
      val transportDoc = document.get(TRANSPORT, classOf[Document])
      transportDoc.remove(TRANSPORT_PAYMENT)
      logger.debug(s"Updated: $transportDoc")

      val documentId = document.get(INDEX_ID).asInstanceOf[String]
      val eori = document.get(INDEX_EORI).asInstanceOf[String]
      val filter = and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori))
      val update = set(TRANSPORT, transportDoc)
      logger.debug(s"[filter: $filter] [update: $update]")

      new UpdateOneModel[Document](filter, update)
    }.grouped(updateBatchSize).zipWithIndex.foreach {
      case (requests, idx) =>
        logger.info(s"Updating batch no. $idx...")

        getDeclarationsCollection(db).bulkWrite(seqAsJavaList(requests))
        logger.info(s"Updated batch no. $idx")
    }

    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = {
    val collectionName = "declarations"
    db.getCollection(collectionName)
  }
}
