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

import java.time.ZonedDateTime

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
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.emails.{SendEmailRequest, VerifiedEmailAddress}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository, WriteResponse}

class NotificationHelperSpec extends UnitSpec with IntegrationPatience {

  private val customsDataStoreConnector = mock[CustomsDataStoreConnector]
  private val emailConnector = mock[EmailConnector]
  private val submissionRepository = mock[SubmissionRepository]
  private val notificationRepository = mock[NotificationRepository]

  private val notificationHelper = new NotificationHelper(customsDataStoreConnector, emailConnector, notificationRepository, submissionRepository)

  private val waitBeforeInMillis = 50

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(customsDataStoreConnector, emailConnector, notificationRepository, submissionRepository)
  }

  "NotificationHelper on parseAndSave" when {

    "the notification's details are not given" should {
      "have no interaction with customs-data-store, email-service, notificationRepo and submissionRepo" in {
        val payload = dataForEmptyNotification(mrn).asXml.toString
        val notificationWithNoDetails = Notification(actionId, payload, None)

        notificationHelper.parseAndSave(notificationWithNoDetails).futureValue

        Thread.sleep(waitBeforeInMillis)
        verifyNoInteractions(customsDataStoreConnector, emailConnector, notificationRepository, submissionRepository)
      }
    }

    "the parsed (with details) notification cannot be saved" should {
      "have no interaction with customs-data-store, email-service and submissionRepo" in {
        when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseFailure)

        val payload = dataForReceivedNotification(mrn).asXml.toString
        val unparsedNotification = Notification(actionId, payload, None)

        notificationHelper.parseAndSave(unparsedNotification).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(notificationRepository).add(any[Notification])(any)
        verifyNoInteractions(customsDataStoreConnector, emailConnector, submissionRepository)

        And("the unparsed notification should not be removed")
        verify(notificationRepository, never).removeUnparsedNotificationsForActionId(any[String])
      }
    }

    "for a notification with a single dmsdoc response, the related submission cannot be updated" should {
      "have no interaction with customs-data-store and email-service" in {
        val data = dataForDmsdocNotification(mrn)
        val payload = data.asXml.toString

        val parsedNotification = Notification(actionId, payload, Some(data.asDomainModel.head))
        when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseSuccess(parsedNotification))

        when(submissionRepository.updateMrn(any[String], any[String])).thenReturn(Future.successful(None))

        val unparsedNotification = Notification(actionId, payload, None)
        notificationHelper.parseAndSave(unparsedNotification).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(notificationRepository).add(any[Notification])(any)
        verify(submissionRepository).updateMrn(any[String], any[String])
        verifyNoInteractions(customsDataStoreConnector, emailConnector)

        And("the unparsed notification should not be removed")
        verify(notificationRepository, never).removeUnparsedNotificationsForActionId(any[String])
      }
    }

    "for a dmsdoc notification, the verified email address cannot be found" should {
      "have no interaction with the email-service" in {
        val data = dataForDmsdocNotification(mrn)
        val payload = data.asXml.toString

        val parsedNotification = Notification(actionId, payload, Some(data.asDomainModel.head))
        when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseSuccess(parsedNotification))

        when(submissionRepository.updateMrn(any[String], any[String])).thenReturn(Future.successful(Some(submission)))
        when(customsDataStoreConnector.getEmailAddress(any[String])(any)).thenReturn(Future.successful(None))

        val unparsedNotification = Notification(actionId, payload, None)
        notificationHelper.parseAndSave(unparsedNotification).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(notificationRepository).add(any[Notification])(any)
        verify(submissionRepository).updateMrn(any[String], any[String])
        verify(customsDataStoreConnector).getEmailAddress(any[String])(any)
        verifyNoInteractions(emailConnector)

        And("the unparsed notification should be removed")
        verify(notificationRepository).removeUnparsedNotificationsForActionId(any[String])
      }
    }

    "for a notification with a single dmsdoc response, the verified email address is retrieved" should {
      "interact with the email-service to require the trader to upload additional documents" in {
        testDmsdocNotification(dataForDmsdocNotification(mrn), 1)
      }
    }

    "for a notification with multiple dmsdoc responses, the verified email address is retrieved" should {
      "interact with the email-service to require the traders to upload additional documents" in {
        testDmsdocNotification(dataForDmsdocNotificationWithMultipleResponses(mrn), 2)
      }
    }

    "a notification has only a single non-dmsdoc responses" should {
      "have no interaction with customs-data-store and email-service" in {
        testNonDmsdocNotification(dataForReceivedNotification(mrn), 1)
      }
    }

    "a notification has multiple non-dmsdoc responses" should {
      "have multiple interactions with notificationRepo and submissionRepo but no one with customs-data-store and email-service" in {
        testNonDmsdocNotification(dataForNotificationWithMultipleResponses(mrn), 2)
      }
    }
  }

  private def testDmsdocNotification(data: ExampleXmlAndNotificationDetailsPair, invocations: Int): WriteResponse[Unit] = {
    val payload = data.asXml.toString

    val parsedNotification = Notification(actionId, payload, Some(data.asDomainModel.head))
    when(notificationRepository.add(any[Notification])(any)).thenReturn(dummyWriteResponseSuccess(parsedNotification))

    when(submissionRepository.updateMrn(any[String], any[String])).thenReturn(Future.successful(Some(submission)))

    val verifiedEmailAddress = Some(VerifiedEmailAddress("trader@mycomany.com", ZonedDateTime.now))
    when(customsDataStoreConnector.getEmailAddress(any[String])(any)).thenReturn(Future.successful(verifiedEmailAddress))

    val unparsedNotification = Notification(actionId, payload, None)
    notificationHelper.parseAndSave(unparsedNotification).futureValue

    // The notification under test has 3 responses, of which only 2 are dmsdoc responses.
    val repoInvocations = if (invocations > 1) invocations + 1 else invocations

    Thread.sleep(waitBeforeInMillis)
    verify(notificationRepository, times(repoInvocations)).add(any[Notification])(any)
    verify(submissionRepository, times(repoInvocations)).updateMrn(any[String], any[String])
    verify(customsDataStoreConnector, times(invocations)).getEmailAddress(any[String])(any)
    verify(emailConnector, times(invocations)).sendEmail(any[SendEmailRequest])(any)

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

    notificationHelper.parseAndSave(unparsedNotification).futureValue

    Thread.sleep(waitBeforeInMillis)
    verify(notificationRepository, times(invocations)).add(any[Notification])(any)
    verify(submissionRepository, times(invocations)).updateMrn(any[String], any[String])
    verifyNoInteractions(customsDataStoreConnector, emailConnector)

    And("the unparsed notification should be removed")
    verify(notificationRepository).removeUnparsedNotificationsForActionId(any[String])
  }
}
