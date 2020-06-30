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

import com.mongodb.client.MongoDatabase
import play.api.Logger
import uk.gov.hmrc.exports.migrations.changelogs.MigrationDefinition
import uk.gov.hmrc.exports.migrations.exceptions.{ExportsMigrationException, LockCheckException}

object ExportsMigrationTool {

  private val lockCollectionName = "exportsMigrationLock"
  private val changeEntryCollectionName = "exportsMigrationChangeLog"

  def apply(mongoDatabase: MongoDatabase): ExportsMigrationTool = {
    val migrationsRegistry = new MigrationsRegistry
    ExportsMigrationTool(mongoDatabase, migrationsRegistry)
  }

  def apply(mongoDatabase: MongoDatabase, migrationsRegistry: MigrationsRegistry): ExportsMigrationTool = {
    val lockRepository = new LockRepository(lockCollectionName, mongoDatabase)
    val lockManager = new LockManager(lockRepository, new TimeUtils)
    val changeEntryRepository = new ChangeEntryRepository(changeEntryCollectionName, mongoDatabase)

    new ExportsMigrationTool(lockManager, migrationsRegistry, changeEntryRepository, mongoDatabase)
  }
}

class ExportsMigrationTool(
  val lockManager: LockManager,
  val migrationsRegistry: MigrationsRegistry,
  val changeEntryRepository: ChangeEntryRepository,
  val database: MongoDatabase,
  val throwExceptionIfCannotObtainLock: Boolean = false
) {

  private val logger = Logger(this.getClass)

  private var enabled: Boolean = true

  def execute(): Unit =
    if (!isEnabled) {
      logger.info("ExportsMigrationTool is disabled. Exiting.")
    } else
      try {
        lockManager.acquireLockDefault()
        executeMigration()
      } catch {
        case lockEx: LockCheckException =>
          if (throwExceptionIfCannotObtainLock) {
            logger.error(lockEx.getMessage)
            throw new ExportsMigrationException(lockEx.getMessage)
          }
          logger.warn(lockEx.getMessage)
          logger.warn("ExportsMigrationTool did not acquire process lock. EXITING WITHOUT RUNNING DATA MIGRATION")
      } finally {
        lockManager.releaseLockDefault() //we do it anyway, it's idempotent

        logger.info("ExportsMigrationTool has finished his job.")
      }

  def isEnabled: Boolean = enabled
  private[migrations] def setEnabled(enabled: Boolean): Unit =
    this.enabled = enabled

  private def executeMigration(): Unit = {
    logger.info("ExportsMigrationTool starting the data migration sequence..")

    migrationsRegistry.migrations.sorted.foreach(executeIfNewOrRunAlways)
  }

  private def executeIfNewOrRunAlways(migrDefinition: MigrationDefinition): Unit = {
    val changeEntry = ChangeEntry(migrDefinition.migrationInformation)

    try {
      if (changeEntryRepository.isNewChange(changeEntry)) {
        migrDefinition.migrationFunction(database)
        changeEntryRepository.save(changeEntry)
        logger.info(s"${changeEntry} applied")

      } else if (isRunAlways(migrDefinition)) {
        migrDefinition.migrationFunction(database)
        logger.info(s"${changeEntry} re-applied")

      } else {
        logger.info(s"${changeEntry} pass over")
      }

    } catch {
      case exc: ExportsMigrationException => logger.error(exc.getMessage)
    }
  }

  private def isRunAlways(migrationDefinition: MigrationDefinition): Boolean = migrationDefinition.migrationInformation.runAlways

}
