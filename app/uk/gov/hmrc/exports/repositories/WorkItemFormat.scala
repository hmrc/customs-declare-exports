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

package uk.gov.hmrc.exports.repositories

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.workitem.{WorkItem, WorkItemFieldNames}

object WorkItemFormat {
  import play.api.libs.functional.syntax._
  import reactivemongo.play.json.ImplicitBSONHandlers._

  def workItemMongoFormat[T](itemFieldName: String = "item")(implicit nFormat: Format[T]): Format[WorkItem[T]] =
    ReactiveMongoFormats.mongoEntity(notificationFormat(itemFieldName)(ReactiveMongoFormats.dateTimeFormats, nFormat))

  private def notificationFormat[T](itemFieldName: String)(implicit dateTimeFormat: Format[DateTime], nFormat: Format[T]): Format[WorkItem[T]] = {
    val reads = (
      (__ \ "id").read[BSONObjectID] and
        (__ \ "receivedAt").read[DateTime] and
        (__ \ "updatedAt").read[DateTime] and
        (__ \ "availableAt").read[DateTime] and
        (__ \ "status").read[uk.gov.hmrc.workitem.ProcessingStatus] and
        (__ \ "failureCount").read[Int].orElse(Reads.pure(0)) and
        (__ \ itemFieldName).read[T]
    )(WorkItem.apply[T] _)

    val writes = (
      (__ \ "id").write[BSONObjectID] and
        (__ \ "receivedAt").write[DateTime] and
        (__ \ "updatedAt").write[DateTime] and
        (__ \ "availableAt").write[DateTime] and
        (__ \ "status").write[uk.gov.hmrc.workitem.ProcessingStatus] and
        (__ \ "failureCount").write[Int] and
        (__ \ itemFieldName).write[T]
    )(unlift(WorkItem.unapply[T]))

    Format(reads, writes)
  }

  val defaultWorkItemFields: WorkItemFieldNames = new WorkItemFieldNames {
    val receivedAt = "receivedAt"
    val updatedAt = "updatedAt"
    val availableAt = "availableAt"
    val status = "status"
    val id = "_id"
    val failureCount = "failureCount"
  }
}
