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
import uk.gov.hmrc.exports.models._
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
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(Seq("conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdIdx")),
    Index(Seq("mrn" -> IndexType.Ascending), unique = true, name = Some("mrnIdx"))
  )

  def findByEori(eori: String): Future[Seq[Submission]] =
    find("eori" -> JsString(eori))

  def getByConversationId(conversationId: String): Future[Option[Submission]] =
    find("conversationId" -> JsString(conversationId)).map(_.headOption)

  def getByEoriAndMrn(eori: String, mrn: String): Future[Option[Submission]] =
    find("eori" -> JsString(eori), "mrn" -> JsString(mrn)).map(_.headOption)

  def save(submission: Submission): Future[Boolean] = insert(submission).map(wr => wr.ok)

  // TODO: Should return updated object
  def updateSubmission(submission: Submission): Future[Boolean] = {
    val finder = Json.obj(
      "_id" -> Json.toJsFieldJsValueWrapper(submission.id)(objectIdFormats),
      "conversationId" -> submission.conversationId
    )
    val modifier = Json.obj("$set" -> Json.obj("mrn" -> submission.mrn, "status" -> submission.status))

    findAndUpdate(finder, modifier, fetchNewObject = true).map {
      case result if result.value.isEmpty =>
        result.lastError.foreach(_.err.foreach(logDatabaseResult))
        false
      case _ =>
        true
    }
  }

  // TODO: Get rid of Option[_]
  def updateMrnAndStatus(eori: String, convId: String, newMrn: String, newStatus: Option[String]): Future[Boolean] =
    if (newStatus.isDefined) {
      val finder = Json.obj("eori" -> eori, "conversationId" -> convId)
      val modifier = Json.obj("$set" -> Json.obj("mrn" -> newMrn, "status" -> newStatus.get))

      findAndUpdate(finder, modifier, fetchNewObject = true).map {
        case result if result.value.isEmpty =>
          result.lastError.foreach(_.err.foreach(logDatabaseResult))
          false
        case _ =>
          true
      }
    } else Future.successful(false)

  def cancelDeclaration(eori: String, mrn: String): Future[CancellationStatus] = {
    val finder = Json.obj("eori" -> eori, "mrn" -> mrn)
    val modifier =
      Json.obj("$set" -> Json.obj("status" -> RequestedCancellation.toString, "isCancellationRequested" -> true))

    find("eori" -> JsString(eori), "mrn" -> JsString(mrn)).map(_.headOption).flatMap {
      case Some(submission) if submission.isCancellationRequested =>
        Future.successful(CancellationRequestExists)
      case Some(_) =>
        findAndUpdate(finder, modifier, fetchNewObject = true).map {
          case result if result.lastError.isDefined && result.lastError.get.err.isDefined =>
            logDatabaseResult(result.lastError.get.err.getOrElse("No error message found"))
            MissingDeclaration
          case _ =>
            CancellationRequested
        }
      case _ => Future.successful(MissingDeclaration)
    }
  }

  private def logDatabaseResult(errorDescription: String): Unit =
    logger.error("Problem during updating database: " + errorDescription)
}
