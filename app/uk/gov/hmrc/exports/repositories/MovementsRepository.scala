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
import play.api.Logger
import play.api.libs.json.JsString
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.exports.models.MovementSubmissions
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementsRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
    extends ReactiveRepository[MovementSubmissions, BSONObjectID](
      "movements",
      mc.mongoConnector.db,
      MovementSubmissions.formats,
      objectIdFormats
    ) with AtomicUpdate[MovementSubmissions] {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(Seq("conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdIdx")),
    Index(Seq("ducr" -> IndexType.Ascending), unique = true, name = Some("ducrIdx"))
  )

  def findByEori(eori: String): Future[Seq[MovementSubmissions]] = find("eori" -> JsString(eori))

  def getByConversationId(conversationId: String): Future[Option[MovementSubmissions]] =
    find("conversationId" -> JsString(conversationId)).map(_.headOption)

  def getByEoriAndDucr(eori: String, ducr: String): Future[Option[MovementSubmissions]] =
    find("eori" -> JsString(eori), "ducr" -> JsString(ducr)).map(_.headOption)

  override def isInsertion(newRecordId: BSONObjectID, oldRecord: MovementSubmissions): Boolean =
    newRecordId.equals(oldRecord.id)

  def save(movementSubmission: MovementSubmissions) = insert(movementSubmission).map { res =>
    if (!res.ok)
    // $COVERAGE-OFF$Trivial
      Logger.error("Error during inserting movement result " + res.writeErrors.mkString("--"))
    // $COVERAGE-ON$
    res.ok
  }

  def updateMovementStatus(movementSubmission: MovementSubmissions) = {
    val finder = BSONDocument("_id" -> movementSubmission.id, "conversationId" -> movementSubmission.conversationId)

    val modifier = BSONDocument("$set" -> BSONDocument("status" -> movementSubmission.status))
    atomicUpdate(finder, modifier).map(res => res.get.writeResult.ok)
  }

}
