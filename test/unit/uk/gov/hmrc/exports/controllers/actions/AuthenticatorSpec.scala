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

package unit.uk.gov.hmrc.exports.controllers.actions

import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import unit.uk.gov.hmrc.exports.base.CustomsExportsBaseSpec
import util.testdata.ExportsTestData._

class AuthenticatorSpec extends CustomsExportsBaseSpec {
  val uri = "/declaration"
  val xmlBody: String = randomSubmitDeclaration.toXml
  val fakeXmlRequest: FakeRequest[String] = FakeRequest("POST", uri).withBody(xmlBody)
  val fakeXmlRequestWithHeaders: FakeRequest[String] =
    fakeXmlRequest
      .withHeaders(
        CustomsHeaderNames.XLrnHeaderName -> declarantLrnValue,
        CustomsHeaderNames.XDucrHeaderName -> declarantDucrValue,
        AUTHORIZATION -> dummyToken,
        CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
      )

  "Export Controller" should {

    "return 401 status when EORI number is missing from request" in {
      userWithoutEori()

      val result = route(app, fakeXmlRequestWithHeaders).get

      status(result) must be(UNAUTHORIZED)
    }

    "return 200 status when a valid request with Enrollments is processed" in {
      withAuthorizedUser()
      withCustomsDeclarationSubmission(ACCEPTED)
      withDataSaved(true)

      val result = route(app, fakeXmlRequestWithHeaders).get

      status(result) must be(ACCEPTED)
    }

    "return 500 status when there is a problem with the service during persisting the submission" in {
      withAuthorizedUser()
      withCustomsDeclarationSubmission(ACCEPTED)
      withDataSaved(false)

      val result = route(app, fakeXmlRequestWithHeaders).get

      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "return 500 status when there is a problem with the service during submission" in {
      withAuthorizedUser()
      withCustomsDeclarationSubmission(ACCEPTED)

      val result = route(app, fakeXmlRequestWithHeaders).get

      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "handle InsufficientEnrolments error" in {
      withUnauthorizedUser(InsufficientEnrolments())

      val result = route(app, fakeXmlRequest).get

      status(result) must be(UNAUTHORIZED)
    }

    "handle AuthorisationException error" in {
      withUnauthorizedUser(InsufficientConfidenceLevel())

      val result = route(app, fakeXmlRequest).get

      status(result) must be(UNAUTHORIZED)
    }

    "handle rest of errors as InternalServerError" in {
      withUnauthorizedUser(new IllegalArgumentException())

      val result = route(app, fakeXmlRequest).get

      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }
}
