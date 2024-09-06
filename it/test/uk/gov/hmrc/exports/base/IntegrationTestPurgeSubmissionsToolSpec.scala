/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.base

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.DeleteResult
import uk.gov.hmrc.exports.repositories._

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestSpec {

  val declarationRepository = instanceOf[DeclarationRepository]
  val jobRunRepository = instanceOf[JobRunRepository]
  val notificationRepository = instanceOf[ParsedNotificationRepository]
  val submissionRepository = instanceOf[SubmissionRepository]
  val unparsedNotificationRepository = instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    removeAll(declarationRepository.collection)
    removeAll(jobRunRepository.collection)
    removeAll(notificationRepository.collection)
    removeAll(submissionRepository.collection)
    removeAll(unparsedNotificationRepository.collection)
    super.beforeEach()
  }

  def removeAll(collection: MongoCollection[_]): DeleteResult =
    collection.deleteMany(BsonDocument()).toFuture().futureValue
}
