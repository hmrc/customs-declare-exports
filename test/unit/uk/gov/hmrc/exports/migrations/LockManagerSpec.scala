/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.Date

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.migrations.LockManager.DefaultKey
import uk.gov.hmrc.exports.migrations.exceptions.{LockManagerException, LockPersistenceException}
import uk.gov.hmrc.exports.migrations.repositories.{LockEntry, LockRefreshChecker, LockRepository, LockStatus}

class LockManagerSpec extends UnitSpec {

  private val lockRepository = mock[LockRepository]
  private val lockRefreshChecker = mock[LockRefreshChecker]
  private val timeUtils = mock[TimeUtils]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(lockRepository, lockRefreshChecker, timeUtils)
    when(timeUtils.currentTime).thenCallRealMethod()
    when(timeUtils.minutesToMillis(any())).thenCallRealMethod()
    when(timeUtils.millisToMinutes(any())).thenCallRealMethod()
  }

  override def afterEach(): Unit = {
    reset(lockRepository, lockRefreshChecker, timeUtils)

    super.afterEach()
  }

  private def lockEntryPassedToLockRepositoryOnAcquireLock: LockEntry = {
    val captor: ArgumentCaptor[LockEntry] = ArgumentCaptor.forClass(classOf[LockEntry])
    verify(lockRepository).insertUpdate(captor.capture())
    captor.getValue
  }

  private def lockEntryPassedToLockRepositoryOnEnsureLock: LockEntry = {
    val captor: ArgumentCaptor[LockEntry] = ArgumentCaptor.forClass(classOf[LockEntry])
    verify(lockRepository).updateIfSameOwner(captor.capture())
    captor.getValue
  }

  private val lockMaxTries = 3
  private val lockAliveTimeMillis = 500L
  private val config = LockManagerConfig(
    lockMaxTries = lockMaxTries,
    lockMaxWaitMillis = 2 * lockAliveTimeMillis,
    lockAcquiredForMillis = lockAliveTimeMillis,
    minimumSleepThreadMillis = 100L
  )
  private val newLockExpiryDate = new Date(System.currentTimeMillis + lockAliveTimeMillis)
  private val currentLockExpiryDate = newLockExpiryDate

  "LockManager on acquireLockDefault" should {
    "provide LockRepository with correct LockEntry" in {

      when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
      doNothing.when(lockRepository).insertUpdate(any())

      val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

      lockManager.acquireLockDefault()

      lockEntryPassedToLockRepositoryOnAcquireLock.key mustBe DefaultKey
      lockEntryPassedToLockRepositoryOnAcquireLock.status mustBe LockStatus.LockHeld.name
      lockEntryPassedToLockRepositoryOnAcquireLock.owner mustNot be(empty)
      lockEntryPassedToLockRepositoryOnAcquireLock.expiresAt must be(newLockExpiryDate)
    }
  }

  "LockManager on acquireLockDefault" when {

    "no exception thrown by LockRepository (no lock in DB, lock belongs to this LockManager, or to some other but is expired)" should {
      "call LockRepository only once" in {

        when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
        doNothing.when(lockRepository).insertUpdate(any())

        val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

        lockManager.acquireLockDefault()

        verify(lockRepository).insertUpdate(any())
      }
    }

    "LockPersistenceException is thrown by LockRepository" when {

      "current lock belongs to different owner and it's not expired" should {

        "call LockRepository up to maxTries times" in {

          when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
          val currentLock = LockEntry(DefaultKey, LockStatus.LockHeld.name, "OtherLockOwner", currentLockExpiryDate)
          when(lockRepository.insertUpdate(any())).thenThrow(new LockPersistenceException("Lock is held"))
          when(lockRepository.findByKey(anyString())).thenReturn(Some(currentLock))

          val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

          intercept[LockManagerException](lockManager.acquireLockDefault())

          verify(lockRepository, times(lockMaxTries)).insertUpdate(any())
        }

        "throw LockCheckException('MaxTries(_) reached') after not succeeding any try" in {

          when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
          val currentLock = LockEntry(DefaultKey, LockStatus.LockHeld.name, "OtherLockOwner", currentLockExpiryDate)
          when(lockRepository.insertUpdate(any())).thenThrow(new LockPersistenceException("Lock is held"))
          when(lockRepository.findByKey(anyString())).thenReturn(Some(currentLock))

          val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

          intercept[LockManagerException](lockManager.acquireLockDefault()).getMessage mustBe s"MaxTries($lockMaxTries) reached"
        }

        "throw LockCheckException('Waiting time required...') if lock's expiry date is further in the future than lockMaxWaitMillis" in {

          when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)

          val currentLockExpiryDate = new Date(newLockExpiryDate.getTime + 10 * config.lockMaxWaitMillis)
          val currentLock = LockEntry(DefaultKey, LockStatus.LockHeld.name, "OtherLockOwner", currentLockExpiryDate)
          when(lockRepository.insertUpdate(any())).thenThrow(new LockPersistenceException("Lock is held"))
          when(lockRepository.findByKey(anyString())).thenReturn(Some(currentLock))

          val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

          intercept[LockManagerException](lockManager.acquireLockDefault()).getMessage must include("Waiting time required")
        }
      }
    }
  }

  "LockManager on ensureLockDefault" when {

    "does need to refresh the lock" should {
      "provide LockRepository with correct LockEntry" in {

        when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
        doNothing.when(lockRepository).updateIfSameOwner(any())
        when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(true)

        val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

        lockManager.ensureLockDefault()

        lockEntryPassedToLockRepositoryOnEnsureLock.key mustBe DefaultKey
        lockEntryPassedToLockRepositoryOnEnsureLock.status mustBe LockStatus.LockHeld.name
        lockEntryPassedToLockRepositoryOnEnsureLock.owner mustNot be(empty)
        lockEntryPassedToLockRepositoryOnEnsureLock.expiresAt must be(newLockExpiryDate)
      }
    }
  }

  "LockManager on ensureLockDefault" when {

    "does NOT need to refresh the lock" should {
      "not call LockRepository" in {

        when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(false)

        val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

        lockManager.ensureLockDefault()

        verifyNoInteractions(lockRepository)
      }
    }

    "does need to refresh the lock" when {

      "LockRepository does not throw any exceptions (there is a lock belonging to this LockManager)" should {
        "call LockRepository only once" in {

          when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(true)
          when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
          doNothing.when(lockRepository).updateIfSameOwner(any())

          val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

          lockManager.ensureLockDefault()

          verify(lockRepository).updateIfSameOwner(any())
        }
      }

      "LockPersistenceException is thrown by LockRepository" when {

        "there is no lock in the DB" should {

          "call LockRepository maxTries times" in {

            when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(true)
            when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
            when(lockRepository.updateIfSameOwner(any())).thenThrow(new LockPersistenceException("Lock is held"))
            when(lockRepository.findByKey(anyString())).thenReturn(None)

            val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

            intercept[LockManagerException](lockManager.ensureLockDefault())

            verify(lockRepository, times(lockMaxTries)).updateIfSameOwner(any())
          }

          "throw LockCheckException('MaxTries(_) reached')" in {

            when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(true)
            when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
            when(lockRepository.updateIfSameOwner(any())).thenThrow(new LockPersistenceException("Lock is held"))
            when(lockRepository.findByKey(anyString())).thenReturn(None)

            val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

            intercept[LockManagerException](lockManager.ensureLockDefault()).getMessage mustBe s"MaxTries($lockMaxTries) reached"
          }
        }

        "there is a lock belonging to some other LockManager (expired or not)" should {

          "call LockRepository only once" in {

            when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(true)
            when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
            val currentLock = LockEntry(DefaultKey, LockStatus.LockHeld.name, "OtherLockOwner", currentLockExpiryDate)
            when(lockRepository.updateIfSameOwner(any())).thenThrow(new LockPersistenceException("Lock is held"))
            when(lockRepository.findByKey(anyString())).thenReturn(Some(currentLock))

            val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

            intercept[LockManagerException](lockManager.ensureLockDefault())

            verify(lockRepository).updateIfSameOwner(any())
          }

          "throw LockCheckException('Lock held by other process. Cannot ensure lock')" in {

            when(lockRefreshChecker.needsRefreshLock(any())).thenReturn(true)
            when(timeUtils.currentTimePlusMillis(any())).thenReturn(newLockExpiryDate)
            val currentLock = LockEntry(DefaultKey, LockStatus.LockHeld.name, "OtherLockOwner", currentLockExpiryDate)
            when(lockRepository.updateIfSameOwner(any())).thenThrow(new LockPersistenceException("Lock is held"))
            when(lockRepository.findByKey(anyString())).thenReturn(Some(currentLock))

            val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

            intercept[LockManagerException](lockManager.ensureLockDefault()).getMessage mustBe "Lock held by other process. Cannot ensure lock"
          }
        }
      }
    }
  }

  "LockManager on releaseLockDefault" should {

    "call LockRepository" in {

      val lockManager = new LockManager(lockRepository, lockRefreshChecker, timeUtils, config)

      lockManager.releaseLockDefault()

      verify(lockRepository).removeByKeyAndOwner(meq(DefaultKey), any())
    }
  }

}
