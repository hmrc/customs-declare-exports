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

import integration.uk.gov.hmrc.exports.repositories.NotificationsRepositorySpec
import integration.uk.gov.hmrc.exports.repositories.SubmissionRepositorySpec.submission
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc.Results.Accepted
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.metrics.{ExportsMetrics, MetricIdentifiers}
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.NotificationService
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}
import unit.uk.gov.hmrc.exports.base.AuthTestSupport
import util.testdata.NotificationTestData

import scala.concurrent.Future
import scala.util.Random

class NotificationsControllerSpec extends WordSpec
    with GuiceOneAppPerSuite with AuthTestSupport with MustMatchers  with BeforeAndAfterEach with NotificationTestData {

  import NotificationsControllerSpec._

  private val notificationServiceMock: NotificationService = mock[NotificationService]
  private val notificationsRepositoryMock: NotificationsRepository = mock[NotificationsRepository]
  private val submissionRepositoryMock: SubmissionRepository = mock[SubmissionRepository]

  override lazy val app: Application = GuiceApplicationBuilder().overrides(
    bind[AuthConnector].to(mockAuthConnector),
    bind[NotificationService].to(notificationServiceMock),
    bind[NotificationsRepository].to(notificationsRepositoryMock),
    bind[SubmissionRepository].to(submissionRepositoryMock)
  ).build()

  private def injector: Injector = app.injector
  private def appConfig: AppConfig = injector.instanceOf[AppConfig]
  private lazy val metrics: ExportsMetrics = injector.instanceOf[ExportsMetrics]

  override def beforeEach: Unit = {
    super.beforeEach()
    when(notificationServiceMock.save(any())).thenReturn(Future.successful(Accepted))
  }


//  private def haveNotifications(notifications: Seq[Notification]): Unit =
//    when(notificationsRepositoryMock.findByEori(any())).thenReturn(Future.successful(notifications))

  private def withNotificationSaved(ok: Boolean): Unit =
    when(notificationsRepositoryMock.save(any())).thenReturn(Future.successful(ok))

//  private def withSubmissionNotification(notifications: Seq[Notification]): Unit =
//    when(notificationsRepositoryMock.getByEoriAndConversationId(any(), any()))
//      .thenReturn(Future.successful(notifications))


  "Notifications controller" should {

    "return 202 status when it successfully get notifications" in {
      withAuthorizedUser()

      val result = route(app, FakeRequest(GET, getNotificationUri)).get

      status(result) must be(OK)
    }

    "return 202 status when it successfully save notification" in {
      when(submissionRepositoryMock.findSubmissionByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))
      withNotificationSaved(true)

      val result = route(app, FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "handle reject notification correctly and Mrn parsed" in {
      val notificationMRN = "19GB3FG7C5D8FFGV00"
      when(submissionRepositoryMock.findSubmissionByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))
      withNotificationSaved(true)

      val result = route(
        app,
        FakeRequest(POST, postNotificationUri)
          .withHeaders(validHeaders.toSeq: _*)
          .withXmlBody(
            exampleRejectNotification(notificationMRN, now.withZone(DateTimeZone.UTC).toString("yyyyMMddHHmmssZ"))
          )
      ).get

      status(result) must be(ACCEPTED)

      val notificationCaptor: ArgumentCaptor[Notification] = ArgumentCaptor.forClass(classOf[Notification])
      verify(notificationServiceMock, atLeastOnce()).save(notificationCaptor.capture())

      notificationCaptor.getValue.mrn must be(notificationMRN)
    }

    "return 202 status when it unable to find submission for conversationID" in {
      when(submissionRepositoryMock.findSubmissionByConversationId(any[String])).thenReturn(Future.successful(None))

      val result = route(app, FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "return 202 status if it fail to save notification" in {
      when(submissionRepositoryMock.findSubmissionByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withNotificationSaved(false)

      val result = route(app, FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "return 200 status when there are notifications connected to specific submission" in {
      withAuthorizedUser()

      val result = route(app, FakeRequest(GET, submissionNotificationUri).withHeaders(validHeaders.toSeq: _*)).get

      status(result) must be(OK)
    }

    "return 200 status when there are no notifications connected to specific submission" in {
      withAuthorizedUser()

      val result = route(app, FakeRequest(GET, submissionNotificationUri).withHeaders(validHeaders.toSeq: _*)).get

      status(result) must be(OK)
    }

    "record notification timing and increase the Success Counter when response is OK" in {

      val timer = metrics.timers(MetricIdentifiers.notificationMetric).getCount
      val counter = metrics.counters(MetricIdentifiers.notificationMetric).getCount
      when(submissionRepositoryMock.findSubmissionByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withNotificationSaved(true)

      val result = route(app, FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)

      metrics.timers(MetricIdentifiers.notificationMetric).getCount mustBe timer + 1
      metrics.counters(MetricIdentifiers.notificationMetric).getCount mustBe counter + 1
    }
  }

//  val olderNotification = Notification(
//    conversationId = "convId",
//    eori = "eori",
//    mrn = mrn,
//    metadata = DeclarationMetadata(),
//    response = Seq(
//      Response(functionCode = "02", issueDateTime = Some(ResponseDateTimeElement(DateTimeString("102", "20190224"))))
//    )
//  )
//
//  val newerNotification = Notification(
//    conversationId = "convId",
//    eori = "eori",
//    mrn = mrn,
//    metadata = DeclarationMetadata(),
//    response = Seq(
//      Response(functionCode = "02", issueDateTime = Some(ResponseDateTimeElement(DateTimeString("102", "20190227"))))
//    )
//  )

  "Saving notification" should {

    //    "call Notifications Service save method" in {
    //      when(submissionRepositoryMock.findSubmissionByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))
    //      withAuthorizedUser()
    //      withSubmissionNotification(Seq.empty)
    //      withNotificationSaved(true)
    //
    //      reset(notificationServiceMock)
    //      val result =
    //        route(
    //          app,
    //          FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(notificationXML(mrn))
    //        ).get
    //
    //      status(result) must be(ACCEPTED)
    //      verify(notificationServiceMock, times(1)).save(any())
    //    }

    "return UnsupportedMediaType if Content Type header is empty" in {
      val result = route(
        app,
        FakeRequest(POST, postNotificationUri)
          .withHeaders(noContentTypeHeader.toSeq: _*)
          .withXmlBody(notificationXML(mrn))
      ).get

      status(result) must be(UNSUPPORTED_MEDIA_TYPE)
    }

    // TODO: decide on corner case - time change 1 hour forward, 1 hour backward, what is the risk ?
  }
}

object NotificationsControllerSpec {

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

  val notification = NotificationsRepositorySpec.notification
}
