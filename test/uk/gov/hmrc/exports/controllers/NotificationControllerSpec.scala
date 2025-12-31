/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testdata.SubmissionTestData.submission
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitTestMockBuilder.buildNotificationServiceMock
import uk.gov.hmrc.exports.base.{AuthTestSupport, MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.{CustomsHeaderNames, HeaderValidator}
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification.REST
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.exports.services.notifications.NotificationService
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}
import org.mockito.Mockito.{times, verify, when}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import scala.xml.NodeSeq
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

class NotificationControllerSpec extends UnitSpec with AuthTestSupport with MockMetrics {

  import NotificationControllerSpec._

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val notificationService: NotificationService = buildNotificationServiceMock
  private val submissionService: SubmissionService = mock[SubmissionService]

  private val controller =
    new NotificationController(authenticator, new HeaderValidator(), exportsMetrics, notificationService, submissionService, cc)

  override def beforeEach(): Unit = {
    super.beforeEach()
    withAuthorizedUser()
  }

  override def afterEach(): Unit = {
    reset(mockAuthConnector, notificationService, submissionService)
    super.afterEach()
  }

  "NotificationController.findAll" should {

    def findAll: Future[Result] = controller.findAll("1234")(FakeRequest(GET, "/submission/notifications"))

    "return 200" when {
      "submission found" in {
        when(submissionService.findSubmission(any(), any())).thenReturn(Future.successful(Some(submission)))
        when(notificationService.findAllNotificationsSubmissionRelated(any())).thenReturn(Future.successful(Seq(notification)))

        val result = findAll

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq(notification))(REST.notificationsWrites))
      }
    }

    "not return notifications" when {
      "those notifications have not had the details parsed from them" in {
        when(submissionService.findSubmission(any(), any())).thenReturn(Future.successful(Some(submission)))
        when(notificationService.findAllNotificationsSubmissionRelated(any())).thenReturn(Future.successful(Seq.empty))

        val result = findAll

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq.empty[ParsedNotification])(REST.notificationsWrites))
      }
    }

    "return 400" when {
      "submission not found" in {
        when(submissionService.findSubmission(any(), any())).thenReturn(Future.successful(None))

        val result = findAll

        status(result) must be(NOT_FOUND)
      }
    }

    "return 401" when {
      "not authenticated" in {
        userWithoutEori()

        val failedResult = findAll

        status(failedResult) must be(UNAUTHORIZED)
      }
    }
  }

  "Notification Controller on saveNotification" when {

    def saveNotification(headers: Map[String, String] = validHeaders): Future[Result] =
      controller.saveNotification(
        FakeRequest(POST, "/customs-declare-exports/notify")
          .withHeaders(headers.toSeq: _*)
          .withBody(exampleRejectNotification(mrn).asXml)
      )

    "everything works correctly" should {

      "return Accepted status" in {
        when(notificationService.handleNewNotification(anyString(), any[NodeSeq])).thenReturn(Future.successful((): Unit))

        val result = saveNotification()

        status(result) must be(ACCEPTED)
      }

      "call NotificationService once" in {
        when(notificationService.handleNewNotification(anyString(), any[NodeSeq])).thenReturn(Future.successful((): Unit))

        saveNotification().futureValue

        verify(notificationService).handleNewNotification(anyString(), any[NodeSeq])
      }
    }

    "a mandatory header is missiing" should {
      "return 400" in {
        val invalidHeaders = validHeaders - CustomsHeaderNames.XConversationIdName
        val result = saveNotification(invalidHeaders)
        status(result) must be(BAD_REQUEST)
        assert(contentAsString(result).contains("Invalid headers"))
      }
    }

    "NotificationService returns failure" should {
      "throw an Exception" in {
        when(notificationService.handleNewNotification(any(), any[NodeSeq]))
          .thenReturn(Future.failed(new Exception("Test Exception")))

        an[Exception] mustBe thrownBy {
          saveNotification().futureValue
        }
      }
    }
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
