/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationRepository @Inject()(mc: ReactiveMongoComponent)(implicit ec: ExecutionContext)
    extends ReactiveRepository[Notification, BSONObjectID]("notifications", mc.mongoConnector.db, Notification.format, objectIdFormats) {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("dateTimeIssued" -> IndexType.Ascending), name = Some("dateTimeIssuedIdx")),
    Index(Seq("mrn" -> IndexType.Ascending), name = Some("mrnIdx")),
    Index(Seq("actionId" -> IndexType.Ascending), name = Some("actionIdIdx"))
  )

  // TODO: Need to change this method to return Future[WriteResult].
  //  In current implementation it will never return false, because in case of an error,
  //  insert throws an Exception which will be propagated.
  def save(notification: Notification): Future[Boolean] = insert(notification).map { res =>
    if (!res.ok) logger.error(s"Errors when persisting export notification: ${res.writeErrors.mkString("--")}")
    res.ok
  }

  def findNotificationsByActionId(actionId: String): Future[Seq[Notification]] =
    find("actionId" -> JsString(actionId))

  def findNotificationsByActionIds(actionIds: Seq[String]): Future[Seq[Notification]] =
    actionIds match {
      case Seq() => Future.successful(Seq.empty)
      case _     => find("$or" -> actionIds.map(id => Json.obj("actionId" -> JsString(id))))
    }

}
