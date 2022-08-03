/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.scheduler.jobs

import com.mongodb.client.{MongoClient, MongoCollection}
import org.bson.Document
import uk.gov.hmrc.exports.mongo.ExportsClient

trait PurgeAncientSubmissionsCollections extends ExportsClient {

  val submissionRepositoryCollectionName: String
  val declarationRepositoryCollectionName: String
  val parsedNotificationRepositoryCollectionName: String
  val unparsedNotificationRepositoryCollectionName: String

  lazy val submissionCollection: MongoCollection[Document] = db.getCollection(submissionRepositoryCollectionName)
  lazy val declarationCollection: MongoCollection[Document] = db.getCollection(declarationRepositoryCollectionName)
  lazy val notificationCollection: MongoCollection[Document] = db.getCollection(parsedNotificationRepositoryCollectionName)
  lazy val unparsedNotificationCollection: MongoCollection[Document] = db.getCollection(unparsedNotificationRepositoryCollectionName)

}
