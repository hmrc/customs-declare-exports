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

package uk.gov.hmrc.exports.base

import java.util.UUID

import org.joda.time.DateTime
import play.api.http.{ContentTypes, HeaderNames}
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.mvc.Codec
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.wco.dec.inventorylinking.movement.response.InventoryLinkingMovementResponse
import uk.gov.hmrc.wco.dec.{MetaData, Response}
import uk.gov.hmrc.exports.controllers.CustomsHeaderNames._
import uk.gov.hmrc.wco.dec.{Declaration => WcoDeclaration, MetaData, Response}

import scala.util.Random

trait ExportsTestData {
  /*
    The first time an declaration is submitted, we save it with the user's EORI, their LRN (if provided)
    and the conversation ID we received from the customs-declarations API response, generating a timestamp to record
    when this occurred.
   */

  val eori: String = "GB167676"
  val randomEori: String = randomString(8)
  val lrn: Option[String] = Some(randomString(22))
  val mrn: String = "MRN87878797"
  val mucr: String = randomString(16)
  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val ducr: String = randomString(16)

  val before: Long = System.currentTimeMillis()
  val submission: Submission = Submission(eori, conversationId, Some(ducr), lrn, Some(mrn), status = "01")
  val submissionData: SubmissionData = SubmissionData.buildSubmissionData(submission, 0)
  val seqSubmissions: Seq[Submission] = Seq(submission)
  val seqSubmissionData: Seq[SubmissionData] = Seq(submissionData)
  val dummyToken: String =
    "Bearer BXQ3/Treo4kQCZvVcCqKPlwxRN4RA9Mb5RF8fFxOuwG5WSg+S+Rsp9Nq998Fgg0HeNLXL7NGwEAIzwM6vuA6YYhRQnTRFaBhrp+1w+kVW8g1qHGLYO48QPWuxdM87VMCZqxnCuDoNxVn76vwfgtpNj0+NwfzXV2Zc12L2QGgF9H9KwIkeIPK/mMlBESjue4V]"
  val declarantEoriValue: String = "ZZ123456789000"
  val declarantLrnValue: String = "MyLrnValue1234"
  val declarantDucrValue: String = "MyDucrValue1234"
  val declarantMrnValue: String = "MyMucrValue1234"
  val movement: MovementSubmissions = MovementSubmissions(eori, conversationId, ducr, None, "Arrival")
  val contentTypeHeader: (String, String) = CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
  val Valid_X_EORI_IDENTIFIER_HEADER: (String, String) = XEoriIdentifierHeaderName -> declarantEoriValue
  val Valid_LRN_HEADER: (String, String) = XLrnHeaderName -> declarantLrnValue
  val Valid_AUTHORIZATION_HEADER: (String, String) = HeaderNames.AUTHORIZATION -> dummyToken
  val VALID_CONVERSATIONID_HEADER: (String, String) = XConversationIdName -> conversationId
  val VALID_DUCR_HEADER: (String, String) = XDucrHeaderName -> declarantDucrValue
  val VALID_MRN_HEADER: (String, String) = XMrnHeaderName -> declarantMrnValue
  val now: DateTime = DateTime.now

  private lazy val responseFunctionCodes: Seq[String] =
    Seq("01", "02", "03", "05", "06", "07", "08", "09", "10", "11", "16", "17", "18")
  protected def randomResponseFunctionCode: String = responseFunctionCodes(Random.nextInt(responseFunctionCodes.length))

  val response1: Seq[Response] = Seq(
    Response(functionCode = randomResponseFunctionCode, functionalReferenceId = Some("123"))
  )
  val response2: Seq[Response] = Seq(
    Response(functionCode = randomResponseFunctionCode, functionalReferenceId = Some("456"))
  )

  val notification = DeclarationNotification(now, conversationId, mrn, eori, DeclarationMetadata(), response1)
  val movementNotification =
    MovementNotification(now, conversationId, eori, movementResponse = InventoryLinkingMovementResponse("EAA"))
  val submissionResponse = SubmissionResponse(eori, conversationId, Some(ducr), lrn, Some(mrn), status = "01")
  val submissionMovementResponse =
    MovementResponse(eori, conversationId, ducr, Some(mucr), "Arrival", Some(GoodsHaveExitedTheCommunity.toString()))

  val ValidHeaders: Map[String, String] = Map(
    contentTypeHeader,
    Valid_AUTHORIZATION_HEADER,
    VALID_CONVERSATIONID_HEADER,
    Valid_X_EORI_IDENTIFIER_HEADER,
    Valid_LRN_HEADER,
    VALID_DUCR_HEADER,
    VALID_MRN_HEADER
  )

  def randomSubmitDeclaration: MetaData =
    MetaData(declaration = Option(WcoDeclaration(functionalReferenceId = Some(randomString(35)))))

  protected def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
}
