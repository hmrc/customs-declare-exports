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

package uk.gov.hmrc.exports.migrations

import akka.actor.{ActorSystem, Cancellable}
import com.github.cloudyrock.mongock.{Mongock, MongockBuilder}
import com.google.inject.Singleton
import com.mongodb.{MongoClient, MongoClientURI}
import javax.inject.Inject
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.exports.config.{AppConfig, ExportsMigrationConfig}
import uk.gov.hmrc.exports.mongock.MigrationExecutionContext

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class MigrationRunner @Inject()(
  appConfig: AppConfig,
  exportsMigrationConfig: ExportsMigrationConfig,
  actorSystem: ActorSystem,
  applicationLifecycle: ApplicationLifecycle
)(implicit mec: MigrationExecutionContext) {

  private val logger = Logger(this.getClass)

  private val uri = new MongoClientURI(appConfig.mongodbUri.replaceAllLiterally("sslEnabled", "ssl"))
  private val client = new MongoClient(uri)
  private val db = client.getDatabase(uri.getDatabase)

  val migrationTask: Cancellable = actorSystem.scheduler.scheduleOnce(0.seconds) {
    if (exportsMigrationConfig.isExportsMigrationEnabled) {
      logger.info("Exports Migration feature enabled. Starting migration with ExportsMigrationTool")
      migrateWithExportsMigrationTool()
    } else {
      logger.info("Exports Migration feature disabled. Starting migration with Mongock")
      migrateWithMongock()
    }
  }
  applicationLifecycle.addStopHook(() => Future.successful(migrationTask.cancel()))

  private def migrateWithMongock(): Unit = {
    val lockAcquiredForMinutes = 3
    val maxWaitingForLockMinutes = 5
    val maxTries = 10

    val runner: Mongock = new MongockBuilder(client, uri.getDatabase, "uk.gov.hmrc.exports.mongock.changesets")
      .setLockConfig(lockAcquiredForMinutes, maxWaitingForLockMinutes, maxTries)
      .build()

    runner.execute()
    runner.close()
  }

  private def migrateWithExportsMigrationTool(): Unit = {
    val migrationTool = ExportsMigrationTool(db)

    migrationTool.execute()
    client.close()
  }

}
