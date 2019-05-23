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
import uk.gov.hmrc.wco.dec.{Response, ResponseStatus}
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

    "test buildStatus maps 39 correctly to CustomsPositionGranted when function code is 11" in new SetUp {
      val nameCode = "39"
      val functionCode = "11"
      val statusSeq = Seq(ResponseStatus(nameCode = Some(nameCode)))
      val responses = Seq(Response(functionCode, status = statusSeq))
      val notificationToSave = submissionNotification.copy(response = responses)

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)

      verify(mockSubmissionRepo).updateMrnAndStatus(any(), any(), any(), status = meq(Some(functionCode + nameCode)))

    }

    "test buildStatus maps 41 correctly to CustomsPositionDenied when function code is 11" in new SetUp {
      val nameCode = "41"
      val functionCode = "11"
      val statusSeq = Seq(ResponseStatus(nameCode = Some(nameCode)))
      val responses = Seq(Response(functionCode, status = statusSeq))
      val notificationToSave = submissionNotification.copy(response = responses)

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)

      verify(mockSubmissionRepo).updateMrnAndStatus(any(), any(), any(), status = meq(Some(functionCode + nameCode)))

    }

    "test buildStatus maps correctly to 11 when function code is 11 and namecode is not 41 or 39" in new SetUp {
      val nameCode = "15"
      val functionCode = "11"
      val statusSeq = Seq(ResponseStatus(nameCode = Some(nameCode)))
      val responses = Seq(Response(functionCode, status = statusSeq))
      val notificationToSave = submissionNotification.copy(response = responses)

      testScenario(notificationToSave, notificationSaveResult = false, submissionUpdateResult = true)

      verify(mockSubmissionRepo).updateMrnAndStatus(any(), any(), any(), status = meq(Some(functionCode)))

    }

  }
}
