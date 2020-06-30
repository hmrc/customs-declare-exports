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
import uk.gov.hmrc.exports.migrations.exceptions.{LockCheckException, LockPersistenceException}

object LockManager {
  private val MIN_LOCK_ACQUIRED_FOR_MILLIS = 2L * 60L * 1000L // 2 minutes
  private val LOCK_REFRESH_MARGIN = 60L * 1000L // 1 minute
  private val DefaultKey = "DEFAULT_LOCK"
  private val MINIMUM_SLEEP_THREAD = 500L

  //long debug/info/error messages

  private val EXPIRATION_ARG_ERROR_MSG = "Lock expiration period must be greater than %d ms"
}

class LockManager(val repository: LockRepository, val timeUtils: TimeUtils) {
  //static constants
  private val logger = Logger(this.getClass)

  /**
    * Owner of the lock
    */
  private val owner = UUID.randomUUID.toString

  /**
    * <p>Maximum time it will wait for the lock in each try.</p>
    * <p>Default 3 minutes</p>
    */
  private var lockMaxWaitMillis = MIN_LOCK_ACQUIRED_FOR_MILLIS + (60 * 1000)

  /**
    * Maximum number of times it will try to take the lock if it's owned by some one else
    */
  private var lockMaxTries = 1

  /**
    * <p>The period of time for which the lock will be owned.</p>
    * <p>Default 2 minutes</p>
    */
  private var lockAcquiredForMillis = MIN_LOCK_ACQUIRED_FOR_MILLIS

  /**
    * Moment when will mandatory to acquire the lock again.
    */
  private var lockExpiresAt: Date = _

  /**
    * Number of tries
    */
  private var tries = 0

  def acquireLockDefault(): Unit = acquireLock(DefaultKey)

  private def acquireLock(lockKey: String): Unit = {
    var keepLooping = true
    do try {
      logger.info("Mongbee trying to acquire the lock")
      val newLockExpiresAt = timeUtils.currentTimePlusMillis(lockAcquiredForMillis)
      val lockEntry = new LockEntry(lockKey, LockStatus.LockHeld.name, owner, newLockExpiresAt)
      repository.insertUpdate(lockEntry)
      logger.info(s"Mongbee acquired the lock until: ${newLockExpiresAt}")
      updateStatus(newLockExpiresAt)
      keepLooping = false
    } catch {
      case _: LockPersistenceException =>
        handleLockException(true)
    } while ({
      keepLooping
    })
  }

  private def updateStatus(lockExpiresAt: Date): Unit = {
    this.lockExpiresAt = lockExpiresAt
    this.tries = 0
  }

  private def handleLockException(acquiringLock: Boolean): Unit = {
    this.tries += 1
    if (this.tries >= lockMaxTries) {
      updateStatus(null)
      throw new LockCheckException("MaxTries(" + lockMaxTries + ") reached")
    }
    val currentLock = repository.findByKey(DefaultKey)
    if (currentLock != null && !currentLock.isOwner(owner)) {
      val currentLockExpiresAt = currentLock.expiresAt
      logger.info(s"Lock is taken by other process until: ${currentLockExpiresAt}")
      if (!acquiringLock) throw new LockCheckException("Lock held by other process. Cannot ensure lock")
      waitForLock(currentLockExpiresAt)
    }
  }

  private def waitForLock(expiresAtMillis: Date): Unit = {
    val diffMillis = expiresAtMillis.getTime - timeUtils.currentTime.getTime
    val sleepingMillis = (if (diffMillis > 0) diffMillis else 0) + MINIMUM_SLEEP_THREAD
    try {
      if (sleepingMillis > lockMaxWaitMillis)
        throw new LockCheckException(maxWaitExceededErrorMsg(sleepingMillis))

      logger.info(goingToSleepMsg(sleepingMillis))
      Thread.sleep(sleepingMillis)
    } catch {
      case ex: InterruptedException =>
        logger.error("ERROR acquiring lock: {}", ex)
        Thread.currentThread.interrupt()
    }
  }

  private def maxWaitExceededErrorMsg(sleepingMillis: Long): String =
    s"Waiting time required(${sleepingMillis} ms) to take the lock is longer than maxWaitingTime(${lockMaxWaitMillis} ms)"

  private def goingToSleepMsg(sleepingMillis: Long) =
    s"ExportsMigrationTool is going to sleep to wait for the lock:  ${sleepingMillis} ms(${timeUtils.millisToMinutes(sleepingMillis)} minutes)"

  def releaseLockDefault(): Unit = releaseLock(DefaultKey)

  private def releaseLock(lockKey: String): Unit = {
    logger.info("ExportsMigrationTool is trying to release the lock.")
    repository.removeByKeyAndOwner(lockKey, this.owner)
    this.lockExpiresAt = null
    logger.info("ExportsMigrationTool released the lock")
  }

}
