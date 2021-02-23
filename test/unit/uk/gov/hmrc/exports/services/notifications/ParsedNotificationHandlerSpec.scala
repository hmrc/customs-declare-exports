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

package uk.gov.hmrc.exports.services.notifications

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.verifyNoInteractions
import org.scalatest.concurrent.IntegrationPatience
import testdata.ExportsTestData.{actionId, mrn}
import testdata.RepositoryTestData.{dummyWriteResponseFailure, dummyWriteResponseSuccess}
import testdata.SubmissionTestData.submission
import testdata.notifications.ExampleXmlAndNotificationDetailsPair
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository, WriteResponse}

class ParsedNotificationHandlerSpec extends UnitSpec with IntegrationPatience {

  private val notificationMailer = mock[NotificationMailer]
  private val submissionRepository = mock[SubmissionRepository]
  private val notificationRepository = mock[NotificationRepository]

  private val parsedNotificationHandler = new ParsedNotificationHandler(notificationMailer, notificationRepository, submissionRepository)

  private val waitBeforeInMillis = 50

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(notificationMailer, notificationRepository, submissionRepository)
  }

  "ParsedNotificationHandler on saveParsedNotifications" when {

    "the notification's details are not given" should {
      "have no interaction with notificationMailer, notificationRepo and submissionRepo" in {
        val payload = dataForEmptyNotification(mrn).asXml.toString
        val notificationWithNoDetails = Notification(actionId, payload, None)

        parsedNotificationHandler.saveParsedNotifications(notificationWithNoDetails, List.empty).futureValue

        Thread.sleep(waitBeforeInMillis)
        verifyNoInteractions(notificationMailer, notificationRepository, submissionRepository)
      }
    }

    "a single parsed notification cannot be saved" should {
      "have no interaction with notificationMailer and submissionRepo" in {
        when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseFailure)

        val data = dataForReceivedNotification(mrn)
        val payload = data.asXml.toString
        val unparsedNotification = Notification(actionId, payload, None)

        parsedNotificationHandler.saveParsedNotifications(unparsedNotification, data.asDomainModel).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(notificationRepository).add(any[Notification])(any)
        verifyNoInteractions(notificationMailer, submissionRepository)

        And("the unparsed notification should not be removed")
        verify(notificationRepository, never).removeUnparsedNotificationsForActionId(any[String])
      }
    }

    "for a notification with a single dmsdoc response, the related submission cannot be updated" should {
      "have no interaction with notificationMailer" in {
        val data = dataForDmsdocNotification(mrn)
        val payload = data.asXml.toString

        val parsedNotification = Notification(actionId, payload, Some(data.asDomainModel.head))
        when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseSuccess(parsedNotification))

        when(submissionRepository.updateMrn(any[String], any[String])).thenReturn(Future.successful(None))

        val unparsedNotification = Notification(actionId, payload, None)
        parsedNotificationHandler.saveParsedNotifications(unparsedNotification, data.asDomainModel).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(notificationRepository).add(any[Notification])(any)
        verify(submissionRepository).updateMrn(any[String], any[String])
        verifyNoInteractions(notificationMailer)

        And("the unparsed notification should not be removed")
        verify(notificationRepository, never).removeUnparsedNotificationsForActionId(any[String])
      }
    }

    "a notification has a single dmsdoc response" should {
      "call notificationMailer on sendEmailForDmsdocNotification" in {
        testDmsdocNotification(dataForDmsdocNotification(mrn), 1)
      }
    }

    "a notification has multiple dmsdoc responses" should {
      "call notificationMailer on sendEmailForDmsdocNotification" in {
        testDmsdocNotification(dataForDmsdocNotificationWithMultipleResponses(mrn), 2)
      }
    }

    "a notification has only a single non-dmsdoc responses" should {
      "have no interaction with notificationMailer" in {
        testNonDmsdocNotification(dataForReceivedNotification(mrn), 1)
      }
    }

    "a notification has multiple non-dmsdoc responses" should {
      "have multiple interactions with notificationRepo and submissionRepo but no one with notificationMailer" in {
        testNonDmsdocNotification(dataForNotificationWithMultipleResponses(mrn), 2)
      }
    }
  }

  private def testDmsdocNotification(data: ExampleXmlAndNotificationDetailsPair, invocations: Int): WriteResponse[Unit] = {
    val payload = data.asXml.toString

    val parsedNotification = Notification(actionId, payload, Some(data.asDomainModel.head))
    when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseSuccess(parsedNotification))

    when(submissionRepository.updateMrn(any[String], any[String])).thenReturn(Future.successful(Some(submission)))

    when(notificationMailer.sendEmailForDmsdocNotification(any, any)(any)).thenReturn(Future.successful(unit))

    val unparsedNotification = Notification(actionId, payload, None)
    parsedNotificationHandler.saveParsedNotifications(unparsedNotification, data.asDomainModel).futureValue

    // The notification under test has 3 responses, of which only 2 are dmsdoc responses.
    val repoInvocations = if (invocations > 1) invocations + 1 else invocations

    Thread.sleep(waitBeforeInMillis)
    verify(notificationRepository, times(repoInvocations)).add(any[Notification])(any)
    verify(submissionRepository, times(repoInvocations)).updateMrn(any[String], any[String])
    verify(notificationMailer, times(invocations)).sendEmailForDmsdocNotification(any, any)(any)

    And("the unparsed notification should be removed")
    verify(notificationRepository).removeUnparsedNotificationsForActionId(any[String])
  }

  private def testNonDmsdocNotification(data: ExampleXmlAndNotificationDetailsPair, invocations: Int): WriteResponse[Unit] = {
    val payload = data.asXml.toString
    val unparsedNotification = Notification(actionId, payload, None)
    val parsedNotification = Notification(actionId, payload, Some(data.asDomainModel.head))

    when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseSuccess(parsedNotification))
    when(notificationRepository.removeUnparsedNotificationsForActionId(any[String])).thenReturn(Future.successful(Right(unit)))
    when(submissionRepository.updateMrn(any[String], any[String])).thenReturn(Future.successful(Some(submission)))

    parsedNotificationHandler.saveParsedNotifications(unparsedNotification, data.asDomainModel).futureValue

    Thread.sleep(waitBeforeInMillis)
    verify(notificationRepository, times(invocations)).add(any[Notification])(any)
    verify(submissionRepository, times(invocations)).updateMrn(any[String], any[String])
    verifyNoInteractions(notificationMailer)

    And("the unparsed notification should be removed")
    verify(notificationRepository).removeUnparsedNotificationsForActionId(any[String])
  }
}
