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
import org.bson.conversions.Bson
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.{and, equal, gt, lt}
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.{JsBoolean, Json}
import repositories.RepositoryOps
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatusGroup.EnhancedStatusGroup
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission}
import uk.gov.hmrc.exports.repositories.SubmissionRepository.dashBoardIndex
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.ZonedDateTime
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

  def countSubmissionsInGroup(eori: String, maybeStatusGroup: Option[EnhancedStatusGroup], limit: Int): Future[Int] =
    maybeStatusGroup.fold(Future.successful(0)) { statusGroup =>
      collection
        .countDocuments(and(equal("eori", eori), equal("enhancedStatusGroup", statusGroup.toString)))
        .toFuture()
        //.map(n => Math.ceil(n.toDouble / limit).toInt)
        .map(_.toInt)
    }

  def fetchFirstPage(eori: String, statusGroup: EnhancedStatusGroup, limit: Int): Future[Seq[Submission]] =
    fetchPage(limit, and(equal("eori", eori), equal("enhancedStatusGroup", statusGroup.toString)))

  private val sort = BsonDocument(Json.obj("enhancedStatusLastUpdated" -> 1).toString)

  def fetchLastPage(eori: String, statusGroup: EnhancedStatusGroup, limit: Int): Future[Seq[Submission]] =
    collection
      .find(and(equal("eori", eori), equal("enhancedStatusGroup", statusGroup.toString)))
      .sort(sort)
      .limit(limit)
      .toFuture()
      .map(_.reverse)

  def fetchLosePage(eori: String, statusGroup: EnhancedStatusGroup, page: Int, limit: Int): Future[Seq[Submission]] =
    collection
      .find(and(equal("eori", eori), equal("enhancedStatusGroup", statusGroup.toString)))
      .hintString(dashBoardIndex)
      .skip((page - 1).max(0) * limit)
      .limit(limit)
      .toFuture()

  def fetchNextPage(eori: String, statusGroup: EnhancedStatusGroup, statusLastUpdated: ZonedDateTime, limit: Int): Future[Seq[Submission]] =
    fetchPage(limit, and(
      and(equal("eori", eori), equal("enhancedStatusGroup", statusGroup.toString)),
      lt("enhancedStatusLastUpdated", Codecs.toBson(statusLastUpdated))
    ))

  def fetchPreviousPage(eori: String, statusGroup: EnhancedStatusGroup, statusLastUpdated: ZonedDateTime, limit: Int): Future[Seq[Submission]] =
    fetchPage(limit, and(
      and(equal("eori", eori), equal("enhancedStatusGroup", statusGroup.toString)),
      gt("enhancedStatusLastUpdated", Codecs.toBson(statusLastUpdated))
    ))

  def findAll(eori: String): Future[Seq[Submission]] =
    collection
      .find(equal("eori", eori))
      .sort(BsonDocument(Json.obj("actions.requestTimestamp" -> -1).toString))
      .toFuture()

  def findById(eori: String, id: String): Future[Option[Submission]] =
    collection
      .find(and(equal("uuid", id), equal("eori", eori)))
      .toFuture()
      .map(_.headOption)

  def findByLrn(eori: String, lrn: String): Future[Seq[Submission]] =
    collection
      .find(and(equal("eori", eori), equal("lrn", lrn)))
      .toFuture()


  private def fetchPage(limit: Int, filter: Bson): Future[Seq[Submission]] =
    collection
      .find(filter)
      .hintString(dashBoardIndex)
      .limit(limit)
      .toFuture()
}

object SubmissionRepository {

  val filter = Json.obj("actions.id" -> Json.obj("$exists" -> JsBoolean(true)))

  val dashBoardIndex = "dashboardIdx"

  val indexes: Seq[IndexModel] = List(
    IndexModel(
      ascending("actions.id"),
      IndexOptions()
        .name("actionIdIdx")
        .partialFilterExpression(BsonDocument(filter.toString))
        .unique(true)
    ),
    IndexModel(compoundIndex(ascending("eori"), descending("action.requestTimestamp")), IndexOptions().name("actionOrderedEori")),
    IndexModel(compoundIndex(ascending("eori"), descending("lrn")), IndexOptions().name("lrnByEori")),
    IndexModel(compoundIndex(
      ascending("eori"), ascending("enhancedStatusGroup"), descending("enhancedStatusLastUpdated")),
      IndexOptions().name(dashBoardIndex)
    )
  )
}
