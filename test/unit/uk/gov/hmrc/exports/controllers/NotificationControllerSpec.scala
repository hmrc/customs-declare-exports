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

import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers.{any, anyString}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testdata.SubmissionTestData.submission
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.base.UnitTestMockBuilder.buildNotificationServiceMock
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification.REST
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.exports.services.notifications.NotificationService
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.util.Random
import scala.xml.{Elem, NodeSeq}

class NotificationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with AuthTestSupport {

  import NotificationControllerSpec._

  val findSubmissionNotificationsUri = "/submission/notifications/1234"
  val findLatestNotificationUri = "/submission/latest-notification/1234"
  val findAllNotificationsForUserUri = "/notifications"
  val saveNotificationUri = "/customs-declare-exports/notify"

  SharedMetricRegistries.clear()

  private val notificationService: NotificationService = buildNotificationServiceMock
  private val submissionService: SubmissionService = mock[SubmissionService]

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].to(mockAuthConnector),
      bind[NotificationService].to(notificationService),
      bind[SubmissionService].to(submissionService)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    withAuthorizedUser()
  }

  override def afterEach(): Unit = {
    reset(mockAuthConnector, notificationService, submissionService)
    super.afterEach()
  }

  "Notification Controller on findAll" should {

    "return 200" when {
      "submission found" in {
        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq(submission)))
        when(notificationService.findAllNotificationsSubmissionRelated(any())).thenReturn(Future.successful(Seq(notification)))

        val result = routeGetFindAll

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq(notification))(REST.notificationsWrites))
      }
    }

    "not return notifications" when {
      "those notifications have not had the details parsed from them" in {
        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq(submission)))
        when(notificationService.findAllNotificationsSubmissionRelated(any())).thenReturn(Future.successful(Seq.empty))

        val result = routeGetFindAll

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq.empty[ParsedNotification])(REST.notificationsWrites))
      }
    }

    "return 400" when {
      "submission not found" in {
        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq.empty))

        val result = routeGetFindAll

        status(result) must be(NOT_FOUND)
      }
    }

    "return 401" when {
      "not authenticated" in {
        userWithoutEori()

        val failedResult = routeGetFindAll

        status(failedResult) must be(UNAUTHORIZED)
      }
    }

    def routeGetFindAll: Future[Result] = route(app, FakeRequest(GET, findSubmissionNotificationsUri)).get
  }

  "Notification Controller on saveNotification" when {

    "everything works correctly" should {

      "return Accepted status" in {
        when(notificationService.handleNewNotification(anyString(), any[NodeSeq])).thenReturn(Future.successful((): Unit))

        val result = routePostSaveNotification()

        status(result) must be(ACCEPTED)
      }

      "call NotificationService once" in {
        when(notificationService.handleNewNotification(anyString(), any[NodeSeq])).thenReturn(Future.successful((): Unit))

        routePostSaveNotification().futureValue

        verify(notificationService).handleNewNotification(anyString(), any[NodeSeq])
      }
    }

    "NotificationService returns failure" should {

      "throw an Exception" in {
        when(notificationService.handleNewNotification(any(), any[NodeSeq]))
          .thenReturn(Future.failed(new Exception("Test Exception")))

        an[Exception] mustBe thrownBy {
          routePostSaveNotification().futureValue
        }
      }
    }

    def routePostSaveNotification(headers: Map[String, String] = validHeaders, xmlBody: Elem = exampleRejectNotification(mrn).asXml): Future[Result] =
      route(
        app,
        FakeRequest(POST, saveNotificationUri)
          .withHeaders(headers.toSeq: _*)
          .withXmlBody(xmlBody)
      ).get
  }
}

object NotificationControllerSpec {

  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val mrn: String = "MRN87878797"
  val eori: String = "GB167676"

  private lazy val responseFunctionCodes: Seq[String] =
    Seq("01", "02", "03", "05", "06", "07", "08", "09", "10", "11", "16", "17", "18")

  private lazy val randomResponseFunctionCode: String = responseFunctionCodes(Random.nextInt(responseFunctionCodes.length))

  private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  private def dateTimeElement(date: LocalDate): Option[ResponseDateTimeElement] =
    Some(ResponseDateTimeElement(DateTimeString("102", date.format(formatter))))

  val response: Seq[Response] = Seq(
    Response(
      functionCode = randomResponseFunctionCode,
      functionalReferenceId = Some("123"),
      issueDateTime = dateTimeElement(LocalDate.parse("2019-02-05"))
    )
  )
}
