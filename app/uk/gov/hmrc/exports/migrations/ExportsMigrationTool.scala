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

import com.mongodb.client.MongoDatabase
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.MigrationDefinition
import uk.gov.hmrc.exports.migrations.exceptions.{ExportsMigrationException, LockManagerException}
import uk.gov.hmrc.exports.migrations.repositories.ChangeEntry.{KeyAuthor, KeyChangeId}
import uk.gov.hmrc.exports.migrations.repositories.{ChangeEntry, ChangeEntryRepository, LockRefreshChecker, LockRepository}

class ExportsMigrationTool(
  val lockManager: LockManager,
  val migrationsRegistry: MigrationsRegistry,
  val changeEntryRepository: ChangeEntryRepository,
  val database: MongoDatabase,
  val throwExceptionIfCannotObtainLock: Boolean = false
) extends Logging {

  private var enabled: Boolean = true

  def execute(): Unit =
    if (!isEnabled) logger.info("ExportsMigrationTool is disabled. Exiting.")
    else
      try {
        lockManager.acquireLockDefault()
        executeMigration()
      } catch {
        case lockEx: LockManagerException =>
          if (throwExceptionIfCannotObtainLock) {
            logger.error(lockEx.getMessage)
            throw new ExportsMigrationException(lockEx.getMessage)
          }
          logger.warn(lockEx.getMessage)
          logger.warn("ExportsMigrationTool did not acquire process lock. EXITING WITHOUT RUNNING DATA MIGRATION")

        case exc: Throwable =>
          logger.error("ExportsMigrationTool - error on executing migration", exc)
      } finally {
        lockManager.releaseLockDefault() // we do it anyway, it's idempotent
        logger.info("ExportsMigrationTool has finished his job.")
      }

  def isEnabled: Boolean = enabled

  private[migrations] def setEnabled(enabled: Boolean): Unit = this.enabled = enabled

  private def executeMigration(): Unit = {
    logger.info(s"ExportsMigrationTool starting the data migration sequence.. ${migrationsRegistry.migrations.size}")
    migrationsRegistry.migrations.sorted.foreach(executeIfNewOrRunAlways)
  }

  private def executeIfNewOrRunAlways(migrDefinition: MigrationDefinition): Unit = {
    val changeEntry = ChangeEntry(migrDefinition)

    try
      if (isNewChange(changeEntry)) {
        lockManager.ensureLockDefault()
        migrDefinition.migrationFunction(database)
        changeEntryRepository.save(changeEntry)
        logger.info(s"${changeEntry} applied")

      } else if (isRunAlways(migrDefinition)) {
        lockManager.ensureLockDefault()
        migrDefinition.migrationFunction(database)
        logger.info(s"${changeEntry} re-applied")

      } else {
        logger.info(s"${changeEntry} pass over")
      }
    catch {
      case exc: ExportsMigrationException =>
        logger.error(s"Error while executing '${migrDefinition.migrationInformation}' ${exc.getMessage}")
    }
  }

  private def isNewChange(changeEntry: ChangeEntry): Boolean = !isExistingChange(changeEntry)

  private def isExistingChange(changeEntry: ChangeEntry): Boolean = changeEntryRepository.findAll().exists { doc =>
    doc.get(KeyChangeId) == changeEntry.changeId && doc.get(KeyAuthor) == changeEntry.author
  }

  private def isRunAlways(migrationDefinition: MigrationDefinition): Boolean = migrationDefinition.migrationInformation.runAlways
}

object ExportsMigrationTool {

  private val lockCollectionName = "exportsMigrationLock"
  private val changeEntryCollectionName = "exportsMigrationChangeLog"

  def apply(mongoDatabase: MongoDatabase, migrationsRegistry: MigrationsRegistry, lockManagerConfig: LockManagerConfig): ExportsMigrationTool = {
    val lockRepository = new LockRepository(lockCollectionName, mongoDatabase)
    val timeUtils = new TimeUtils
    val lockRefreshChecker = new LockRefreshChecker(timeUtils)
    val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, lockManagerConfig)
    val changeEntryRepository = new ChangeEntryRepository(changeEntryCollectionName, mongoDatabase)

    lockRepository.ensureIndex()
    changeEntryRepository.ensureIndex()

    new ExportsMigrationTool(lockManager, migrationsRegistry, changeEntryRepository, mongoDatabase)
  }
}
