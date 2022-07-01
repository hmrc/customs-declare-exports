/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.test.Helpers._
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.{AuthTestSupport, IntegrationTestSpec}
import uk.gov.hmrc.exports.controllers.SubmissionControllerISpec.submission
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import java.util.UUID

class SubmissionControllerISpec extends IntegrationTestSpec with AuthTestSupport {

  private val repository = instanceOf[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
    postToDownstreamService("/auth/authorise", OK, enrolments)
  }

  "SubmissionController.findAllBy" when {

    "receives an empty parameter" should {
      "return all Submissions for given EORI" in {
        repository.insertOne(submission).futureValue

        val getRequest = getWithAuth(routes.SubmissionController.findAllBy(SubmissionQueryParameters()))
        val response = route(app, getRequest).get
        status(response) mustBe OK
        val result = contentAsJson(response).as[List[Submission]].head
        result mustBe submission
      }
    }
  }
}

object SubmissionControllerISpec extends IntegrationTestSpec {

  val actionId = "74d4670c-93ab-41df-99eb-d811fd5de75f"

  val submission = Json
    .parse(s"""{
       |  "uuid" : "${UUID.randomUUID.toString}",
       |  "eori" : "$eori",
       |  "lrn" : "MNscA32pIUdNv6nzo",
       |  "ducr" : "5OG921285214345-PV45",
       |  "actions" : [
       |    {
       |      "id" : "$actionId",
       |      "requestType" : "SubmissionRequest",
       |      "requestTimestamp" : "2022-05-11T09:42:41.138Z[UTC]",
       |      "notifications" : [
       |        {
       |          "notificationId" : "8ce6ea97-cd82-41e7-90b3-e016c29e8768",
       |          "dateTimeIssued" : "2020-12-01T17:33:31Z[UTC]",
       |          "enhancedStatus" : "AMENDED"
       |        },
       |        {
       |          "notificationId" : "f0245e51-2be3-440c-9cf7-da09f3a0874b",
       |          "dateTimeIssued" : "2020-12-01T17:32:31Z[UTC]",
       |          "enhancedStatus" : "GOODS_ARRIVED"
       |        }
       |      ]
       |    }
       |  ],
       |  "enhancedStatusLastUpdated" : "2020-12-01T17:33:31Z[UTC]",
       |  "latestEnhancedStatus" : "AMENDED",
       |  "mrn" : "22GB9515JH78573779"
       |}
      |""".stripMargin)
    .as[Submission]
}
