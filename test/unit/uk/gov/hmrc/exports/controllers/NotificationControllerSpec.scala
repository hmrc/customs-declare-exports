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

import com.codahale.metrics.SharedMetricRegistries
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.services.{NotificationService, SubmissionService}
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}
import unit.uk.gov.hmrc.exports.base.AuthTestSupport
import unit.uk.gov.hmrc.exports.base.UnitTestMockBuilder.{buildNotificationServiceMock, buildSubmissionServiceMock}
import testdata.NotificationTestData._
import testdata.SubmissionTestData.submission

import scala.concurrent.Future
import scala.util.Random
import scala.xml.Elem

class NotificationControllerSpec
    extends WordSpec with GuiceOneAppPerSuite with AuthTestSupport with BeforeAndAfterEach with ScalaFutures with MustMatchers {

  import NotificationControllerSpec._

  val getSubmissionNotificationsUri = "/submission-notifications/1234"
  val getAllNotificationsForUserUri = "/notifications"
  val saveNotificationUri = "/customs-declare-exports/notify"

  SharedMetricRegistries.clear()

  private val notificationServiceMock: NotificationService = buildNotificationServiceMock
  private val submissionService: SubmissionService = buildSubmissionServiceMock
  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].to(mockAuthConnector),
      bind[NotificationService].to(notificationServiceMock),
      bind[SubmissionService].to(submissionService)
    )
    .build()

  override def afterEach(): Unit = {
    reset(mockAuthConnector, notificationServiceMock)

    super.afterEach()
  }
  "GET /:id" should {
    "return 200" when {
      "submission found" in {
        withAuthorizedUser()
        when(submissionService.getSubmission(any(), any())).thenReturn(Future.successful(Some(submission)))
        when(notificationServiceMock.getNotifications(any()))
          .thenReturn(Future.successful(Seq(notification)))

        val result = route(app, FakeRequest("GET", "/declarations/1234/submission/notifications")).get

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq(notification)))
      }
    }

    "return 400" when {
      "submission not found" in {
        withAuthorizedUser()
        when(submissionService.getSubmission(any(), any())).thenReturn(Future.successful(None))

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
        withAuthorizedUser()
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationServiceMock.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        val result = routeGetAllNotificationsForUser()

        status(result) must be(OK)
      }

      "return all Notifications returned by Notification Service" in {
        withAuthorizedUser()
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationServiceMock.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        val result = routeGetAllNotificationsForUser()

        contentAsJson(result) must equal(Json.toJson(notificationsFromService))
      }

      "call Notification Service once" in {
        withAuthorizedUser()
        val notificationsFromService = Seq(notification, notification_2, notification_3)
        when(notificationServiceMock.getAllNotificationsForUser(any()))
          .thenReturn(Future.successful(notificationsFromService))

        routeGetAllNotificationsForUser().futureValue

        verify(notificationServiceMock, times(1)).getAllNotificationsForUser(any())
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

        verifyZeroInteractions(notificationServiceMock)
      }
    }

    def routeGetAllNotificationsForUser(headers: Map[String, String] = validHeaders): Future[Result] =
      route(app, FakeRequest(GET, getAllNotificationsForUserUri).withHeaders(headers.toSeq: _*)).get
  }

  "Notification Controller on saveNotification" when {

    "everything works correctly" should {

      "return Accepted status" in {
        withAuthorizedUser()
        when(notificationServiceMock.saveAll(any())).thenReturn(Future.successful(Right((): Unit)))

        val result = routePostSaveNotification()

        status(result) must be(ACCEPTED)
      }

      "call NotificationService once" in {
        withAuthorizedUser()
        when(notificationServiceMock.saveAll(any())).thenReturn(Future.successful(Right((): Unit)))

        routePostSaveNotification().futureValue

        verify(notificationServiceMock, times(1)).saveAll(any())
      }

      "call NotificationService once, even if payload contains more Response elements" in {
        withAuthorizedUser()
        when(notificationServiceMock.saveAll(any())).thenReturn(Future.successful(Right((): Unit)))

        routePostSaveNotification(xmlBody = exampleNotificationWithMultipleResponsesXML(mrn)).futureValue

        verify(notificationServiceMock, times(1)).saveAll(any())
      }

      "call NotificationService with the same amount of Notifications as it is in the payload" in {
        withAuthorizedUser()
        when(notificationServiceMock.saveAll(any())).thenReturn(Future.successful(Right((): Unit)))

        when(notificationServiceMock.buildNotificationsFromRequest(any(), any()))
          .thenReturn(Seq(notification, notification_2))

        routePostSaveNotification(xmlBody = exampleNotificationWithMultipleResponsesXML(mrn)).futureValue

        val notificationsCaptor: ArgumentCaptor[Seq[Notification]] = ArgumentCaptor.forClass(classOf[Seq[Notification]])
        verify(notificationServiceMock, times(1)).saveAll(notificationsCaptor.capture())

        notificationsCaptor.getValue.length must equal(2)
      }
    }

    "NotificationService returns Either.Left" should {

      "return InternalServerError" in {
        withAuthorizedUser()
        when(notificationServiceMock.saveAll(any())).thenReturn(Future.successful(Left("Error message")))

        val result = routePostSaveNotification()

        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    def routePostSaveNotification(headers: Map[String, String] = validHeaders, xmlBody: Elem = exampleRejectNotificationXML(mrn)): Future[Result] =
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
