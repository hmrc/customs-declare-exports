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

package uk.gov.hmrc.exports.repositories

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository.WorkItemFormat
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.workitem.{WorkItem, WorkItemFieldNames, WorkItemRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SendEmailWorkItemRepository @Inject()(configuration: Configuration, reactiveMongoComponent: ReactiveMongoComponent)
    extends WorkItemRepository[SendEmailDetails, BSONObjectID](
      collectionName = "sendEmailWorkItems",
      mongo = reactiveMongoComponent.mongoConnector.db,
      itemFormat = WorkItemFormat.workItemMongoFormat[SendEmailDetails],
      config = configuration.underlying
    ) {

  override lazy val collection: JSONCollection =
    mongo().collection[JSONCollection](collectionName, failoverStrategy = RepositorySettings.failoverStrategy)

  override def indexes: Seq[Index] = super.indexes ++ Seq(
    Index(key = Seq("sendEmailDetails.notificationId" -> IndexType.Ascending), name = Some("sendEmailDetailsNotificationIdIdx"), unique = true)
  )

  override def now: DateTime = DateTime.now

  override def workItemFields: WorkItemFieldNames = new WorkItemFieldNames {
    val receivedAt = "receivedAt"
    val updatedAt = "updatedAt"
    val availableAt = "availableAt"
    val status = "status"
    val id = "_id"
    val failureCount = "failureCount"
  }

  override def inProgressRetryAfterProperty: String = "workItem.sendEmail.retryAfterMillis"

  def pushNew(item: SendEmailDetails)(implicit ec: ExecutionContext): Future[WorkItem[SendEmailDetails]] = pushNew(item, now)
}

object SendEmailWorkItemRepository {

  object WorkItemFormat {
    import play.api.libs.functional.syntax._
    import reactivemongo.play.json.ImplicitBSONHandlers._

    def workItemMongoFormat[T](implicit nFormat: Format[T]): Format[WorkItem[T]] =
      ReactiveMongoFormats.mongoEntity(notificationFormat(ReactiveMongoFormats.objectIdFormats, ReactiveMongoFormats.dateTimeFormats, nFormat))

    private def notificationFormat[T](
      implicit bsonIdFormat: Format[BSONObjectID],
      dateTimeFormat: Format[DateTime],
      nFormat: Format[T]
    ): Format[WorkItem[T]] = {
      val reads = (
        (__ \ "id").read[BSONObjectID] and
          (__ \ "receivedAt").read[DateTime] and
          (__ \ "updatedAt").read[DateTime] and
          (__ \ "availableAt").read[DateTime] and
          (__ \ "status").read[uk.gov.hmrc.workitem.ProcessingStatus] and
          (__ \ "failureCount").read[Int].orElse(Reads.pure(0)) and
          (__ \ "sendEmailDetails").read[T]
      )(WorkItem.apply[T] _)

      val writes = (
        (__ \ "id").write[BSONObjectID] and
          (__ \ "receivedAt").write[DateTime] and
          (__ \ "updatedAt").write[DateTime] and
          (__ \ "availableAt").write[DateTime] and
          (__ \ "status").write[uk.gov.hmrc.workitem.ProcessingStatus] and
          (__ \ "failureCount").write[Int] and
          (__ \ "sendEmailDetails").write[T]
      )(unlift(WorkItem.unapply[T]))

      Format(reads, writes)
    }
  }

}
