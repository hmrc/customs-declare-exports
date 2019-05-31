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

import org.joda.time.DateTimeZone
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.metrics.MetricIdentifiers
import uk.gov.hmrc.exports.models.{DeclarationMetadata, DeclarationNotification}
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}
import unit.uk.gov.hmrc.exports.base.CustomsExportsBaseSpec
import util.{ExportsTestData, NotificationTestData}

import scala.concurrent.Future

class NotificationsControllerSpec
    extends CustomsExportsBaseSpec with ExportsTestData with BeforeAndAfterEach with NotificationTestData {

  override def beforeEach: Unit = {
    super.beforeEach()
    reset(mockSubmissionRepository, mockNotificationsRepository)
  }

  "Notifications controller" should {

    "return 202 status when it successfully get notifications" in {
      withAuthorizedUser()
      haveNotifications(Seq(notification))

      val result = route(app, FakeRequest(GET, getNotificationUri)).get

      status(result) must be(OK)
    }

    "return 202 status when it successfully save notification" in {
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withNotificationSaved(true)
      withSubmissionNotification(Seq.empty)

      val result = route(app, FakeRequest(POST, uri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "handle reject notification correctly and Mrn parsed" in {
      val notificationMRN = "19GB3FG7C5D8FFGV00"
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))
      withSubmissionNotification(Seq(notification))
      withNotificationSaved(true)

      val result = route(
        app,
        FakeRequest(POST, uri)
          .withHeaders(validHeaders.toSeq: _*)
          .withXmlBody(
            exampleRejectNotification(notificationMRN, now.withZone(DateTimeZone.UTC).toString("yyyyMMddHHmmssZ"))
          )
      ).get

      status(result) must be(ACCEPTED)

      val mrnCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      verify(mockSubmissionRepository).updateMrnAndStatus(
        any[String],
        any[String],
        mrnCaptor.capture(),
        any[Option[String]]
      )

      mrnCaptor.getValue must be(notificationMRN)

    }

    "return 202 status when it unable to find submission for conversationID" in {
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(None))

      val result = route(app, FakeRequest(POST, uri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "return 202 status if it fail to save notification" in {
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withNotificationSaved(false)
      withSubmissionNotification(Seq.empty)

      val result = route(app, FakeRequest(POST, uri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)
    }

    "return 200 status when there are notifications connected to specific submission" in {
      withAuthorizedUser()
      withSubmissionNotification(Seq(submissionNotification))

      val result = route(app, FakeRequest(GET, submissionNotificationUri).withHeaders(validHeaders.toSeq: _*)).get

      status(result) must be(OK)
    }

    "return 200 status when there are no notifications connected to specific submission" in {
      withAuthorizedUser()
      withSubmissionNotification(Seq.empty)

      val result = route(app, FakeRequest(GET, submissionNotificationUri).withHeaders(validHeaders.toSeq: _*)).get

      status(result) must be(OK)
    }

    "record notification timing and increase the Success Counter when response is OK" in {

      val timer = metrics.timers(MetricIdentifiers.notificationMetric).getCount
      val counter = metrics.counters(MetricIdentifiers.notificationMetric).getCount
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withNotificationSaved(true)
      withSubmissionNotification(Seq.empty)

      val result = route(app, FakeRequest(POST, uri).withHeaders(validHeaders.toSeq: _*).withXmlBody(validXML)).get

      status(result) must be(ACCEPTED)

      metrics.timers(MetricIdentifiers.notificationMetric).getCount mustBe timer + 1
      metrics.counters(MetricIdentifiers.notificationMetric).getCount mustBe counter + 1
    }
  }

  val olderNotification = DeclarationNotification(
    conversationId = "convId",
    eori = "eori",
    mrn = mrn,
    metadata = DeclarationMetadata(),
    response = Seq(
      Response(functionCode = "02", issueDateTime = Some(ResponseDateTimeElement(DateTimeString("102", "20190224"))))
    )
  )

  val newerNotification = DeclarationNotification(
    conversationId = "convId",
    eori = "eori",
    mrn = mrn,
    metadata = DeclarationMetadata(),
    response = Seq(
      Response(functionCode = "02", issueDateTime = Some(ResponseDateTimeElement(DateTimeString("102", "20190227"))))
    )
  )

  "Saving notification" should {

    "update submission status when there are no existing notifications" in {
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withAuthorizedUser()
      withSubmissionNotification(Seq.empty)
      withNotificationSaved(true)

      val result =
        route(
          app,
          FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(notificationXML(mrn))
        ).get

      status(result) must be(ACCEPTED)
      verify(mockSubmissionRepository, times(1)).updateMrnAndStatus("GB167676", "XConv1", mrn, Some("02"))
    }

    "update submission status when the existing notification is older than incoming one" in {
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withAuthorizedUser()
      withSubmissionNotification(Seq(olderNotification))
      withNotificationSaved(true)

      val result =
        route(
          app,
          FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(notificationXML(mrn))
        ).get

      status(result) must be(ACCEPTED)
      verify(mockSubmissionRepository, times(1)).updateMrnAndStatus("GB167676", "XConv1", mrn, Some("02"))
    }

    "not update submission when notification that exist is newer than incoming one" in {
      when(mockSubmissionRepository.getByConversationId(any[String])).thenReturn(Future.successful(Some(submission)))

      withAuthorizedUser()
      withSubmissionNotification(Seq(newerNotification))
      withNotificationSaved(true)

      val result =
        route(
          app,
          FakeRequest(POST, postNotificationUri).withHeaders(validHeaders.toSeq: _*).withXmlBody(notificationXML(mrn))
        ).get

      status(result) must be(ACCEPTED)
      verify(mockSubmissionRepository, times(0)).updateMrnAndStatus("eori1", "XConv1", mrn, Some("02"))
    }

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
