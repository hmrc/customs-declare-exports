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

import com.mongodb.client.model.Indexes.{ascending, compoundIndex, descending}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import repositories.RepositoryOps
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionQueryParameters}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class SubmissionRepository @Inject() (val mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Submission](
      mongoComponent = mongoComponent,
      collectionName = "submissions",
      domainFormat = Submission.format,
      indexes = SubmissionRepository.indexes
    ) with RepositoryOps[Submission] {

  override def classTag: ClassTag[Submission] = implicitly[ClassTag[Submission]]
  override val executionContext = ec

  def addAction(mrn: String, newAction: Action): Future[Option[Submission]] = {
    val filter = Json.obj("mrn" -> mrn)
    val update = Json.obj("$addToSet" -> Json.obj("actions" -> newAction))
    findOneAndUpdate(filter, update)
  }

  def findAll(eori: String, queryParameters: SubmissionQueryParameters): Future[Seq[Submission]] = {
    val filter = Json.toJson(queryParameters).as[JsObject] + ("eori" -> JsString(eori))
    collection
      .find(BsonDocument(filter.toString))
      .sort(BsonDocument(Json.obj("actions.requestTimestamp" -> -1).toString))
      .toFuture()
  }
}

object SubmissionRepository {

  val filter = Json.obj("actions.id" -> Json.obj("$exists" -> JsBoolean(true)))

  val indexes: Seq[IndexModel] = List(
    IndexModel(
      ascending("actions.id"),
      IndexOptions()
        .name("actionIdIdx")
        .partialFilterExpression(BsonDocument(filter.toString))
        .unique(true)
    ),
    IndexModel(compoundIndex(ascending("eori"), descending("action.requestTimestamp")), IndexOptions().name("actionOrderedEori"))
  )
}
