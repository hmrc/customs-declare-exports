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

package unit.uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models.{DeclarationMetadata, DeclarationNotification}
import uk.gov.hmrc.exports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.NotificationService
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement, ResponseStatus}
import util.NotificationTestData

import scala.concurrent.Future

class NotificationsServiceSpec
    extends UnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach with NotificationTestData {

  val mockNotificationRepo: NotificationsRepository = mock[NotificationsRepository]
  val mockSubmissionRepo: SubmissionRepository = mock[SubmissionRepository]
  val mockMetrics: ExportsMetrics = mock[ExportsMetrics]

  override def beforeEach: Unit =
    reset(mockMetrics, mockSubmissionRepo, mockNotificationRepo)

  trait SetUp {
    val testObj = new NotificationService(
      submissionRepository = mockSubmissionRepo,
      notificationsRepository = mockNotificationRepo,
      metrics = mockMetrics
    )

    def testScenario(
      notificationToSave: DeclarationNotification,
      notificationSaveResult: Boolean,
      submissionUpdateResult: Boolean
    ): Unit = {
      when(mockNotificationRepo.getByEoriAndConversationId(any(), any())).thenReturn(Future.successful(Seq.empty))
      when(mockNotificationRepo.save(any())).thenReturn(Future.successful(notificationSaveResult))
      when(mockSubmissionRepo.updateMrnAndStatus(any(), any(), any(), any()))
        .thenReturn(Future.successful(submissionUpdateResult))

      val result = testObj.save(notificationToSave).futureValue

      status(result) should be(ACCEPTED)
      verify(mockMetrics).incrementCounter(any())
    }
  }

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"

  "NotificationsService" should {
    "successfully Save DeclarationNotification in repo when save called and return ACCEPTED" in new SetUp {

      val notificationToSave: DeclarationNotification =
        submissionNotification.copy(dateTimeReceived = submissionNotification.dateTimeReceived.plusHours(2))

      testScenario(submissionNotification, notificationSaveResult = true, submissionUpdateResult = true)
    }

    "return ACCEPTED when submission is not able to be updated" in new SetUp {

      val notificationToSave: DeclarationNotification =
        submissionNotification.copy(dateTimeReceived = submissionNotification.dateTimeReceived.plusHours(2))

      testScenario(notificationToSave, notificationSaveResult = true, submissionUpdateResult = false)
    }

    "return ACCEPTED when notification received is older than existing notification" in new SetUp {

      val notificationToSave: DeclarationNotification =
        submissionNotification.copy(dateTimeReceived = submissionNotification.dateTimeReceived.minusHours(2))

      testScenario(notificationToSave, notificationSaveResult = true, submissionUpdateResult = true)
    }

    "return ACCEPTED when notification is not able to be persisted" in new SetUp {

      val notificationToSave: DeclarationNotification =
        submissionNotification.copy(dateTimeReceived = submissionNotification.dateTimeReceived.minusHours(2))

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)
    }

    "test buildStatus maps nameCode 39 correctly to CustomsPositionGranted when function code is 11" in new SetUp {

      val statusSeq = Seq(ResponseStatus(nameCode = Some(NameCodeGranted)))
      val responses = Seq(Response(PositionFunctionCode, status = statusSeq))
      val notificationToSave = submissionNotification.copy(response = responses)

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)

      verify(mockSubmissionRepo).updateMrnAndStatus(
        any(),
        any(),
        any(),
        status = meq(Some(PositionFunctionCode + NameCodeGranted))
      )
    }

    "test buildStatus maps nameCode 41 correctly to CustomsPositionDenied when function code is 11" in new SetUp {

      val statusSeq = Seq(ResponseStatus(nameCode = Some(NameCodeDenied)))
      val responses = Seq(Response(PositionFunctionCode, status = statusSeq))
      val notificationToSave = submissionNotification.copy(response = responses)

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)

      verify(mockSubmissionRepo).updateMrnAndStatus(
        any(),
        any(),
        any(),
        status = meq(Some(PositionFunctionCode + NameCodeDenied))
      )
    }

    "test buildStatus maps correctly to functionCode 11 when function code is 11 and nameCode is not 41 or 39" in new SetUp {

      val statusSeq = Seq(ResponseStatus(nameCode = Some(IncorrectNameCode)))
      val responses = Seq(Response(PositionFunctionCode, status = statusSeq))
      val notificationToSave = submissionNotification.copy(response = responses)

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)

      verify(mockSubmissionRepo).updateMrnAndStatus(any(), any(), any(), status = meq(Some(PositionFunctionCode)))
    }

    "return the newest notification when all notifications has issue date time" in new SetUp {
      import NotificationsServiceSpec._

      val firstNotification =
        generateNotificationWithDate(Some(ResponseDateTimeElement(DateTimeString("102", "20190522"))))
      val secondNotification =
        generateNotificationWithDate(Some(ResponseDateTimeElement(DateTimeString("102", "20190525"))))
      val thridNotification =
        generateNotificationWithDate(Some(ResponseDateTimeElement(DateTimeString("102", "20190529"))))
      val notifications: Seq[DeclarationNotification] = Seq(firstNotification, secondNotification, thridNotification)
      when(mockNotificationRepo.getByEoriAndConversationId(any(), any())).thenReturn(Future.successful(notifications))

      val result = testObj.getTheNewestExistingNotification(eori, conversationId).futureValue

      result should be(Some(thridNotification))
    }

    "return the newest notification when middle notification doesn't have issue date time" in new SetUp {
      import NotificationsServiceSpec._

      val firstNotification =
        generateNotificationWithDate(Some(ResponseDateTimeElement(DateTimeString("102", "20190522"))))
      val secondNotification = generateNotificationWithDate(None)
      val thridNotification =
        generateNotificationWithDate(Some(ResponseDateTimeElement(DateTimeString("102", "20190529"))))
      val notifications: Seq[DeclarationNotification] = Seq(firstNotification, secondNotification, thridNotification)
      when(mockNotificationRepo.getByEoriAndConversationId(any(), any())).thenReturn(Future.successful(notifications))

      val result = testObj.getTheNewestExistingNotification(eori, conversationId).futureValue

      result should be(Some(thridNotification))
    }
  }
}

object NotificationsServiceSpec {
  val eori = "123456"
  val conversationId = "654321"

  def generateNotificationWithDate(date: Option[ResponseDateTimeElement]): DeclarationNotification =
    DeclarationNotification(
      conversationId = "12345",
      eori = eori,
      mrn = conversationId,
      metadata = DeclarationMetadata(),
      response = Seq(Response(functionCode = "08", issueDateTime = date))
    )

}
