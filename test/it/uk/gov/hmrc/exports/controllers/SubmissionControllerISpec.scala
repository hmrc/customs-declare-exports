/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.controllers.routes.SubmissionController
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

class SubmissionControllerISpec extends IntegrationTestSpec with AuthTestSupport {

  private val repository = instanceOf[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
    postToDownstreamService("/auth/authorise", OK, enrolments)
  }

  "SubmissionController.find" should {

    "return a 200 response" when {
      "a matching submission is found" in {
        repository.insertOne(submission).futureValue

        val getRequest = getWithAuth(SubmissionController.find(SubmissionControllerISpec.id))
        val response = route(app, getRequest).get
        status(response) mustBe OK
        val result = contentAsJson(response).as[Submission]
        result mustBe submission
      }
    }

    "return a 400 response" when {
      "no matching submission is found" in {
        val getRequest = getWithAuth(SubmissionController.find(SubmissionControllerISpec.id))
        val response = route(app, getRequest).get
        status(response) mustBe NOT_FOUND
      }
    }
  }

  "SubmissionController.isLrnAlreadyUsed" should {

    "return a 200 response with 'true'" when {
      "a lrn has been used within 48hrs" in {
        repository.insertOne(submission).futureValue

        val getRequest = getWithAuth(SubmissionController.isLrnAlreadyUsed(SubmissionControllerISpec.lrn))
        val response = route(app, getRequest).get
        status(response) mustBe OK
        val result = contentAsJson(response).as[Boolean]
        result mustBe true
      }
    }

    "return a 400 response with 'false'" when {
      "a lrn has not been used within 48hr" in {
        val getRequest = getWithAuth(SubmissionController.isLrnAlreadyUsed("lrn"))
        val response = route(app, getRequest).get
        status(response) mustBe OK
        val result = contentAsJson(response).as[Boolean]
        result mustBe false
      }
    }
  }
}

object SubmissionControllerISpec extends IntegrationTestSpec {

  val id = UUID.randomUUID.toString
  val actionId = "74d4670c-93ab-41df-99eb-d811fd5de75f"
  val lrn = "MNscA32pIUdNv6nzo"
  val now = ZonedDateTime.now(ZoneId.of("UTC"))

  val submission = Json
    .parse(s"""{
       |  "uuid" : "$id",
       |  "eori" : "$eori",
       |  "lrn" : "$lrn",
       |  "ducr" : "5OG921285214345-PV45",
       |  "actions" : [
       |    {
       |      "id" : "$actionId",
       |      "requestType" : "SubmissionRequest",
       |      "requestTimestamp" : "$now",
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
       |  "mrn" : "22GB9515JH78573779",
       |  "latestDecId" : "$id",
       |  "latestVersionNo" : 1,
       |  "blockAmendments" : false
       |}
      |""".stripMargin)
    .as[Submission]
}
