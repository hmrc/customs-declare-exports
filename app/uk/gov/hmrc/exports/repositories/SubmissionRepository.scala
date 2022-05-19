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

import play.api.libs.json.{JsObject, JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.ReadPreference
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONBoolean, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionQueryParameters}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
    extends ReactiveRepository[Submission, BSONObjectID]("submissions", mc.mongoConnector.db, Submission.formats, objectIdFormats) {

  override lazy val collection: JSONCollection =
    mongo().collection[JSONCollection](collectionName, failoverStrategy = RepositorySettings.failoverStrategy)

  override def indexes: Seq[Index] = Seq(
    Index(
      Seq("actions.id" -> IndexType.Ascending),
      unique = true,
      name = Some("actionIdIdx"),
      partialFilter = Some(BSONDocument(Seq("actions.id" -> BSONDocument("$exists" -> BSONBoolean(true)))))
    ),
    Index(Seq("eori" -> IndexType.Ascending, "action.requestTimestamp" -> IndexType.Descending), name = Some("actionOrderedEori"))
  )

  //looking up by MRN field that is not indexed (eori is though)! This is for the cancellation request processing!
  def findSubmissionByMrnAndEori(mrn: String, eori: String): Future[Option[Submission]] =
    find("eori" -> eori, "mrn" -> mrn).map(_.headOption)

  def findByActionId(actionId: String): Future[Option[Submission]] =
    find("actions.id" -> actionId).map(_.headOption)

  def findBy(eori: String, queryParameters: SubmissionQueryParameters): Future[Seq[Submission]] = {
    val query = Json.toJson(queryParameters).as[JsObject] + (otherField = ("eori", JsString(eori)))
    collection
      .find(query, projection = None)(ImplicitBSONHandlers.JsObjectDocumentWriter, ImplicitBSONHandlers.JsObjectDocumentWriter)
      .sort(Json.obj("actions.requestTimestamp" -> -1))
      .cursor[Submission](ReadPreference.primaryPreferred)
      .collect(maxDocs = -1, FailOnError[Seq[Submission]]())
  }

  def save(submission: Submission): Future[Submission] =
    insert(submission).map { res =>
      if (!res.ok) logger.error(s"Errors when persisting declaration submission: ${res.writeErrors.mkString("--")}")
      submission
    }

  def addAction(mrn: String, newAction: Action): Future[Option[Submission]] = {
    val query = Json.obj("mrn" -> mrn)
    val update = Json.obj("$addToSet" -> Json.obj("actions" -> newAction))
    performUpdate(query, update)
  }

  def addAction(submission: Submission, action: Action): Future[Submission] = {
    val query = Json.obj("uuid" -> submission.uuid)
    val update = Json.obj("$addToSet" -> Json.obj("actions" -> action))
    performUpdate(query, update).map(_.getOrElse(throw new IllegalStateException("Submission must exist before")))
  }

  def updateAfterNotificationParsing(submission: Submission): Future[Option[Submission]] = {
    val query = Json.obj("uuid" -> submission.uuid)
    val update = Json.obj("$set" -> Json.obj(
      "mrn" -> submission.mrn,
      "latestEnhancedStatus" -> submission.latestEnhancedStatus,
      "enhancedStatusLastUpdated" -> submission.enhancedStatusLastUpdated,
      "actions" -> submission.actions)
    )
    performUpdate(query, update)
  }

  private def performUpdate(query: JsObject, update: JsObject): Future[Option[Submission]] =
    findAndUpdate(query, update, fetchNewObject = true).map { updateResult =>
      if (updateResult.value.isEmpty) {
        updateResult.lastError.foreach(_.err.foreach(errorMsg => logger.error(s"Problem during database update: $errorMsg")))
      }
      updateResult.result[Submission]
    }
}
