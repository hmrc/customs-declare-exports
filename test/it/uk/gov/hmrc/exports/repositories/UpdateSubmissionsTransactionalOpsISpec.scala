/*
 * Copyright 2023 HM Revenue & Customs
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

import testdata.ExportsTestData
import testdata.ExportsTestData.actionId
import testdata.SubmissionTestData.{action, notificationSummary_1, notificationSummary_2, pendingSubmissionWithoutMrn, submission}
import testdata.notifications.NotificationTestData.{notification, notificationForExternalAmendment}
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.AMENDMENT_DRAFT
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{AMENDED, EnhancedStatus, GOODS_ARRIVED_MESSAGE, UNKNOWN}
import uk.gov.hmrc.exports.models.declaration.submissions.{ExternalAmendmentRequest, Submission, SubmissionRequest, SubmissionStatus}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateSubmissionsTransactionalOpsISpec extends IntegrationTestSpec {

  private val appConfig = mock[AppConfig]
  private val mongoComponent = instanceOf[MongoComponent]
  private val declarationRepository = instanceOf[DeclarationRepository]
  private val notificationRepository = instanceOf[ParsedNotificationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]

  private val transactionalOps =
    new UpdateSubmissionsTransactionalOps(mongoComponent, declarationRepository, submissionRepository, notificationRepository, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    declarationRepository.removeAll.futureValue
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
          "return the Submission document with the action including a new NotificationSummary and MRN" in {
            val storedSubmission = submissionRepository.insertOne(pendingSubmissionWithoutMrn).futureValue.toOption.get
            testUpdateSubmissionAndNotifications(actionId, List(notification), storedSubmission, UNKNOWN, 1)
          }
        }

        "the stored Submission document's related action DOES CONTAIN already one or more NotificationSummaries" should {
          "return the Submission document with the action including, prepended since more recent, a new NotificationSummary" in {
            val actionWithNotificationSummaries = action.copy(notifications = Some(List(notificationSummary_2, notificationSummary_1)))
            val submissionWithActionWithNotificationSummaries = submission.copy(actions = List(actionWithNotificationSummaries))
            val storedSubmission = submissionRepository.insertOne(submissionWithActionWithNotificationSummaries).futureValue.toOption.get

            val details = notification.details.copy(status = SubmissionStatus.ACCEPTED)

            testUpdateSubmissionAndNotifications(actionId, List(notification.copy(details = details)), storedSubmission, GOODS_ARRIVED_MESSAGE, 3)
          }
        }
      }

      "an 'external amendment' Notification is given" should {
        "add a new Action to the Submission and" should {
          "remove any existing 'AMENDMENT_DRAFT' declarations" in {
            val declaration =
              aDeclaration(withEori(ExportsTestData.eori), withStatus(AMENDMENT_DRAFT), withParentDeclarationId(submission.latestDecId.value))
            declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

            submission.latestDecId.value mustBe submission.uuid
            submission.latestVersionNo mustBe 1
            submission.actions.size mustBe 1
            submission.actions.head.requestType mustBe SubmissionRequest
            submissionRepository.insertOne(submission).futureValue.isRight mustBe true

            val submissionForAmendment =
              transactionalOps.updateSubmissionAndNotifications(actionId, List(notificationForExternalAmendment), submission).futureValue.get

            submissionForAmendment.latestDecId mustBe None
            submissionForAmendment.latestVersionNo mustBe 2
            submissionForAmendment.actions.size mustBe 2
            val externalAmendAction = submissionForAmendment.actions.find(_.requestType == ExternalAmendmentRequest).value
            externalAmendAction.decId mustBe None
            externalAmendAction.versionNo mustBe 2

            val submissionAction = submissionForAmendment.actions.find(_.requestType == SubmissionRequest).value
            submissionAction.decId mustBe Some(submission.uuid)
            submissionAction.versionNo mustBe 1
            submissionAction.notifications.get.size mustBe 1
            submissionAction.notifications.get.last.enhancedStatus mustBe AMENDED

            declarationRepository.findAll().futureValue.size mustBe 0
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
      if (submission.mrn.isDefined) actualSubmission.mrn mustBe submission.mrn
      else actualSubmission.mrn mustBe defined

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
