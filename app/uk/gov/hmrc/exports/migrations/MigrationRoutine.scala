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

package uk.gov.hmrc.exports.migrations

import com.google.inject.Singleton
import com.mongodb.client.{MongoClient, MongoClients}
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.migrations.changelogs.cache._
import uk.gov.hmrc.exports.migrations.changelogs.emaildetails.RenameSendEmailDetailsToItem
import uk.gov.hmrc.exports.migrations.changelogs.notification.{MakeParsedDetailsOptional, SplitTheNotificationsCollection}
import uk.gov.hmrc.exports.migrations.changelogs.submission._

import javax.inject.Inject

@Singleton
class MigrationRoutine @Inject() (appConfig: AppConfig) extends Logging {

  logger.info("Starting migration with ExportsMigrationTool")

  private val (client, mongoDatabase) = createMongoClient
  private val db = client.getDatabase(mongoDatabase)

  private val lockMaxWaitMillis = minutesToMillis(5)
  private val lockAcquiredForMillis = minutesToMillis(3)

  private val lockMaxTries = 10
  private val lockManagerConfig = LockManagerConfig(lockMaxTries, lockMaxWaitMillis, lockAcquiredForMillis)

  private val migrationsRegistry = MigrationsRegistry()
    .register(new MakeParsedDetailsOptional())
    .register(new SplitTheNotificationsCollection())
    .register(new RenameToAdditionalDocuments())
    .register(new MakeTransportPaymentMethodNotOptional())
    .register(new RemoveRedundantIndexes())
    .register(new RenameSendEmailDetailsToItem())
    .register(new AddNotificationSummariesToSubmissions())
    .register(new RemoveMeansOfTransportCrossingTheBorderNationality())
    .register(new AddDeclarationMetaEntity())
    .register(new AddSubmissionFieldsForAmend())
    .register(new AddSequencingOfMultipleItems())
    .register(new AddActionFieldsForAmend())
    .register(new AddSequenceIdsToPackageInformation())
    .register(new RemoveBlockAmendmentsField())

  ExportsMigrationTool(db, migrationsRegistry, lockManagerConfig).execute()

  client.close()

  private def createMongoClient: (MongoClient, String) = {
    val (mongoUri, _) = {
      val sslParamPos = appConfig.mongodbUri.lastIndexOf('?'.toInt)
      if (sslParamPos > 0) appConfig.mongodbUri.splitAt(sslParamPos) else (appConfig.mongodbUri, "")
    }
    val (_, mongoDatabase) = mongoUri.splitAt(mongoUri.lastIndexOf('/'.toInt))
    (MongoClients.create(appConfig.mongodbUri), mongoDatabase.drop(1))
  }

  private def minutesToMillis(minutes: Int): Long = minutes * 60L * 1000L
}
