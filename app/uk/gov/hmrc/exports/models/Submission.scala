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

package uk.gov.hmrc.exports.models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.mongoEntity

case class Submission(
                       eori: String,
                       conversationId: String,
                       mrn: Option[String] = None,
                       submittedTimestamp: Long = System.currentTimeMillis(),
                       id: BSONObjectID = BSONObjectID.generate(),
                       arrivalConversationId: Option[String] = None,
                       departureConversationId: Option[String] = None,
                       status: Option[String] = Some("Pending")
                     )

object Submission {
  implicit val objectIdFormats = ReactiveMongoFormats.objectIdFormats
  implicit val formats = mongoEntity {
    Json.format[Submission]
  }
}

case class SubmissionResponse(eori: String, conversationId: String, mrn: Option[String] = None)

object SubmissionResponse {
  implicit val formats = Json.format[SubmissionResponse]
}

case class ExportsResponse(status: Int, message: String)

object ExportsResponse {
  implicit val formats = Json.format[ExportsResponse]
}