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
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.ReadPreference
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONBoolean, BSONDocument, BSONObjectID}
import reactivemongo.play.json.commands.JSONFindAndModifyCommand.FindAndModifyResult
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
    extends ReactiveRepository[Submission, BSONObjectID](
      "submissions",
      mc.mongoConnector.db,
      Submission.formats,
      objectIdFormats
    ) {

  override def indexes: Seq[Index] = Seq(
    Index(
      Seq("actions.id" -> IndexType.Ascending),
      unique = true,
      name = Some("actionIdIdx"),
      partialFilter = Some(BSONDocument(Seq("actions.id" -> BSONDocument("$exists" -> BSONBoolean(true)))))
    ),
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(
      Seq("eori" -> IndexType.Ascending, "action.requestTimestamp" -> IndexType.Descending),
      name = Some("actionOrderedEori")
    ),
    Index(Seq("updatedDateTime" -> IndexType.Ascending), name = Some("updateTimeIdx"))
  )

  def findAllSubmissionsForEori(eori: String): Future[Seq[Submission]] = {
    import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter

    collection
      .find(Json.obj("eori" -> eori), None)
      .sort(Json.obj("actions.requestTimestamp" -> -1))
      .cursor[Submission](ReadPreference.primaryPreferred)
      .collect(maxDocs = -1, FailOnError[Seq[Submission]]())
  }

  def findOrCreate(eori: Eori, id: String, onMissing: Submission): Future[Submission] =
    findSubmissionByUuid(eori.value, id).flatMap {
      case Some(submission) => Future.successful(submission)
      case None             => save(onMissing)
    }

  def findSubmissionByMrn(mrn: String): Future[Option[Submission]] = find("mrn" -> mrn).map(_.headOption)

  def findSubmissionByUuid(eori: String, uuid: String): Future[Option[Submission]] =
    find("eori" -> eori, "uuid" -> uuid).map(_.headOption)

  def save(submission: Submission): Future[Submission] = insert(submission).map { res =>
    if (!res.ok) logger.error(s"Errors when persisting declaration submission: ${res.writeErrors.mkString("--")}")
    submission
  }

  def updateMrn(conversationId: String, newMrn: String): Future[Option[Submission]] = {
    val query = Json.obj("actions.id" -> conversationId)
    val update = Json.obj("$set" -> Json.obj("mrn" -> newMrn))
    performUpdate(query, update)
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

  private def performUpdate(query: JsObject, update: JsObject): Future[Option[Submission]] =
    findAndUpdate(query, update, fetchNewObject = true).map { updateResult =>
      if (updateResult.value.isEmpty) logDatabaseUpdateError(updateResult)
      updateResult.result[Submission]
    }

  private def logDatabaseUpdateError(res: FindAndModifyResult): Unit =
    res.lastError.foreach(_.err.foreach(errorMsg => logger.error(s"Problem during database update: $errorMsg")))

}
