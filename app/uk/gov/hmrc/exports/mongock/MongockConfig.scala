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

package uk.gov.hmrc.exports.mongock

//import java.util
//import java.util.concurrent.TimeUnit
//
//import akka.actor.{ActorSystem, Cancellable}
//import com.github.cloudyrock.mongock.{Mongock, MongockBuilder}
//import com.google.inject.Singleton
//import com.mongodb.client.{MongoCollection, MongoDatabase}
//import com.mongodb.{MongoClient, MongoClientURI}
//import javax.inject.Inject
//import org.bson.Document
//import org.mongodb.scala.model.Filters.{and, exists, not, size, eq => feq}
//import org.mongodb.scala.model.UpdateOneModel
//import org.mongodb.scala.model.Updates.set
//import play.api.Logger
//import play.api.inject.ApplicationLifecycle
//import uk.gov.hmrc.exports.config.AppConfig
//import uk.gov.hmrc.exports.models.generators.{IdGenerator, StringIdGenerator}
//
//import scala.collection.JavaConversions._
//import scala.concurrent.Future
//import scala.concurrent.duration._
//
//@Singleton
//case class MongockConfig @Inject()(appConfig: AppConfig, actorSystem: ActorSystem, applicationLifecycle: ApplicationLifecycle)(
//  implicit mec: MigrationExecutionContext
//) {
//
//  private val logger = Logger(this.getClass)
//
//  val migrationTask: Cancellable = actorSystem.scheduler.scheduleOnce(0.seconds) {
//    migrateWithoutMongock()
//  }
//  applicationLifecycle.addStopHook(() => Future.successful(migrationTask.cancel()))
//
//  private def migrateWithMongock() = {
//    val uri = new MongoClientURI(appConfig.mongodbUri.replaceAllLiterally("sslEnabled", "ssl"))
//
//    val client = new MongoClient(uri)
//
//    val lockAcquiredForMinutes = 3
//    val maxWaitingForLockMinutes = 5
//    val maxTries = 10
//
//    val runner: Mongock = new MongockBuilder(client, uri.getDatabase, "uk.gov.hmrc.exports.mongock.changesets")
//      .setLockConfig(lockAcquiredForMinutes, maxWaitingForLockMinutes, maxTries)
//      .build()
//
//    val startTime = System.nanoTime()
//    runner.execute()
//    runner.close()
//
//    val endTime = System.nanoTime()
//    val executionTime = Duration(endTime - startTime, TimeUnit.NANOSECONDS)
//    logger.info(s"Migration execution time = ${executionTime.toMillis} ms")
//  }
//
//  private def migrateWithoutMongock() = {
//    val uri = new MongoClientURI(appConfig.mongodbUri.replaceAllLiterally("sslEnabled", "ssl"))
//    val client = new MongoClient(uri)
//    val db = client.getDatabase(uri.getDatabase)
//
//    val startTime = System.nanoTime()
//    migrationFunction(db)
//
//    val endTime = System.nanoTime()
//    val executionTime = Duration(endTime - startTime, TimeUnit.NANOSECONDS)
//    logger.info(s"Migration execution time = ${executionTime.toMillis} ms")
//  }
//
//  private def migrationFunction(db: MongoDatabase) = {
//    val INDEX_ID = "id"
//    val INDEX_EORI = "eori"
//
//    logger.info("Applying 'CEDS-2387 Add ID field to /items/packageInformation' db migration...")
//
//    val queryBatchSize = 2
//    val updateBatchSize = 10
//
//    getDeclarationsCollection(db)
//      .find(
//        and(
//          exists("items"),
//          not(size("items", 0)),
//          exists("items.packageInformation"),
//          not(size("items.packageInformation", 0)),
//          not(exists("items.packageInformation.id"))
//        )
//      )
//      .batchSize(queryBatchSize)
//      .toIterator
//      .map { document =>
//        val items = document.get("items", classOf[util.List[Document]])
//        val itemsUpdated: util.List[Document] = items.map(updateItem)
//        logger.debug(s"Items updated: $itemsUpdated")
//
//        val documentId = document.get(INDEX_ID).asInstanceOf[String]
//        val eori = document.get(INDEX_EORI).asInstanceOf[String]
//        val filter = and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori))
//        val update = set[util.List[Document]]("items", itemsUpdated)
//        logger.debug(s"[filter: $filter] [update: $update]")
//
//        new UpdateOneModel[Document](filter, update)
//      }
//      .grouped(updateBatchSize)
//      .zipWithIndex
//      .foreach {
//        case (requests, idx) =>
//          logger.info(s"ChangeSet 007. Updating batch no. $idx...")
//
//          getDeclarationsCollection(db).bulkWrite(requests)
//          logger.info(s"ChangeSet 007. Updated batch no. $idx")
//      }
//
//    logger.info("Applying 'CEDS-2387 Add ID field to /items/packageInformation' db migration... Done.")
//  }
//
//  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = {
//    val collectionName = "declarations"
//    db.getCollection(collectionName)
//  }
//
//  private def updateItem(itemDocument: Document): Document = {
//    val packageInformationElems = itemDocument.get("packageInformation", classOf[util.List[Document]])
//    val idGenerator: IdGenerator[String] = new StringIdGenerator
//
//    val packageInformationElemsUpdated = packageInformationElems.map(_.append("id", idGenerator.generateId()))
//
//    itemDocument.updated("packageInformation", packageInformationElemsUpdated)
//    new Document(itemDocument)
//  }
//
//}
