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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.migrations.exceptions.LockManagerException
import uk.gov.hmrc.exports.migrations.repositories.{ChangeEntry, ChangeEntryRepository}

import java.time.Instant

class ExportsMigrationToolSpec extends UnitSpec {

  private val lockManager: LockManager = mock[LockManager]
  private val migrationsRegistry: MigrationsRegistry = mock[MigrationsRegistry]
  private val changeEntryRepository: ChangeEntryRepository = mock[ChangeEntryRepository]
  private val database: MongoDatabase = mock[MongoDatabase]
  private val migrationStatus: MigrationStatus = mock[MigrationStatus]

  private def exportsMigrationTool() =
    new ExportsMigrationTool(lockManager, migrationsRegistry, changeEntryRepository, database, migrationStatus)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(lockManager, migrationsRegistry, changeEntryRepository, database, migrationStatus)
    when(migrationsRegistry.migrations).thenReturn(Seq.empty)
    when(migrationStatus.isSuccess).thenReturn(true)
  }

  override def afterEach(): Unit = {
    reset(lockManager, migrationsRegistry, changeEntryRepository, database)

    super.afterEach()
  }

  "ExportsMigrationTool on execute" should {

    "call LockManager acquireLockDefault method" in {
      exportsMigrationTool().execute()
      verify(lockManager).acquireLockDefault()
    }

    "call LockManager releaseLockDefault method" in {
      exportsMigrationTool().execute()
      verify(lockManager).releaseLockDefault()
    }
  }

  "ExportsMigrationTool on execute" when {

    "MigrationsRegistry is empty" should {

      "not call LockManager ensureLockDefault" in {
        exportsMigrationTool().execute()
        verify(lockManager, never).ensureLockDefault()
      }

      "not call ChangeEntryRepository" in {
        exportsMigrationTool().execute()
        verifyNoInteractions(changeEntryRepository)
      }
    }

    "MigrationsRegistry contains new migration definition" should {

      val testMigrationInformation = MigrationInformation(id = "TestId", order = 1, author = "TestAuthor")
      val migrationDefinition = new MigrationDefinition {
        override val migrationInformation: MigrationInformation = testMigrationInformation
        override def migrationFunction(db: MongoDatabase): Unit = {}
      }

      "call ChangeEntryRepository findAll method" in {
        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition))
        when(changeEntryRepository.findAll()).thenReturn(List.empty)

        exportsMigrationTool().execute()

        verify(changeEntryRepository).findAll()
      }

      "call LockManager ensureLockDefault" in {
        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition))
        when(changeEntryRepository.findAll()).thenReturn(List.empty)

        exportsMigrationTool().execute()

        verify(lockManager).ensureLockDefault()
      }

      "call ChangeEntryRepository save method" in {
        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition))
        when(changeEntryRepository.findAll()).thenReturn(List.empty)

        exportsMigrationTool().execute()

        verify(changeEntryRepository).save(any[ChangeEntry])
      }
    }

    "MigrationsRegistry contains old migration definition with runAlways flag" should {
      val testMigrationInformation = MigrationInformation(id = "TestId", order = 1, author = "TestAuthor", runAlways = true)
      val migrationDefinition = new MigrationDefinition {
        override val migrationInformation: MigrationInformation = testMigrationInformation
        override def migrationFunction(db: MongoDatabase): Unit = {}
      }
      val currentChangeEntry = ChangeEntry(
        changeId = testMigrationInformation.id,
        author = testMigrationInformation.author,
        timestamp = Instant.now,
        changeLogClass = "testChangeLogClass"
      )

      "call ChangeEntryRepository findAll method" in {
        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition))
        when(changeEntryRepository.findAll()).thenReturn(List(currentChangeEntry.buildFullDBObject))

        exportsMigrationTool().execute()

        verify(changeEntryRepository).findAll()
      }

      "call LockManager ensureLockDefault" in {
        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition))
        when(changeEntryRepository.findAll()).thenReturn(List(currentChangeEntry.buildFullDBObject))

        exportsMigrationTool().execute()

        verify(lockManager).ensureLockDefault()
      }

      "not call ChangeEntryRepository save method" in {
        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition))
        when(changeEntryRepository.findAll()).thenReturn(List(currentChangeEntry.buildFullDBObject))

        exportsMigrationTool().execute()

        verify(changeEntryRepository, never).save(any[ChangeEntry])
      }
    }

    "MigrationsRegistry contains multiple new migration definitions" should {

      "execute them in order" in {
        def testMigrationInformation(order: Int) = MigrationInformation(id = "TestId", order = order, author = "TestAuthor")
        val migrationDefinition_1 = mock[MigrationDefinition]
        val migrationDefinition_2 = mock[MigrationDefinition]
        val migrationDefinition_3 = mock[MigrationDefinition]
        when(migrationDefinition_1.migrationInformation).thenReturn(testMigrationInformation(1))
        when(migrationDefinition_2.migrationInformation).thenReturn(testMigrationInformation(2))
        when(migrationDefinition_3.migrationInformation).thenReturn(testMigrationInformation(3))

        when(migrationsRegistry.migrations).thenReturn(Seq(migrationDefinition_2, migrationDefinition_3, migrationDefinition_1))
        when(changeEntryRepository.findAll()).thenReturn(List.empty)

        exportsMigrationTool().execute()

        val inOrder = Mockito.inOrder(migrationDefinition_1, migrationDefinition_2, migrationDefinition_3)
        inOrder.verify(migrationDefinition_1).migrationFunction(any[MongoDatabase])
        inOrder.verify(migrationDefinition_2).migrationFunction(any[MongoDatabase])
        inOrder.verify(migrationDefinition_3).migrationFunction(any[MongoDatabase])
      }
    }

    "LockManager throws LockManagerException" should {

      "not throw ExportsMigrationException and flag migrationStatus failure" in {
        val exceptionMessage = "Test Exception Message"
        when(lockManager.acquireLockDefault()).thenThrow(new LockManagerException(exceptionMessage))

        noException shouldBe thrownBy(exportsMigrationTool().execute())
        verify(migrationStatus).markFailed()
      }

      "call LockManager releaseLockDefault method and flag migrationStatus failure" in {
        val exceptionMessage = "Test Exception Message"
        when(lockManager.acquireLockDefault()).thenThrow(new LockManagerException(exceptionMessage))

        exportsMigrationTool().execute()

        verify(lockManager, atLeastOnce).releaseLockDefault()
        verify(migrationStatus).markFailed()
      }
    }

    "Any other exception is thrown during the migration process" should {
      "call LockManager releaseLockDefault method and flag migrationStatus failure" in {
        val exceptionMessage = "Test Exception Message"
        when(lockManager.acquireLockDefault()).thenThrow(new Exception(exceptionMessage))

        exportsMigrationTool().execute()

        verify(lockManager, atLeastOnce).releaseLockDefault()
        verify(migrationStatus).markFailed()
      }
    }
  }
}
