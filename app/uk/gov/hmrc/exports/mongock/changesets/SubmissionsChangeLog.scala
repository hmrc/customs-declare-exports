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

package uk.gov.hmrc.exports.mongock.changesets

import com.github.cloudyrock.mongock.{ChangeLog, ChangeSet}
import com.mongodb.client.model.Filters._
import com.mongodb.client.model.Updates.rename
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import play.api.Logger

@ChangeLog
class SubmissionsChangeLog {

  private val logger = Logger(this.getClass)

  @ChangeSet(order = "001", id = "BAU Baseline/Delete all existing indexes on the DB", author = "Paulo Monteiro")
  def dbIndexesBaseline(db: MongoDatabase): Unit = {

    logger.info("Applying 'BAU Baseline/Delete all existing indexes on the DB' db migration...")
    collection(db).dropIndexes();
    logger.info("Applying 'BAU Baseline/Delete all existing indexes on the DB' db migration... Done.")
  }

  private def collection(db: MongoDatabase): MongoCollection[Document] = {
    val collectionName = "submissions"
    db.getCollection(collectionName)
  }
}
