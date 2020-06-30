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

package uk.gov.hmrc.exports.migrations.repositories

import java.util.Date

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.{and, lt, or, eq => feq}
import com.mongodb.client.model.{Filters, UpdateOptions}
import com.mongodb.client.result.UpdateResult
import com.mongodb.{DuplicateKeyException, ErrorCategory, MongoWriteException}
import org.bson.Document
import org.bson.conversions.Bson
import uk.gov.hmrc.exports.migrations.exceptions.LockPersistenceException

class LockRepository(collectionName: String, db: MongoDatabase) extends MongoRepository(db, collectionName, Array(LockEntry.KeyField)) {

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

  /**
    * Retrieves a lock by key
    *
    * @param lockKey key
    * @return LockEntry
    */
  private[migrations] def findByKey(lockKey: String): LockEntry = {
    val result: Document = collection.find(new Document().append(LockEntry.KeyField, lockKey)).first
    if (result != null) {
      new LockEntry(
        result.getString(LockEntry.KeyField),
        result.getString(LockEntry.StatusField),
        result.getString(LockEntry.OwnerField),
        result.getDate(LockEntry.ExpiresAtField)
      )
    } else {
      null
    }
  }

  /**
    * Removes from database all the locks with the same key(only can be one) and owner
    *
    * @param lockKey lock key
    * @param owner   lock owner
    */
  private[migrations] def removeByKeyAndOwner(lockKey: String, owner: String): Unit =
    collection.deleteMany(and(Filters.eq(LockEntry.KeyField, lockKey), Filters.eq(LockEntry.OwnerField, owner)))

  private def insertUpdate(newLock: LockEntry, onlyIfSameOwner: Boolean): Unit =
//    var lockHeld: Boolean = false
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

//      case ex: MongoWriteException =>
//        lockHeld = ex.getError.getCategory eq ErrorCategory.DUPLICATE_KEY
//        if (!lockHeld) {
//          throw ex
//        }
//      case _: DuplicateKeyException =>
//        lockHeld = true
    }
//    if (lockHeld) {
//      throw new LockPersistenceException("Lock is held")
//    }

  private def getAcquireLockQuery(lockKey: String, owner: String, onlyIfSameOwner: Boolean): Bson = {
    val expiresAtCond: Bson = lt(LockEntry.ExpiresAtField, new Date)
    val ownerCond: Bson = feq(LockEntry.OwnerField, owner)
    val orCond: Bson = if (onlyIfSameOwner) {
      or(ownerCond)
    } else {
      or(expiresAtCond, ownerCond)
    }
    and(feq(LockEntry.KeyField, lockKey), feq(LockEntry.StatusField, LockStatus.LockHeld.name), orCond)
  }

}
