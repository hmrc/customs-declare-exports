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

import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.Helpers.idWrites
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParsedNotificationRepository @Inject()(mc: ReactiveMongoComponent)(implicit ec: ExecutionContext)
    extends ReactiveRepository[ParsedNotification, BSONObjectID](
      "notifications",
      mc.mongoConnector.db,
      ParsedNotification.DbFormat.format,
      objectIdFormats
    ) {

  override lazy val collection: JSONCollection =
    mongo().collection[JSONCollection](collectionName, failoverStrategy = RepositorySettings.failoverStrategy)

  override def indexes: Seq[Index] = Seq(
    Index(
      Seq("actionId" -> IndexType.Ascending, "details.dateTimeIssued" -> IndexType.Ascending),
      name = Some("detailsDateTimeIssuedOrderedActionId")
    )
  )

  def findLatestNotification(actionIds: Seq[String]): Future[Option[ParsedNotification]] =
    if (actionIds.isEmpty) Future.successful(None)
    else {
      val query = Json.obj("$or" -> actionIds.map(id => Json.obj("actionId" -> JsString(id))))
      val sort = Json.obj("details.dateTimeIssued" -> -1)
      collection.find(query, None).sort(sort).one[ParsedNotification]
    }

  def findNotificationsByActionId(actionId: String): Future[Seq[ParsedNotification]] =
    find("actionId" -> JsString(actionId))

  def findNotificationsByActionIds(actionIds: Seq[String]): Future[Seq[ParsedNotification]] =
    actionIds match {
      case Seq() => Future.successful(Seq.empty)
      case _     => find("$or" -> actionIds.map(id => Json.obj("actionId" -> JsString(id))))
    }
}
