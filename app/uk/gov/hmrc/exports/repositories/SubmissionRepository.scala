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
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.commands.JSONFindAndModifyCommand.FindAndModifyResult
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
    Index(Seq("actions.conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdIdx")),
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx"))
  )

  def findAllSubmissionsForEori(eori: String): Future[Seq[Submission]] = find("eori" -> eori)

  def findSubmissionByMrn(mrn: String): Future[Option[Submission]] = find("mrn" -> mrn).map(_.headOption)

  def findSubmissionByConversationId(conversationId: String): Future[Option[Submission]] =
    find("actions.conversationId" -> conversationId).map(_.headOption)

  def findSubmissionByUuid(uuid: String): Future[Option[Submission]] = find("uuid" -> uuid).map(_.headOption)

  // TODO: Need to change this method to return Future[WriteResult].
  //  In current implementation it will never return false, because in case of an error,
  //  insert throws an Exception which will be propagated to the caller.
  def save(submission: Submission): Future[Boolean] = insert(submission).map { res =>
    if (!res.ok) logger.error(s"Errors when persisting declaration submission: ${res.writeErrors.mkString("--")}")
    res.ok
  }

  def updateMrn(conversationId: String, newMrn: String): Future[Option[Submission]] = {
    val query = Json.obj("actions.conversationId" -> conversationId)
    val update = Json.obj("$set" -> Json.obj("mrn" -> newMrn))
    performUpdate(query, update)
  }

  def addAction(mrn: String, newAction: Action): Future[Option[Submission]] = {
    val query = Json.obj("mrn" -> mrn)
    val update = Json.obj("$addToSet" -> Json.obj("actions" -> newAction))
    performUpdate(query, update)
  }

  private def performUpdate(query: JsObject, update: JsObject): Future[Option[Submission]] =
    findAndUpdate(query, update, fetchNewObject = true).map { updateResult =>
      if (updateResult.value.isEmpty) logDatabaseUpdateError(updateResult)
      updateResult.result[Submission]
    }

  private def logDatabaseUpdateError(res: FindAndModifyResult): Unit =
    res.lastError.foreach(_.err.foreach(errorMsg => logger.error(s"Problem during database update: $errorMsg")))

}
