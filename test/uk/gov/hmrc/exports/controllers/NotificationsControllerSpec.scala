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

package uk.gov.hmrc.exports.controllers

import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}
import uk.gov.hmrc.exports.models.{DeclarationMetadata, DeclarationNotification}

class NotificationsControllerSpec extends CustomsExportsBaseSpec with ExportsTestData {

  val uri = "/customs-declare-exports/notify"
  val movementUri = "/customs-declare-exports/notifyMovement"
  val submissionNotificationUri = "/customs-declare-exports/submission-notifications/1234"

  val getNotificationUri = "/customs-declare-exports/notifications/eori1"
  val validXML = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
    <wstxns1:Response xmlns:wstxns1="urn:wco:datamodel:WCO:RES-DMS:2"></wstxns1:Response>
  </MetaData>

  val movementXml = <inventoryLinkingMovementRequest xmlns="http://gov.uk/customs/inventoryLinking/v1">
    <messageCode>EAL</messageCode>
  </inventoryLinkingMovementRequest>

  val validHeaders = Map(
    "X-CDS-Client-ID" -> "1234",
    "X-Conversation-ID" -> "XConv1",
    "X-EORI-Identifier" -> "eori1",
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    "X-Badge-Identifier" -> "badgeIdentifier1"
  )

  val noEoriHeaders = Map(
    "X-CDS-Client-ID" -> "1234",
    "X-Conversation-ID" -> "XConv1",
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    "X-Badge-Identifier" -> "badgeIdentifier1"
  )

  val submissionNotification =
    DeclarationNotification(conversationId = "1234", eori = "eori", metadata = DeclarationMetadata())

  "NotificationsControllerSpec" should {
    "successfully accept Notification" in {
      withNotificationSaved(true)

      val result = route(app, FakeRequest(POST, uri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "failed to save Notification" in {
      withNotificationSaved(false)

      val result = route(app, FakeRequest(POST, uri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "500 response if no eori header in Notification" in {
      val result = route(app, FakeRequest(POST, uri).withHeaders(noEoriHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(INTERNAL_SERVER_ERROR)
      contentAsString(result) must be
      ("<errorResponse><code>INTERNAL_SERVER_ERROR</code><message>" +
        "ClientId or ConversationId or EORI is missing in the request headers</message></errorResponse>")
    }

    "get Notifications" in {
      withAuthorizedUser()
      haveNotifications(Seq(notification))

      val result = route(app, FakeRequest(GET, getNotificationUri)).get

      status(result) must be(OK)
    }

    "successfully saved movement notification" in {
      withMovementNotificationSaved(true)

      val result =
        route(app, FakeRequest(POST, movementUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(movementXml)).get

      status(result) must be(ACCEPTED)
    }

    "failed to save movement notification" in {
      withMovementNotificationSaved(false)

      val result =
        route(app, FakeRequest(POST, movementUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(movementXml)).get

      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "500 response if no eori header in movement notification" in {
      val result =
        route(app, FakeRequest(POST, movementUri).withHeaders(noEoriHeaders.toSeq: _*).withXmlBody(movementXml)).get

      status(result) must be(INTERNAL_SERVER_ERROR)
      contentAsString(result) must be
      ("<errorResponse><code>INTERNAL_SERVER_ERROR</code><message>" +
        "ClientId or ConversationId or EORI is missing in the request headers</message></errorResponse>")
    }

    "return 200 status and notifications relates with specific submission" in {
      withAuthorizedUser()
      withSubmissionNotification(Some(submissionNotification))

      val result = route(app, FakeRequest(GET, submissionNotificationUri).withHeaders(validHeaders.toSeq: _*)).get

      status(result) must be(OK)
    }

    "return 204 when there is no notifications related with submission" in {
      withAuthorizedUser()
      withSubmissionNotification(None)

      val result = route(app, FakeRequest(GET, submissionNotificationUri).withHeaders(validHeaders.toSeq: _*)).get

      status(result) must be(NO_CONTENT)
    }
  }
}
