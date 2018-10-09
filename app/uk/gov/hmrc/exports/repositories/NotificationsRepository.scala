/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.JsString
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.models.ExportsNotification
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.mongo.{ReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationsRepository @Inject()(mc: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository[ExportsNotification, BSONObjectID]("exportNotifications", mc.mongoConnector.db,
    ExportsNotification.exportsNotificationFormats, objectIdFormats) {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(Seq("conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdIdx"))
  )

  def findByEori(eori: String): Future[Seq[ExportsNotification]] = find("eori" -> JsString(eori))

  def getByConversationId(conversationId: String): Future[Option[ExportsNotification]] =
    find("conversationId" -> JsString(conversationId)).map(_.headOption)

  def getByEoriAndConversationId(eori: String, conversationId: String): Future[Option[ExportsNotification]] =
    find("eori" -> JsString(eori), "conversationId" -> JsString(conversationId)).map(_.headOption)

  def save(exportsNotification: ExportsNotification): Future[Boolean] = insert(exportsNotification).map(res =>
    if (res.ok) res.ok
    else {
      Logger.error("errors in inserting Notification result" + res.writeErrors.mkString("--"))
      res.ok
    })
}


