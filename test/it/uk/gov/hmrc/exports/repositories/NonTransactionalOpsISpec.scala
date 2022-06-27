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

package uk.gov.hmrc.exports.repositories

import testdata.ExportsTestData.actionId
import testdata.SubmissionTestData.{action, notificationSummary_1, notificationSummary_2, submission}
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.IntegrationTestMongoSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{EnhancedStatus, GOODS_ARRIVED_MESSAGE, UNKNOWN}
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionStatus}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class NonTransactionalOpsISpec extends IntegrationTestMongoSpec {

  private val appConfig = mock[AppConfig]
  private val mongoComponent = instanceOf[MongoComponent]
  private val notificationRepository = instanceOf[ParsedNotificationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]

  private val transactionalOps = new TransactionalOps(mongoComponent, submissionRepository, notificationRepository, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    notificationRepository.removeAll.futureValue
    submissionRepository.removeAll.futureValue
  }

  "TransactionalOps.updateSubmissionAndNotifications" when {

    "appConfig.useTransactionalDBOps is false, and" when {

      appConfig.useTransactionalDBOps mustBe false

      "a ParsedNotification is given without a stored Submission document (because it was removed in the meanwhile)" should {
        "return an empty option" in {
          transactionalOps.updateSubmissionAndNotifications(actionId, List(notification), submission).futureValue mustBe None
        }
      }

      "a single ParsedNotification is given with a stored Submission document and" when {

        "the stored Submission document's related action DOES NOT CONTAIN yet any NotificationSummary" should {
          "return the Submission document with the action including a new NotificationSummary" in {
            val storedSubmission = submissionRepository.insertOne(submission).futureValue.right.value
            testUpdateSubmissionAndNotifications(actionId, List(notification), storedSubmission, UNKNOWN, 1)
          }
        }

        "the stored Submission document's related action DOES CONTAIN already one or more NotificationSummaries" should {
          "return the Submission document with the action including, prepended since more recent, a new NotificationSummary" in {
            val actionWithNotificationSummaries = action.copy(notifications = Some(List(notificationSummary_2, notificationSummary_1)))
            val submissionWithActionWithNotificationSummaries = submission.copy(actions = List(actionWithNotificationSummaries))
            val storedSubmission = submissionRepository.insertOne(submissionWithActionWithNotificationSummaries).futureValue.right.value

            val details = notification.details.copy(status = SubmissionStatus.ACCEPTED)

            testUpdateSubmissionAndNotifications(actionId, List(notification.copy(details = details)), storedSubmission, GOODS_ARRIVED_MESSAGE, 3)
          }
        }
      }
    }
  }

  private def testUpdateSubmissionAndNotifications(
    actionId: String,
    notifications: Seq[ParsedNotification],
    submission: Submission,
    expectedEnhancedStatus: EnhancedStatus,
    expectedNotificationSummaries: Int
  ): Option[Submission] =
    transactionalOps.updateSubmissionAndNotifications(actionId, notifications, submission).futureValue.map { actualSubmission =>
      actualSubmission.mrn mustBe submission.mrn
      actualSubmission.latestEnhancedStatus.value mustBe expectedEnhancedStatus

      actualSubmission.actions.size mustBe 1
      val notificationSummaries = actualSubmission.actions.head.notifications.value
      notificationSummaries.size mustBe expectedNotificationSummaries

      val notificationSummary = notificationSummaries.head
      notificationSummary.dateTimeIssued mustBe actualSubmission.enhancedStatusLastUpdated.value
      notificationSummary.enhancedStatus mustBe actualSubmission.latestEnhancedStatus.value
      actualSubmission
    }
}
