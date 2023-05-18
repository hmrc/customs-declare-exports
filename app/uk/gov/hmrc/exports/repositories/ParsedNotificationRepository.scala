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

import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.{JsString, Json}
import repositories.RepositoryOps
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class ParsedNotificationRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ParsedNotification](
      mongoComponent = mongoComponent,
      collectionName = "notifications",
      domainFormat = ParsedNotification.format,
      indexes = ParsedNotificationRepository.indexes
    ) with RepositoryOps[ParsedNotification] {

  override def classTag: ClassTag[ParsedNotification] = implicitly[ClassTag[ParsedNotification]]
  override val executionContext = ec

  def findNotifications(actionIds: Seq[String]): Future[Seq[ParsedNotification]] =
    if (actionIds.isEmpty) Future.successful(List.empty)
    else findAll(Json.obj("$or" -> actionIds.map(id => Json.obj("actionId" -> JsString(id)))))

  def findLatestNotification(actionId: String): Future[Option[ParsedNotification]] =
    findFirst(Json.obj("actionId" -> actionId), Json.obj("details.dateTimeIssued" -> -1))
}

object ParsedNotificationRepository {

  val indexes: Seq[IndexModel] = List(
    IndexModel(ascending("actionId", "details.dateTimeIssued"), IndexOptions().name("detailsDateTimeIssuedOrderedActionId"))
  )
}
