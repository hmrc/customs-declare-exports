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

import java.util.{Date, UUID}

import play.api.Logger
import uk.gov.hmrc.exports.migrations.LockManager._
import uk.gov.hmrc.exports.migrations.exceptions.{LockManagerException, LockPersistenceException}
import uk.gov.hmrc.exports.migrations.repositories.{LockEntry, LockRefreshChecker, LockRepository, LockStatus}

object LockManager {
  val DefaultKey: String = "DEFAULT_LOCK"
  val MinLockAcquiredForMillis: Long = 2L * 60L * 1000L // 2 minutes
  val LockRefreshMarginMillis: Long = 60L * 1000L // 1 minute
  val MinimumSleepThreadMillisDefault: Long = 500L

  case class LockManagerConfig(
    lockMaxTries: Int = 1,
    lockMaxWaitMillis: Long = MinLockAcquiredForMillis + 60L * 1000L,
    lockAcquiredForMillis: Long = MinLockAcquiredForMillis,
    minimumSleepThreadMillis: Long = MinimumSleepThreadMillisDefault
  )
}

class LockManager(
  val repository: LockRepository,
  val lockRefreshChecker: LockRefreshChecker,
  val timeUtils: TimeUtils,
  val config: LockManagerConfig
) {

  private val logger = Logger(this.getClass)
  private val lockOwner = UUID.randomUUID.toString

  private var lockExpiresAt: Date = _
  private var tries = 0

  def acquireLockDefault(): Unit = acquireLock(DefaultKey)

  private def acquireLock(lockKey: String): Unit = {
    var keepLooping = true
    do try {
      logger.info("ExportsMigrationTool trying to acquire the lock")
      val newLockExpiresAt = timeUtils.currentTimePlusMillis(config.lockAcquiredForMillis)
      val lockEntry = new LockEntry(lockKey, LockStatus.LockHeld.name, lockOwner, newLockExpiresAt)
      repository.insertUpdate(lockEntry)
      updateStatus(newLockExpiresAt)
      logger.info(s"ExportsMigrationTool acquired the lock until: ${newLockExpiresAt}")
      keepLooping = false
    } catch {
      case _: LockPersistenceException =>
        handleLockException(true)
    } while ({
      keepLooping
    })
  }

  private[migrations] def ensureLockDefault(): Unit = ensureLock(DefaultKey)

  private def ensureLock(lockKey: String): Unit = {
    var keepLooping = true
    // The first check will pass when there is no lock in the DB,
    // but later it is not created, so it ends up in a loop doing nothing useful until lockMaxTries
    do if (lockRefreshChecker.needsRefreshLock(lockExpiresAt)) try {
      logger.info("ExportsMigrationTool trying to refresh the lock")
      val lockExpiresAtTemp = timeUtils.currentTimePlusMillis(config.lockAcquiredForMillis)
      val lockEntry = new LockEntry(lockKey, LockStatus.LockHeld.name, lockOwner, lockExpiresAtTemp)
      repository.updateIfSameOwner(lockEntry)
      updateStatus(lockExpiresAtTemp)
      logger.info(s"ExportsMigrationTool refreshed the lock until: ${lockExpiresAtTemp}")
      keepLooping = false
    } catch {
      case _: LockPersistenceException =>
        handleLockException(false)
    } else keepLooping = false while ({
      keepLooping
    })
  }

  private def updateStatus(lockExpiresAt: Date): Unit = {
    this.lockExpiresAt = lockExpiresAt
    this.tries = 0
  }

  private def handleLockException(acquiringLock: Boolean): Unit = {
    this.tries += 1
    if (this.tries >= config.lockMaxTries) {
      updateStatus(null)
      throw new LockManagerException("MaxTries(" + config.lockMaxTries + ") reached")
    }
    val currentLock = repository.findByKey(DefaultKey)
    // Check for ownership
    if (currentLock != null && !currentLock.isOwner(lockOwner)) {
      val currentLockExpiresAt = currentLock.expiresAt
      logger.info(s"Lock is taken by other process until: ${currentLockExpiresAt}")
      if (!acquiringLock) throw new LockManagerException("Lock held by other process. Cannot ensure lock")
      waitForLock(currentLockExpiresAt)
    }
  }

  private def waitForLock(expiresAtMillis: Date): Unit = {
    val diffMillis = expiresAtMillis.getTime - timeUtils.currentTime.getTime
    val sleepingMillis = (if (diffMillis > 0) diffMillis else 0) + MinimumSleepThreadMillisDefault
    try {
      if (sleepingMillis > config.lockMaxWaitMillis)
        throw new LockManagerException(maxWaitExceededErrorMsg(sleepingMillis))

      logger.info(goingToSleepMsg(sleepingMillis))
      Thread.sleep(sleepingMillis)
    } catch {
      case ex: InterruptedException =>
        logger.error("ERROR acquiring lock: {}", ex)
        Thread.currentThread.interrupt()
    }
  }

  private def maxWaitExceededErrorMsg(sleepingMillis: Long): String =
    s"Waiting time required (${sleepingMillis} ms) to take the lock is longer than maxWaitingTime (${config.lockMaxWaitMillis} ms)"

  private def goingToSleepMsg(sleepingMillis: Long): String =
    s"ExportsMigrationTool is going to sleep to wait for the lock:  ${sleepingMillis} ms (${timeUtils.millisToMinutes(sleepingMillis)} minutes)"

  def releaseLockDefault(): Unit = releaseLock(DefaultKey)

  private def releaseLock(lockKey: String): Unit = {
    logger.info("ExportsMigrationTool is trying to release the lock.")
    repository.removeByKeyAndOwner(lockKey, this.lockOwner)
    this.lockExpiresAt = null
    logger.info("ExportsMigrationTool released the lock")
  }

}
