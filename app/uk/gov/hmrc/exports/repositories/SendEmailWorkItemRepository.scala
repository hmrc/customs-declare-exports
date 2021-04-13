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
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.workitem.{WorkItem, WorkItemFieldNames, WorkItemRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SendEmailWorkItemRepository @Inject()(configuration: Configuration, reactiveMongoComponent: ReactiveMongoComponent)
    extends WorkItemRepository[SendEmailDetails, BSONObjectID](
      collectionName = "sendEmailWorkItems",
      mongo = reactiveMongoComponent.mongoConnector.db,
      itemFormat = SendEmailWorkItemRepository.workItemFormat,
      config = configuration.underlying
    ) {

  override lazy val collection: JSONCollection =
    mongo().collection[JSONCollection](collectionName, failoverStrategy = RepositorySettings.failoverStrategy)

  override def indexes: Seq[Index] = super.indexes ++ Seq(
    Index(key = Seq("sendEmailDetails.notificationId" -> IndexType.Ascending), name = Some("sendEmailDetailsNotificationIdIdx"), unique = true)
  )

  override def now: DateTime = DateTime.now

  override def workItemFields: WorkItemFieldNames = WorkItemFormat.defaultWorkItemFields

  override def inProgressRetryAfterProperty: String = "workItem.sendEmail.retryAfterMillis"

  def pushNew(item: SendEmailDetails)(implicit ec: ExecutionContext): Future[WorkItem[SendEmailDetails]] = pushNew(item, now)

  def markAlertTriggered(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[WorkItem[SendEmailDetails]]] = {
    val fields = Json.obj("sendEmailDetails.alertTriggered" -> true)

    findAndUpdate(
      query = Json.obj(workItemFields.id -> ReactiveMongoFormats.objectIdWrite.writes(id)),
      update = Json.obj("$set" -> fields),
      fetchNewObject = true
    ).map { updateResult =>
      updateResult.lastError.foreach(_.err.foreach(errorMsg => logger.warn(s"Problem during $collectionName collection update: $errorMsg")))

      updateResult.result[WorkItem[SendEmailDetails]]
    }
  }
}

object SendEmailWorkItemRepository {
  private val workItemFormat = WorkItemFormat.workItemMongoFormat[SendEmailDetails]("sendEmailDetails")
}
