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

package uk.gov.hmrc.exports.controllers.actions

import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}
import uk.gov.hmrc.exports.models.SubmissionResponse

class ExportControllerSpec extends CustomsExportsBaseSpec with ExportsTestData {
  val uri = "/save-submission-response"
  val jsonBody = Json.toJson[SubmissionResponse](submissionResponse)
  val fakeRequest = FakeRequest("POST", uri).withBody((jsonBody))

  "Auth Action" should {
    "return InsufficientEnrolments when EORI number is missing" in {
      userWithoutEori()

      val result = route(app, fakeRequest).get

      status(result) must be (UNAUTHORIZED)
    }

    "return a success  when a valid request with Enrollments" in {
      withAuthorizedUser()
      withSubmissionSaved(true)

      val result = route(app, fakeRequest).get

      status(result) must be (OK)
    }

    "return an Internal Server Error when there is a problem with the service" in {
      withAuthorizedUser()
      withSubmissionSaved(false)

      val result = route(app, fakeRequest).get

      status(result) must be (INTERNAL_SERVER_ERROR)
    }
  }
}
