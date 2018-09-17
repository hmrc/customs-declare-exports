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
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.{mongoEntity, objectIdFormats}
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
  extends ReactiveRepository[Submission, BSONObjectID]("submissions", mc.mongoConnector.db, Submission.formats, objectIdFormats)
    with AtomicUpdate[Submission] {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(Seq("conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdIdx")),
    Index(Seq("mrn" -> IndexType.Ascending), name = Some("mrnIdx"))
  )

  def findByEori(eori: String): Future[Seq[Submission]] = find("eori" -> JsString(eori))

  def getByConversationId(conversationId: String): Future[Option[Submission]] = find("conversationId" -> JsString(conversationId)).map(_.headOption)

  def getByEoriAndMrn(eori: String, mrn: String): Future[Option[Submission]] = find("eori" -> JsString(eori), "mrn" -> JsString(mrn)).map(_.headOption)

  override def isInsertion(newRecordId: BSONObjectID, oldRecord: Submission): Boolean = newRecordId.equals(oldRecord.id)

  def save(submission:Submission) = insert(submission)
}

case class Submission(eori: String,
                      conversationId: String,
                      lrn: Option[String] = None,
                      mrn: Option[String] = None,
                      submittedTimestamp: Long = System.currentTimeMillis(),
                      id: BSONObjectID = BSONObjectID.generate())

object Submission {

  implicit val formats = mongoEntity {
    Json.format[Submission]
  }

}
