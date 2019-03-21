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

import play.api.http.{ContentTypes, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}
import uk.gov.hmrc.exports.models._

class SubmissionControllerSpec extends CustomsExportsBaseSpec with ExportsTestData {
  val submitUri = "/declaration"
  val saveMovementUri = "/save-movement-submission"
  val updateUri = "/update-submission"
  val cancelUri = "/cancel-declaration"

  val jsonBody: JsValue = Json.toJson[MovementResponse](submissionMovementResponse)
  val fakeRequest: FakeRequest[JsValue] = FakeRequest("POST", saveMovementUri).withBody(jsonBody)

  val xmlBody: String =  randomSubmitDeclaration.toXml
  val fakeSubmitXmlRequest: FakeRequest[String] = FakeRequest("POST", submitUri).withBody(xmlBody)
  val fakeSubmitXmlRequestWithHeaders: FakeRequest[String] = fakeSubmitXmlRequest
    .withHeaders(CustomsHeaderNames.XLrnHeaderName -> declarantLrnValue,
      CustomsHeaderNames.XDucrHeaderName -> declarantDucrValue,
      AUTHORIZATION -> dummyToken,
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  val fakeSubmitXmlRequestWithMissingHeaders: FakeRequest[String] = fakeSubmitXmlRequest
    .withHeaders(
      AUTHORIZATION -> dummyToken,
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))



  def fakeRequestWithPayload(uri: String, payload: String): FakeRequest[String] =
    FakeRequest("POST", uri).withBody(payload)

  val validCancelHeaders = CustomsHeaderNames.XMrnHeaderName -> declarantMrnValue
    AUTHORIZATION -> dummyToken
    CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)

  def fakeCancellationRequest(payload: String): FakeRequest[String] = fakeRequestWithPayload(cancelUri, payload)

  def fakeCancelRequestWithHeaders(payload: String): FakeRequest[String] = fakeCancellationRequest(payload)
    .withHeaders(validCancelHeaders)

  val fakeCancelXmlRequestWithMissingHeaders: FakeRequest[String] = fakeCancellationRequest("<someXml></someXml>")
    .withHeaders(AUTHORIZATION -> dummyToken,
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))
  val submissionJson: JsValue = Json.toJson[Submission](submission)
  val jsonSeqSubmission: JsValue = Json.toJson[Seq[SubmissionData]](seqSubmissionData)

  val updateSubmissionRequest: FakeRequest[JsValue] = FakeRequest("POST", updateUri).withBody(submissionJson)

  "Actions for submission" when {

    "POST to /declaration" should {

      "return 200 status when submission has been saved" in {
        withAuthorizedUser()
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



      "return 500 status when something goes wrong" in {
        withAuthorizedUser()
        withDataSaved(false)

        val failedResult = route(app, fakeSubmitXmlRequestWithHeaders).get

        status(failedResult) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "POST to /update-submission" should {

      "return 200 status when submission has been updated" in {
        withAuthorizedUser()
        withSubmissionUpdated(true)

        val result = route(app, updateSubmissionRequest).get

        status(result) must be(OK)
      }

      "return 500 status when something goes wrong" in {
        withAuthorizedUser()
        withSubmissionUpdated(false)

        val failedResult = route(app, updateSubmissionRequest).get

        status(failedResult) must be(INTERNAL_SERVER_ERROR)
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
    }

    "POST to /save-movement-submission" should {

      "return 200 status when movement has been saved" in {
        withAuthorizedUser()
        withDataSaved(true)

        val result = route(app, fakeRequest).get

        status(result) must be(OK)
      }

      "return 500 status when something goes wrong" in {
        withAuthorizedUser()
        withDataSaved(false)

        val failedResult = route(app, fakeRequest).get

        status(failedResult) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "GET from /movements/:eori" should {

      "return 200 status with movements as response body" in {
        withAuthorizedUser()
        withMovements(Seq(movement))

        val result = route(app, FakeRequest("GET", "/movements")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq(movement)))
      }

      // TODO: 204 is safe response
      "return 200 status without empty response" in {
        withAuthorizedUser()
        withMovements(Seq.empty)

        val result = route(app, FakeRequest("GET", "/movements")).get

        status(result) must be(OK)
      }
    }

    "GET submissions using EORI number" should {

      "return 200 status with submission response body" in {
        withAuthorizedUser()
        withSubmissions(seqSubmissions)
        withNotification(Seq.empty)

        val result = route(app, FakeRequest("GET", "/submissions")).get

        status(result) must be(OK)
        contentAsJson(result) must be(jsonSeqSubmission)
      }

      // TODO: 204 is safe response
      "return 200 status without submission response" in {
        withAuthorizedUser()
        withSubmissions(Seq.empty)
        withNotification(Seq.empty)

        val result = route(app, FakeRequest("GET", "/submissions")).get

        status(result) must be(OK)
      }
    }
  }

  //TODO: add return status when declaration is cancelled
  "POST cancel declaration" should {

    "return bad request" when {

      "there is a missing required header " in {
        withAuthorizedUser()
        withCancellationRequest(CancellationRequested, Status.ACCEPTED)

        val result = route(app, fakeCancelXmlRequestWithMissingHeaders).get

        status(result) must be(BAD_REQUEST)

      }

      "none xml is sent " in {
        withAuthorizedUser()
        withCancellationRequest(CancellationRequested, Status.ACCEPTED)

        val result = route(app, fakeCancelRequestWithHeaders("SOMETEXT")).get

        status(result) must be(BAD_REQUEST)

      }

    }

    "return CancellationRequested" when {

      "there is an existing declaration" in {
        withAuthorizedUser()
        withCancellationRequest(CancellationRequested, Status.ACCEPTED)

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson[CancellationStatus](CancellationRequested))
      }


    }

    "return CancellationRequestExists" when {

      "there is an declaration with existing cancellation request, connection to api successful" in {
        withAuthorizedUser()
        withCancellationRequest(CancellationRequestExists, Status.ACCEPTED)

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson[CancellationStatus](CancellationRequestExists))
      }
    }

    "return MissingDeclaration" when {

      "there is no existing declaration" in {
        withAuthorizedUser()
        withCancellationRequest(MissingDeclaration, Status.ACCEPTED)

        val result = route(app, fakeCancelRequestWithHeaders("<someXml></someXml>")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson[CancellationStatus](MissingDeclaration))
      }
    }
  }
}
