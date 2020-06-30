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

import com.mongodb.client.MongoDatabase
import org.bson.Document
import uk.gov.hmrc.exports.migrations.repositories.ChangeEntry.{KeyAuthor, KeyChangeId}

class ChangeEntryRepository(collectionName: String, mongoDatabase: MongoDatabase)
    extends MongoRepository(mongoDatabase, collectionName, Array(KeyAuthor, KeyChangeId)) {

  private[migrations] def isNewChange(changeEntry: ChangeEntry) = {
    val entry = collection.find(buildSearchQueryDBObject(changeEntry)).first
    entry == null
  }

  private[migrations] def save(changeEntry: ChangeEntry): Unit =
    collection.insertOne(changeEntry.buildFullDBObject)

  private def buildSearchQueryDBObject(entry: ChangeEntry) = new Document().append(KeyChangeId, entry.changeId).append(KeyAuthor, entry.author)

}
