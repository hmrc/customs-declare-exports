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
import org.mongodb.scala.model.Filters
import play.api.Logger

@ChangeLog(order = "002")
class CacheChangeLog {

  private val logger = Logger(this.getClass)

  @ChangeSet(order = "006", id = "CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode", author = "Maciej Rewera")
  def updateTransportBorderModeOfTransportCode(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode'...")

    getDeclarationsCollection(db).updateMany(
      and(exists("transport.borderModeOfTransportCode"), not(Filters.eq("transport.borderModeOfTransportCode", ""))),
      rename("transport.borderModeOfTransportCode", "temp")
    )
    getDeclarationsCollection(db).updateMany(exists("temp"), rename("temp", "transport.borderModeOfTransportCode.code"))

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode'... Done.")
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = {
    val collectionName = "declarations"
    db.getCollection(collectionName)
  }
}
