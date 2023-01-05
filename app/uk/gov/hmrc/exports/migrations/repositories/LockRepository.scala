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

package uk.gov.hmrc.exports.migrations.repositories

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.{and, eq => feq, lt, or}
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.UpdateResult
import com.mongodb.{DuplicateKeyException, ErrorCategory, MongoWriteException}
import org.bson.Document
import org.bson.conversions.Bson
import uk.gov.hmrc.exports.migrations.exceptions.LockPersistenceException
import uk.gov.hmrc.exports.migrations.repositories.LockEntry.{ExpiresAtField, KeyField, OwnerField, StatusField}

import java.time.Instant
import scala.jdk.CollectionConverters._

class LockRepository(collectionName: String, db: MongoDatabase) extends MongoRepository(db, collectionName, Array(KeyField)) {

  /**
   * Retrieves a lock by key
   *
   * @param lockKey key
   * @return LockEntry
   */
  private[migrations] def findByKey(lockKey: String): Option[LockEntry] =
    collection
      .find(new Document().append(KeyField, lockKey))
      .iterator
      .asScala
      .toSeq
      .headOption
      .map(LockEntry(_))

  /**
   * Removes from database all the locks with the same key (only can be one) and owner
   *
   * @param lockKey lock key
   * @param owner   lock owner
   */
  private[migrations] def removeByKeyAndOwner(lockKey: String, owner: String): Unit =
    collection.deleteMany(and(feq(KeyField, lockKey), feq(OwnerField, owner)))

  /**
   * If there is a lock in the database with the same key, updates it if either is expired or both share the same owner.
   * If there is no lock with the same key, it's inserted.
   *
   * @param newLock lock to replace the existing one or be inserted.
   * @throws LockPersistenceException if there is a lock in database with same key, but belongs to another owner
   *                                   and is not expired or cannot insert/update the lock for any other reason
   */
  private[migrations] def insertUpdate(newLock: LockEntry): Unit =
    insertUpdate(newLock, onlyIfSameOwner = false)

  /**
   * If there is a lock in the database with the same key and owner, updates it. Otherwise throws a LockPersistenceException
   *
   * @param newLock lock to replace the existing one.
   * @throws LockPersistenceException if there is no lock in the database with the same key and owner or cannot update
   *                                  the lock for any other reason
   */
  private[migrations] def updateIfSameOwner(newLock: LockEntry): Unit =
    insertUpdate(newLock, onlyIfSameOwner = true)

  private def insertUpdate(newLock: LockEntry, onlyIfSameOwner: Boolean): Unit =
    try {
      val acquireLockQuery: Bson = getAcquireLockQuery(newLock.key, newLock.owner, onlyIfSameOwner)
      val result: UpdateResult = collection.updateMany(
        acquireLockQuery,
        new Document().append("$set", newLock.buildFullDBObject),
        new UpdateOptions().upsert(!onlyIfSameOwner)
      )
      if (result.getModifiedCount <= 0 && result.getUpsertedId == null)
        throw new LockPersistenceException("Lock is held")
    } catch {
      case ex: MongoWriteException if ex.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
        throw new LockPersistenceException("Lock is held")

      case _: DuplicateKeyException =>
        throw new LockPersistenceException("Lock is held")
    }

  private def getAcquireLockQuery(lockKey: String, owner: String, onlyIfSameOwner: Boolean): Bson = {
    val expiresAtCond: Bson = lt(ExpiresAtField, Instant.now)
    val ownerCond: Bson = feq(OwnerField, owner)
    val orCond: Bson = if (onlyIfSameOwner) {
      or(ownerCond)
    } else {
      or(expiresAtCond, ownerCond)
    }
    and(feq(KeyField, lockKey), feq(StatusField, LockStatus.LockHeld.name), orCond)
  }
}
