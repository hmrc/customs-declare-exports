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

package uk.gov.hmrc.exports.routines

import com.google.inject.Singleton
import com.mongodb.client.{MongoClient, MongoClients}
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig

import javax.inject.Inject
import scala.concurrent.Future

@Singleton
class DeleteMigrationCollectionsRoutine @Inject() (appConfig: AppConfig)(implicit rec: RoutinesExecutionContext) extends Routine with Logging {

  private val (client, mongoDatabase) = createMongoClient
  private val db = client.getDatabase(mongoDatabase)

  override def execute(): Future[Unit] = Future {
    logger.info("Starting cleanup to delete migration collections from mongo...")

    List("exportsMigrationLock", "exportsMigrationChangeLog").foreach { collection =>
      db.getCollection(collection).drop()
      logger.info(s"...dropped $collection collection from mongo")
    }
  }

  private def createMongoClient: (MongoClient, String) = {
    val (mongoUri, _) = {
      val sslParamPos = appConfig.mongodbUri.lastIndexOf('?'.toInt)
      if (sslParamPos > 0) appConfig.mongodbUri.splitAt(sslParamPos) else (appConfig.mongodbUri, "")
    }
    val (_, mongoDatabase) = mongoUri.splitAt(mongoUri.lastIndexOf('/'.toInt))
    (MongoClients.create(appConfig.mongodbUri), mongoDatabase.drop(1))
  }
}
