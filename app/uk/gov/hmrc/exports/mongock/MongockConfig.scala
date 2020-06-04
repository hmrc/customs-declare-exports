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

import akka.actor.{ActorSystem, Cancellable}
import com.github.cloudyrock.mongock.{Mongock, MongockBuilder}
import com.google.inject.Singleton
import com.mongodb.{MongoClient, MongoClientURI}
import javax.inject.Inject
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.exports.config.AppConfig

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
case class MongockConfig @Inject()(appConfig: AppConfig, actorSystem: ActorSystem, applicationLifecycle: ApplicationLifecycle)(
  implicit mec: MigrationExecutionContext
) {

  val migrationTask: Cancellable = actorSystem.scheduler.scheduleOnce(0.seconds) {
    migrateWithMongock()
  }
  applicationLifecycle.addStopHook(() => Future.successful(migrationTask.cancel()))

  private def migrateWithMongock() = {
    val uri = new MongoClientURI(appConfig.mongodbUri.replaceAllLiterally("sslEnabled", "ssl"))

    val client = new MongoClient(uri)

    val lockAcquiredForMinutes = 3
    val maxWaitingForLockMinutes = 5
    val maxTries = 10

    val runner: Mongock = new MongockBuilder(client, uri.getDatabase, "uk.gov.hmrc.exports.mongock.changesets")
      .setLockConfig(lockAcquiredForMinutes, maxWaitingForLockMinutes, maxTries)
      .build()

    runner.execute()
    runner.close()
  }

}
