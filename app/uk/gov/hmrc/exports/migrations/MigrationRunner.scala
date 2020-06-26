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

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import com.google.inject.Singleton
import com.mongodb.{MongoClient, MongoClientURI}
import javax.inject.Inject
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.mongock.MigrationExecutionContext

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class MigrationRunner @Inject()(appConfig: AppConfig, actorSystem: ActorSystem, applicationLifecycle: ApplicationLifecycle)(
  implicit mec: MigrationExecutionContext
) {

  private val logger = Logger(this.getClass)

  private val uri = new MongoClientURI(appConfig.mongodbUri.replaceAllLiterally("sslEnabled", "ssl"))
  private val client = new MongoClient(uri)
  private val db = client.getDatabase(uri.getDatabase)

  private val migrationTool = ExportsMigrationTool(db)

  val migrationTask: Cancellable = actorSystem.scheduler.scheduleOnce(0.seconds) {

    val startTime = System.nanoTime()

    migrationTool.execute()
    client.close()

    val endTime = System.nanoTime()
    val executionTime = Duration(endTime - startTime, TimeUnit.NANOSECONDS)
    logger.info(s"Migration execution time = ${executionTime.toMillis} ms")
  }
  applicationLifecycle.addStopHook(() => Future.successful(migrationTask.cancel()))

}
