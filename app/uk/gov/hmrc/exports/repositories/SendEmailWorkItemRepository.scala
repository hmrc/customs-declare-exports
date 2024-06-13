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

package uk.gov.hmrc.exports.repositories

import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Configuration
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.util.TimeUtils.instant
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}

import java.time.{Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class SendEmailWorkItemRepository @Inject() (config: Configuration, mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends WorkItemRepository[SendEmailDetails](
      collectionName = "sendEmailWorkItems",
      mongoComponent = mongoComponent,
      itemFormat = SendEmailDetails.format,
      workItemFields = WorkItemFields.default
    ) with RepositoryOps[WorkItem[SendEmailDetails]] {

  override def classTag: ClassTag[WorkItem[SendEmailDetails]] = implicitly[ClassTag[WorkItem[SendEmailDetails]]]
  override val executionContext = ec

  override def ensureIndexes(): Future[Seq[String]] = {
    val workItemIndexes: Seq[IndexModel] =
      indexes ++ List(IndexModel(ascending("item.notificationId"), IndexOptions().name("sendEmailDetailsNotificationIdIdx").unique(true)))
    MongoUtils.ensureIndexes(collection, workItemIndexes, replaceIndexes = true)
  }

  override lazy val inProgressRetryAfter: Duration =
    Duration.ofMillis(config.getMillis("workItem.sendEmail.retryAfterMillis"))

  override def now(): Instant = instant()
}
