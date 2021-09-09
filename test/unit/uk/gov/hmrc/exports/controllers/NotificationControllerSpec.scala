/*
 * Copyright 2021 HM Revenue & Customs
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
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
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
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.exports.base.UnitTestMockBuilder.buildNotificationServiceMock
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.exports.services.notifications.NotificationService
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}

import scala.concurrent.Future
import scala.util.Random
import scala.xml.{Elem, NodeSeq}

class NotificationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with AuthTestSupport {

  import NotificationControllerSpec._

  val getSubmissionNotificationsUri = "/submission-notifications/1234"
  val getAllNotificationsForUserUri = "/notifications"
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

  "Notification Controller on findById" should {

    "return 200" when {
      "submission found" in {
        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq(submission)))
        when(notificationService.getNotifications(any()))
          .thenReturn(Future.successful(Seq(notification)))

        val result = route(app, FakeRequest("GET", "/declarations/1234/submission/notifications")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq(notification))(ParsedNotification.FrontendFormat.notificationsWrites))
      }
    }

    "not return notifications" when {
      "those notifications have not had the details parsed from them" in {
        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq(submission)))
        when(notificationService.getNotifications(any())).thenReturn(Future.successful(Seq.empty))

        val result = route(app, FakeRequest("GET", "/declarations/1234/submission/notifications")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq.empty[ParsedNotification])(ParsedNotification.FrontendFormat.notificationsWrites))
      }
    }

    "return 400" when {
      "submission not found" in {
        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq.empty))

        val result = route(app, FakeRequest("GET", "/declarations/1234/submission/notifications")).get

        status(result) must be(NOT_FOUND)
      }
    }

    "return 401" when {
      "not authenticated" in {
        userWithoutEori()

        val failedResult = route(app, FakeRequest("GET", "/declarations/1234/submission/notifications")).get

        status(failedResult) must be(UNAUTHORIZED)
      }
    }
  }

  "Notification Controller on getAllNotificationsForUser" when {

    "everything works correctly" should {

      "return Ok status" in {
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationService.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        val result = routeGetAllNotificationsForUser()

        status(result) must be(OK)
      }

      "return all Notifications returned by Notification Service" in {
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationService.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        val result = routeGetAllNotificationsForUser()

        contentAsJson(result) must equal(Json.toJson(notificationsFromService)(ParsedNotification.FrontendFormat.notificationsWrites))
      }

      "return only those Notifications returned by Notification Service that have been parsed" in {
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationService.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        val result = routeGetAllNotificationsForUser()

        contentAsJson(result) must equal(
          Json.toJson(Seq(notification, notification_2, notification_3))(ParsedNotification.FrontendFormat.notificationsWrites)
        )
      }

      "call Notification Service once" in {
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationService.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        routeGetAllNotificationsForUser().futureValue

        verify(notificationService).getAllNotificationsForUser(any())
      }
    }

    "authorisation header is missing" should {

      "return Unauthorised response" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = routeGetAllNotificationsForUser(headersWithoutAuthorisation)

        status(result) must be(UNAUTHORIZED)
      }

      "not call NotificationService" in {
        withUnauthorizedUser(InsufficientEnrolments())

        routeGetAllNotificationsForUser(headersWithoutAuthorisation).futureValue

        verifyNoInteractions(notificationService)
      }
    }

    def routeGetAllNotificationsForUser(headers: Map[String, String] = validHeaders): Future[Result] =
      route(app, FakeRequest(GET, getAllNotificationsForUserUri).withHeaders(headers.toSeq: _*)).get
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

  val now: DateTime = DateTime.now.withZone(DateTimeZone.UTC)
  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val mrn: String = "MRN87878797"
  val eori: String = "GB167676"

  private lazy val responseFunctionCodes: Seq[String] =
    Seq("01", "02", "03", "05", "06", "07", "08", "09", "10", "11", "16", "17", "18")
  private lazy val randomResponseFunctionCode: String = responseFunctionCodes(Random.nextInt(responseFunctionCodes.length))
  private def dateTimeElement(dateTimeVal: DateTime) =
    Some(ResponseDateTimeElement(DateTimeString("102", dateTimeVal.toString("yyyyMMdd"))))
  val response: Seq[Response] = Seq(
    Response(
      functionCode = randomResponseFunctionCode,
      functionalReferenceId = Some("123"),
      issueDateTime = dateTimeElement(DateTime.parse("2019-02-05T10:11:12.123"))
    )
  )
}
