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

package uk.gov.hmrc.exports.controllers

import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}
import uk.gov.hmrc.exports.models.{Submission, SubmissionResponse}

class SubmissionControllerSpec extends CustomsExportsBaseSpec with ExportsTestData {
  val saveUri = "/save-submission-response"
  val updateUri = "/update-submission"

  val jsonBody = Json.toJson[SubmissionResponse](submissionResponse)
  val fakeRequest = FakeRequest("POST", saveUri).withBody((jsonBody))

  val submissionJson = Json.toJson[Submission](submission)

  val updateSubmissionRequest = FakeRequest("POST", updateUri).withBody(submissionJson)

  "POST /save-submission-response" should {
    "return 200 when submission has been saved" in {
      withAuthorizedUser()
      withSubmissionSaved(true)
      val result = route(app, fakeRequest).get

      status(result) must be(OK)
    }

    "return 500 when something goes wrong" in {
      withAuthorizedUser()
      withSubmissionSaved(false)
      val failedResult = route(app, fakeRequest).get

      status(failedResult) must be(INTERNAL_SERVER_ERROR)
    }
  }

  "POST /update-submission" should {
    "return 200 when submission has been updated" in {
      withAuthorizedUser()
      withSubmissionUpdated(true)
      val result = route(app, updateSubmissionRequest).get

      status(result) must be(OK)
    }

    "return 500 when something goes wrong" in {
      withAuthorizedUser()
      withSubmissionUpdated(false)
      val failedResult = route(app, updateSubmissionRequest).get

      status(failedResult) must be(INTERNAL_SERVER_ERROR)
    }
  }
}
