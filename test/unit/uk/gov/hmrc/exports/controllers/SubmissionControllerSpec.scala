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

package unit.uk.gov.hmrc.exports.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.{ContentTypes, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.exports.base.CustomsExportsBaseSpec
import util.testdata.ExportsTestData._
import util.testdata.SubmissionTestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class SubmissionControllerSpec extends CustomsExportsBaseSpec with BeforeAndAfterEach {

  val submitUri = "/declaration"
  val updateUri = "/update-submission"
  val cancelUri = "/cancel-declaration"

  val xmlBody: String = randomSubmitDeclaration.toXml
  val fakeSubmitXmlRequest: FakeRequest[String] = FakeRequest("POST", submitUri).withBody(xmlBody)
  val fakeSubmitXmlRequestWithHeaders: FakeRequest[String] = fakeSubmitXmlRequest
    .withHeaders(
      CustomsHeaderNames.XLrnHeaderName -> declarantLrnValue,
      CustomsHeaderNames.XDucrHeaderName -> declarantDucrValue,
      AUTHORIZATION -> dummyToken,
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
    )

  val fakeSubmitXmlRequestWithMissingHeaders: FakeRequest[String] = fakeSubmitXmlRequest
    .withHeaders(AUTHORIZATION -> dummyToken, CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  def fakeRequestWithPayload(uri: String, payload: String): FakeRequest[String] =
    FakeRequest("POST", uri).withBody(payload)

  val validCancelHeaders: (String, String) = CustomsHeaderNames.XMrnHeaderName -> declarantMrnValue

  def fakeCancellationRequest(payload: String): FakeRequest[String] = fakeRequestWithPayload(cancelUri, payload)

  def fakeCancelRequestWithHeaders(payload: String): FakeRequest[String] =
    fakeCancellationRequest(payload)
      .withHeaders(validCancelHeaders)

  val fakeCancelXmlRequestWithMissingHeaders: FakeRequest[String] = fakeCancellationRequest("<someXml></someXml>")
    .withHeaders(AUTHORIZATION -> dummyToken, CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  val submissionJson: JsValue = Json.toJson[Submission](submission)
  val submissionSeq: Seq[Submission] = Seq(submission)
  val submissionSeqJson: JsValue = Json.toJson[Seq[Submission]](submissionSeq)

  val updateSubmissionRequest: FakeRequest[JsValue] = FakeRequest("POST", updateUri).withBody(submissionJson)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockDeclarationsApiConnector, mockSubmissionRepository)
  }

  "Actions for submission" when {

    "POST to /declaration" should {

      "return 200 status when submission has been saved" in {

        withAuthorizedUser()
        withCustomsDeclarationSubmission(ACCEPTED)
        withDataSaved(true)

        val result = route(app, fakeSubmitXmlRequestWithHeaders).get

        status(result) must be(ACCEPTED)
      }

      "return 400 status when required headers are missing" in {

        withAuthorizedUser()
        withDataSaved(true)

        val result = route(app, fakeSubmitXmlRequestWithMissingHeaders).get

        status(result) must be(BAD_REQUEST)
      }

      "return 500 status when something goes wrong in persisting data" in {

        withAuthorizedUser()
        withCustomsDeclarationSubmission(ACCEPTED)
        withDataSaved(false)

        val failedResult = route(app, fakeSubmitXmlRequestWithHeaders).get

        status(failedResult) must be(INTERNAL_SERVER_ERROR)
      }

      "return 500 status when something goes wrong in submission to dec api" in {

        withAuthorizedUser()
        withCustomsDeclarationSubmission(BAD_REQUEST)

        val failedResult = route(app, fakeSubmitXmlRequestWithHeaders).get

        status(failedResult) must be(INTERNAL_SERVER_ERROR)
      }

      "return 401 status when user is without eori" in {

        userWithoutEori()

        val failedResult = route(app, fakeSubmitXmlRequestWithMissingHeaders).get

        status(failedResult) must be(UNAUTHORIZED)
      }
    }

    "GET from /submission" should {

      "return 200 status with submission response body" in {

        withAuthorizedUser()
        getSubmission(Some(submission))

        val result = route(app, FakeRequest("GET", "/submission/1234")).get

        status(result) must be(OK)
        contentAsJson(result) must be(submissionJson)
      }

      // TODO: 204 is safe response
      "return 200 status without submission response" in {

        withAuthorizedUser()
        getSubmission(None)

        val result = route(app, FakeRequest("GET", "/submission/1234")).get

        status(result) must be(OK)
      }

      "return 401 status when user is without eori" in {

        userWithoutEori()

        val failedResult = route(app, FakeRequest("GET", "/submission/1234")).get

        status(failedResult) must be(UNAUTHORIZED)
      }
    }

    "GET submissions using EORI number" should {

      "return 200 status with submission response body" in {

        withAuthorizedUser()
        withSubmissions(submissionSeq)

        val result = route(app, FakeRequest("GET", "/submissions")).get

        status(result) must be(OK)
        contentAsJson(result) must be(submissionSeqJson)
      }

      // TODO: 204 is safe response
      "return 200 status without submission response" in {

        withAuthorizedUser()
        withSubmissions(Seq.empty)

        val result = route(app, FakeRequest("GET", "/submissions")).get

        status(result) must be(OK)
      }

      "return 401 status when user is without eori" in {

        userWithoutEori()

        val failedResult = route(app, FakeRequest("GET", "/submissions")).get

        status(failedResult) must be(UNAUTHORIZED)
      }
    }
  }

  //TODO: add return status when declaration is cancelled
  "POST cancel declaration" should {

    "return specific error status" when {

      "there is a missing required header - BAD_REQUEST" in {

        withAuthorizedUser()
        withCancellationRequest(CancellationRequested, Status.ACCEPTED)

        val result = route(app, fakeCancelXmlRequestWithMissingHeaders).get

        status(result) must be(BAD_REQUEST)
      }

      "none xml is sent - BAD_REQUEST" in {

        withAuthorizedUser()
        withCancellationRequest(CancellationRequested, Status.ACCEPTED)

        val result = route(app, fakeCancelRequestWithHeaders("SOMETEXT")).get

        status(result) must be(BAD_REQUEST)
      }

      "user is without EORI - UNAUTHORIZED" in {

        userWithoutEori()
        withCancellationRequest(CancellationRequestExists, Status.ACCEPTED)

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(UNAUTHORIZED)
        contentAsString(result) must be(
          "<?xml version='1.0' encoding='UTF-8'?>\n<errorResponse>\n        <code>UNAUTHORIZED</code>\n        <message>Insufficient Enrolments</message>\n      </errorResponse>"
        )
      }
    }

    "return CancellationRequested" when {

      "there is an existing declaration" in {

        withAuthorizedUser()
        when(mockSubmissionRepository.findSubmissionByMrn(any[String])).thenReturn(Future.successful(Some(submission)))
        when(mockSubmissionRepository.addAction(any[String], any[Action])).thenReturn(Future.successful(Some(cancelledSubmission)))
        when(mockDeclarationsApiConnector.submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier]))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, Some(conversationId))))

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson[CancellationStatus](CancellationRequested))
      }
    }

    "return CancellationRequestExists" when {

      "there is an declaration with existing cancellation request" in {

        withAuthorizedUser()
        when(mockSubmissionRepository.findSubmissionByMrn(any[String])).thenReturn(Future.successful(Some(cancelledSubmission)))
        when(mockSubmissionRepository.addAction(any[String], any[Action])).thenReturn(Future.successful(Some(cancelledSubmission)))
        when(mockDeclarationsApiConnector.submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier]))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, Some(conversationId))))

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson[CancellationStatus](CancellationRequestExists))
      }
    }

    "return MissingDeclaration" when {

      "there is no existing declaration" in {

        withAuthorizedUser()
        when(mockSubmissionRepository.findSubmissionByMrn(any[String])).thenReturn(Future.successful(None))
        when(mockDeclarationsApiConnector.submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier]))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, Some(conversationId))))

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson[CancellationStatus](MissingDeclaration))
      }
    }
  }

}
