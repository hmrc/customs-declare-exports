/*
 * Copyright 2024 HM Revenue & Customs
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

import org.bson.conversions.Bson
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.{JsBoolean, JsObject, Json}
import uk.gov.hmrc.exports.models.FetchSubmissionPageData
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.fromStatusGroup
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup.StatusGroup
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission}
import uk.gov.hmrc.exports.repositories.SubmissionRepository.dashBoardIndex
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

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
  override val executionContext: ExecutionContext = ec

  override val lastUpdatedField: Option[String] = Some("lastUpdated")

  def addAction(uuid: String, action: Action): Future[Option[Submission]] = {
    val filter = Json.obj("uuid" -> uuid)
    val update = Json.obj("$addToSet" -> Json.obj("actions" -> Json.toJson(action)))

    findOneAndUpdate(filter, update)
  }

  def countSubmissionsInGroup(eori: String, statusGroup: StatusGroup): Future[Int] =
    collection
      .countDocuments(fetchFilter(eori, statusGroup))
      .toFuture()
      .map(_.toInt)

  private lazy val ascending = BsonDocument(Json.obj("enhancedStatusLastUpdated" -> 1, "uuid" -> 1).toString)
  private lazy val descending = BsonDocument(Json.obj("enhancedStatusLastUpdated" -> -1, "uuid" -> -1).toString)

  def fetchFirstPage(eori: String, statusGroup: StatusGroup, fetchData: FetchSubmissionPageData): Future[Seq[Submission]] =
    collection
      .find(fetchFilter(eori, statusGroup))
      .hintString(dashBoardIndex)
      .limit(fetchData.limit)
      .sort(if (fetchData.reverse) ascending else descending)
      .toFuture()

  def fetchLastPage(eori: String, statusGroup: StatusGroup, fetchData: FetchSubmissionPageData): Future[Seq[Submission]] =
    collection
      .find(fetchFilter(eori, statusGroup))
      .hintString(dashBoardIndex)
      .limit(fetchData.limit)
      .sort(if (fetchData.reverse) descending else ascending)
      .toFuture()
      .map(_.reverse)

  def fetchLoosePage(eori: String, statusGroup: StatusGroup, page: Int, fetchData: FetchSubmissionPageData): Future[Seq[Submission]] =
    collection
      .find(fetchFilter(eori, statusGroup))
      .hintString(dashBoardIndex)
      .limit(fetchData.limit)
      .skip((page - 1).max(0) * fetchData.limit)
      .sort(if (fetchData.reverse) ascending else descending)
      .toFuture()

  def fetchNextPage(
    eori: String,
    statusGroup: StatusGroup,
    statusLastUpdated: ZonedDateTime,
    fetchData: FetchSubmissionPageData
  ): Future[Seq[Submission]] =
    if (fetchData.reverse) nextPageAscending(eori, statusGroup, statusLastUpdated, fetchData.limit, fetchData.uuid)
    else previousPageDescending(eori, statusGroup, statusLastUpdated, fetchData.limit, fetchData.uuid)

  def fetchPreviousPage(
    eori: String,
    statusGroup: StatusGroup,
    statusLastUpdated: ZonedDateTime,
    fetchData: FetchSubmissionPageData
  ): Future[Seq[Submission]] =
    if (fetchData.reverse) previousPageDescending(eori, statusGroup, statusLastUpdated, fetchData.limit, fetchData.uuid)
    else nextPageAscending(eori, statusGroup, statusLastUpdated, fetchData.limit, fetchData.uuid)

  private def nextPageAscending(
    eori: String,
    statusGroup: StatusGroup,
    statusLastUpdated: ZonedDateTime,
    limit: Int,
    uuid: String
  ): Future[Seq[Submission]] =
    collection
      .find(fetchFilter(eori, statusGroup, statusLastUpdated, "gt", uuid))
      .hintString(dashBoardIndex)
      .limit(limit)
      .sort(ascending)
      .toFuture()

  private def previousPageDescending(
    eori: String,
    statusGroup: StatusGroup,
    statusLastUpdated: ZonedDateTime,
    limit: Int,
    uuid: String
  ): Future[Seq[Submission]] =
    collection
      .find(fetchFilter(eori, statusGroup, statusLastUpdated, "lt", uuid))
      .hintString(dashBoardIndex)
      .limit(limit)
      .sort(descending)
      .toFuture()

  private def fetchFilter(eori: String, statusGroup: StatusGroup): Bson = {
    val filter =
      s"""
         |{
         |  "eori": "$eori",
         |  "latestEnhancedStatus": { "$$in": [ ${fromStatusGroup(statusGroup).map(s => s""""$s"""").mkString(",")} ] }
         |}""".stripMargin
    BsonDocument(filter)
  }

  private def fetchFilter(eori: String, statusGroup: StatusGroup, statusLastUpdated: ZonedDateTime, comp: String, uuid: String): Bson = {
    val filter =
      s"""
         |{
         |  "eori": "$eori",
         |  "latestEnhancedStatus": { "$$in": [ ${fromStatusGroup(statusGroup).map(s => s""""$s"""").mkString(",")} ] },
         |  "$$or": [
         |    { "enhancedStatusLastUpdated": { "$$$comp": ${Json.toJson(statusLastUpdated)} } },
         |    { "enhancedStatusLastUpdated": ${Json.toJson(statusLastUpdated)}, "uuid": {"$$$comp": "$uuid" } }
         |  ]
         |}""".stripMargin
    BsonDocument(filter)
  }

  def findAction(eori: String, actionId: String): Future[Option[Action]] = {
    val filter = and(equal("actions.id", actionId), equal("eori", eori))
    findOne(filter).map(_.flatMap(_.actions.find(_.id == actionId)))
  }

  def findByAction(eori: String, actionId: String): Future[Option[Submission]] = {
    val filter = and(equal("actions.id", actionId), equal("eori", eori))
    findOne(filter)
  }

  def findById(eori: String, id: String): Future[Option[Submission]] =
    collection
      .find(and(equal("eori", eori), equal("uuid", id)))
      .toFuture()
      .map(_.headOption)

  def findByLrn(eori: String, lrn: String): Future[Seq[Submission]] =
    collection
      .find(and(equal("eori", eori), equal("lrn", lrn)))
      .toFuture()

  def findByLatestDecId(eori: String, latestDecId: String): Future[Option[Submission]] =
    collection
      .find(and(equal("eori", eori), equal("latestDecId", latestDecId)))
      .toFuture()
      .map(_.headOption)
}

object SubmissionRepository {

  import com.mongodb.client.model.Indexes.{ascending, compoundIndex, descending}

  val filter: JsObject = Json.obj("actions.id" -> Json.obj("$exists" -> JsBoolean(true)))

  val dashBoardIndex = "dashboardIdx"

  val indexes: Seq[IndexModel] = List(
    IndexModel(
      ascending("actions.id"),
      IndexOptions()
        .name("actionIdIdx")
        .partialFilterExpression(BsonDocument(filter.toString))
        .unique(true)
    ),
    IndexModel(ascending("uuid"), IndexOptions().name("uuidIdx").unique(true)),
    IndexModel(compoundIndex(ascending("eori"), descending("lrn")), IndexOptions().name("lrnByEori")),
    IndexModel(
      compoundIndex(ascending("eori"), ascending("latestEnhancedStatus"), descending("enhancedStatusLastUpdated")),
      IndexOptions().name(dashBoardIndex)
    )
  )
}
