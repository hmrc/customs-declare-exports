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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import testdata.ExportsTestData.actionId_2
import testdata.SubmissionTestData.{action_2, notificationSummary_1, notificationSummary_2, submission}
import testdata.notifications.NotificationTestData.{notification_2, notification_3}
import uk.gov.hmrc.exports.base.IntegrationTestMongoSpec
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.UNKNOWN
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository, TransactionalOps}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import scala.concurrent.ExecutionContext.Implicits.global

class ParseAndSaveActionISpec extends IntegrationTestMongoSpec {

  private val notificationFactory = instanceOf[NotificationFactory]
  private val notificationRepository = instanceOf[ParsedNotificationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]
  private val transactionalOps = instanceOf[TransactionalOps]

  private val parseAndSaveAction = new ParseAndSaveAction(notificationFactory, submissionRepository, transactionalOps)

  override def beforeEach(): Unit = {
    super.beforeEach()
    notificationRepository.removeAll.futureValue
    submissionRepository.removeAll.futureValue
  }

  private val notification2 = notification_2.copy(actionId = actionId_2, details = notification_2.details.copy(status = SubmissionStatus.ACCEPTED))

  "ParseAndSaveAction.save" when {

    /* Do not remove. It provides an example of a potential implementation in case we are notified that we could
   receive from the parsing a list of notifications with different actionIds for the same Submission document.

   This behaviour can be tested uncommenting the 'save' (loop) method in ParseAndSaveAction.scala

    "provided with multiple ParsedNotifications with different actionIds but related to the same Submission, and" when {
      "the Submission's actions DO NOT CONTAIN yet any NotificationSummary" should {
        "return the Submission with the actions including the new NotificationSummaries in desc order" in {
          submissionRepository.create(submission.copy(actions = List(action_2, action_3))).futureValue

          val notification3 =  notification_3.copy(actionId = actionId_3)
          val submissions = parseAndSaveAction.save(List(notification2, notification3)).futureValue
          submissions.size mustBe 2

          val submission1 = submissions.head
          val submission2 = submissions.last

          submission1.uuid mustBe submission2.uuid
          submission1.mrn.value mustBe notification2.details.mrn
          submission1.mrn.value mustBe submission2.mrn.value

          submission1.latestEnhancedStatus.value mustBe GOODS_ARRIVED
          submission2.latestEnhancedStatus.value mustBe UNKNOWN
          submission1.enhancedStatusLastUpdated.value mustBe notification2.details.dateTimeIssued
          submission2.enhancedStatusLastUpdated.value mustBe notification3.details.dateTimeIssued

          submission1.actions.size mustBe 2
          submission1.actions.size mustBe submission2.actions.size
          submission1.actions(0).notifications.value.size mustBe 1
          submission1.actions(1).notifications mustBe None
          submission2.actions(0).notifications.value.size mustBe 1
          submission2.actions(1).notifications.value.size mustBe 1
        }
      }
    }

    "provided with multiple ParsedNotifications with different actionIds but related to the same Submission, and" when {
      "the Submission's actions DO CONTAIN already NotificationSummaries" should {
        "return the Submission with the actions including the new NotificationSummaries in desc order" in {
          val action2 = action_2.copy(notifications = Some(List(notificationSummary_1)))
          val action3 = action_3.copy(notifications = Some(List(notificationSummary_2)))
          submissionRepository.create(submission.copy(actions = List(action2, action3))).futureValue

          val notification3 =  notification_3.copy(actionId = actionId_3)
          val submissions = parseAndSaveAction.save(List(notification2, notification3)).futureValue
          submissions.size mustBe 2

          val submission1 = submissions.head
          val submission2 = submissions.last

          submission1.uuid mustBe submission2.uuid
          submission1.mrn.value mustBe notification2.details.mrn
          submission1.mrn.value mustBe submission2.mrn.value

          submission1.latestEnhancedStatus.value mustBe GOODS_ARRIVED_MESSAGE
          submission2.latestEnhancedStatus.value mustBe UNKNOWN
          submission1.enhancedStatusLastUpdated.value mustBe notification2.details.dateTimeIssued
          submission2.enhancedStatusLastUpdated.value mustBe notification3.details.dateTimeIssued

          submission1.actions.size mustBe 2
          submission1.actions.size mustBe submission2.actions.size
          submission1.actions(0).notifications.value.size mustBe 2
          submission1.actions(1).notifications.value.size mustBe 1
          submission2.actions(0).notifications.value.size mustBe 2
          submission2.actions(1).notifications.value.size mustBe 2
        }
      }
    }
     */

    "provided with multiple ParsedNotifications with the same actionId, and" when {
      "the Submission's action DOES NOT CONTAIN yet any NotificationSummary" should {
        "return the Submission with the action including the new NotificationSummaries in desc order" in {
          submissionRepository.create(submission.copy(actions = List(action_2))).futureValue

          val notification3 = notification_3.copy(actionId = actionId_2)
          val submissions = parseAndSaveAction.save(List(notification2, notification3)).futureValue
          submissions.size mustBe 1

          val submission1 = submissions.head

          submission1.mrn.value mustBe notification2.details.mrn
          submission1.latestEnhancedStatus.value mustBe UNKNOWN
          submission1.enhancedStatusLastUpdated.value mustBe notification3.details.dateTimeIssued

          submission1.actions.size mustBe 1
          val notifications = submission1.actions(0).notifications.value
          notifications.size mustBe 2
          assert(notifications(0).dateTimeIssued.isAfter(notifications(1).dateTimeIssued))
        }
      }
    }

    "provided with multiple ParsedNotifications with the same actionId, and" when {
      "the Submission's action DOES CONTAIN already NotificationSummaries" should {
        "return the Submission with the action including all NotificationSummaries in desc order" in {
          val action2 = action_2.copy(notifications = Some(List(notificationSummary_1, notificationSummary_2)))
          submissionRepository.insertOne(submission.copy(actions = List(action2))).futureValue

          val notification3 = notification_3.copy(actionId = actionId_2)
          val submissions = parseAndSaveAction.save(List(notification2, notification3)).futureValue
          submissions.size mustBe 1

          val submission1 = submissions.head

          submission1.mrn.value mustBe notification2.details.mrn
          submission1.latestEnhancedStatus.value mustBe UNKNOWN
          submission1.enhancedStatusLastUpdated.value mustBe notification3.details.dateTimeIssued

          submission1.actions.size mustBe 1
          val notifications = submission1.actions(0).notifications.value
          notifications.size mustBe 4
          assert(
            notifications(0).dateTimeIssued.isAfter(notifications(1).dateTimeIssued) &&
              notifications(1).dateTimeIssued.isAfter(notifications(2).dateTimeIssued) &&
              notifications(2).dateTimeIssued.isAfter(notifications(3).dateTimeIssued)
          )
        }
      }
    }
  }
}