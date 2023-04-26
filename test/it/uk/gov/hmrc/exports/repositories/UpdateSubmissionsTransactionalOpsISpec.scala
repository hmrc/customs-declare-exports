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

import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.AMENDMENT_DRAFT
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.mongo.MongoComponent
import org.scalacheck.Gen
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.codesMap

import java.util.UUID
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
              aDeclaration(withEori(eori), withStatus(AMENDMENT_DRAFT), withParentDeclarationId(submission.latestDecId.value))
            declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

            submission.latestDecId.value mustBe submission.uuid
            submission.latestVersionNo mustBe 1
            submission.actions.size mustBe 1
            submission.actions.head.requestType mustBe SubmissionRequest
            submissionRepository.insertOne(submission).futureValue.isRight mustBe true

            val submissionForAmendment =
              transactionalOps.updateSubmissionAndNotifications(actionId, List(notificationForExternalAmendment), submission).futureValue

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

      "an 'amendment' Notification is given" should {
        "Store a NotificationSummary record for the notification against the correct AmendmentRequest action" when {
          "CUSTOMS_POSITION_GRANTED (DMSREQ and code 39) notification status" which {
            "updates the latestEnhancedStatus field to equal the latest notification status received against the current SubmissionRequest action" in {

              val notifications = Seq(
                notificationSummary_1,
                NotificationSummary(notification_2.unparsedNotificationId, notificationSummary_1.dateTimeIssued plusHours 1, GOODS_HAVE_EXITED)
              )

              val submissionAction = action.copy(notifications = Some(notifications))
              val amendmentAction = action_2.copy(versionNo = 2, requestType = AmendmentRequest)

              val testSubmission = submission.copy(actions = Seq(submissionAction, amendmentAction), latestEnhancedStatus = RECEIVED)

              val notificationForAmendment = ParsedNotification(
                unparsedNotificationId = UUID.randomUUID,
                actionId = amendmentAction.id,
                details = NotificationDetails(
                  mrn = mrn,
                  dateTimeIssued = dateTimeIssued_2,
                  status = SubmissionStatus.CUSTOMS_POSITION_GRANTED,
                  errors = Seq.empty
                )
              )

              testSubmission.latestDecId.value mustBe testSubmission.uuid
              testSubmission.latestVersionNo mustBe 1

              submissionRepository
                .insertOne(testSubmission)
                .futureValue
                .isRight mustBe true

              val submissionForAmendment = transactionalOps
                .updateSubmissionAndNotifications(amendmentAction.id, List(notificationForAmendment), testSubmission)
                .futureValue

              submissionForAmendment.latestDecId.value mustBe amendmentAction.decId.value
              submissionForAmendment.latestVersionNo mustBe 2
              submissionForAmendment.latestEnhancedStatus mustBe GOODS_HAVE_EXITED

            }
          }
          "REJECTED notification status" in {

            val submissionAction = action.copy(notifications = Some(Seq(notificationSummary_1)))
            val amendmentAction = action_2.copy(versionNo = 2, requestType = AmendmentRequest)

            val testSubmission = submission.copy(actions = Seq(submissionAction, amendmentAction))

            val notificationForAmendment = ParsedNotification(
              unparsedNotificationId = UUID.randomUUID,
              actionId = amendmentAction.id,
              details = NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued_3, status = SubmissionStatus.REJECTED, errors = Seq.empty)
            )

            testSubmission.latestDecId.value mustBe testSubmission.uuid
            testSubmission.latestVersionNo mustBe 1

            submissionRepository
              .insertOne(testSubmission)
              .futureValue
              .isRight mustBe true

            val submissionForAmendment = transactionalOps
              .updateSubmissionAndNotifications(amendmentAction.id, List(notificationForAmendment), testSubmission)
              .futureValue

            submissionForAmendment.latestDecId.value mustBe testSubmission.latestDecId.value
            submissionForAmendment.latestVersionNo mustBe 1
            submissionForAmendment.latestEnhancedStatus mustBe ON_HOLD

          }
          "CUSTOMS_POSITION_DENIED (DMSREQ and code 41) notification status" in {

            val submissionAction = action.copy(notifications = Some(Seq(notificationSummary_1)))
            val amendmentAction = action_2.copy(versionNo = 2, requestType = AmendmentRequest)

            val testSubmission = submission.copy(actions = Seq(submissionAction, amendmentAction))

            val notificationForAmendment = ParsedNotification(
              unparsedNotificationId = UUID.randomUUID,
              actionId = amendmentAction.id,
              details = NotificationDetails(
                mrn = mrn,
                dateTimeIssued = dateTimeIssued_3,
                status = SubmissionStatus.CUSTOMS_POSITION_DENIED,
                errors = Seq.empty
              )
            )

            testSubmission.latestDecId.value mustBe testSubmission.uuid
            testSubmission.latestVersionNo mustBe 1

            submissionRepository
              .insertOne(testSubmission)
              .futureValue
              .isRight mustBe true

            val submissionForAmendment = transactionalOps
              .updateSubmissionAndNotifications(amendmentAction.id, List(notificationForAmendment), testSubmission)
              .futureValue

            submissionForAmendment.latestDecId.value mustBe testSubmission.latestDecId.value
            submissionForAmendment.latestVersionNo mustBe 1
            submissionForAmendment.latestEnhancedStatus mustBe ON_HOLD

          }
        }
      }

      "raise exception" when {

        "a ParsedNotification is given without a stored Submission document (because it was removed in the meanwhile)" in {
          transactionalOps.updateSubmissionAndNotifications(actionId, List(notification), submission).futureValue mustBe None
        }

        "amendmentRequest" when {

          "status is other than CUSTOMS_POSITION_GRANTED, CUSTOMS_POSITION_DENIED, 03" in {

            val status = Gen.oneOf(codesMap removedAll List("1139", "1141", "REJECTED")).sample.get._2

            val notifications = Seq(
              notificationSummary_1,
              NotificationSummary(notification_2.unparsedNotificationId, notificationSummary_1.dateTimeIssued plusHours 1, GOODS_HAVE_EXITED)
            )

            val submissionAction = action.copy(notifications = Some(notifications))
            val amendmentAction = action_2.copy(versionNo = 2, requestType = AmendmentRequest)

            val testSubmission = submission.copy(actions = Seq(submissionAction, amendmentAction), latestEnhancedStatus = RECEIVED)

            val notificationForAmendment = ParsedNotification(
              unparsedNotificationId = UUID.randomUUID,
              actionId = amendmentAction.id,
              details = NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued_2, status = status, errors = Seq.empty)
            )

            submissionRepository
              .insertOne(testSubmission)
              .futureValue
              .isRight mustBe true

            val submissionForAmendment = transactionalOps
              .updateSubmissionAndNotifications(amendmentAction.id, List(notificationForAmendment), testSubmission)
              .futureValue

            submissionForAmendment mustBe None

          }

          "SubmissionRequest of Submission is without notifications" in {

            val submissionAction = action.copy(notifications = None)
            val amendmentAction = action_2.copy(versionNo = 2, requestType = AmendmentRequest)

            val testSubmission = submission.copy(actions = Seq(submissionAction, amendmentAction), latestEnhancedStatus = RECEIVED)

            val notificationForAmendment = ParsedNotification(
              unparsedNotificationId = UUID.randomUUID,
              actionId = amendmentAction.id,
              details = NotificationDetails(
                mrn = mrn,
                dateTimeIssued = dateTimeIssued_2,
                status = SubmissionStatus.CUSTOMS_POSITION_GRANTED,
                errors = Seq.empty
              )
            )

            submissionRepository
              .insertOne(testSubmission)
              .futureValue
              .isRight mustBe true

            val submissionForAmendment = transactionalOps
              .updateSubmissionAndNotifications(amendmentAction.id, List(notificationForAmendment), testSubmission)
              .futureValue

            submissionForAmendment mustBe None

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
  ): Submission = {
    val actualSubmission = transactionalOps.updateSubmissionAndNotifications(actionId, notifications, submission).futureValue

    if (submission.mrn.isDefined) actualSubmission.mrn mustBe submission.mrn
    else actualSubmission.mrn mustBe defined

    actualSubmission.latestEnhancedStatus mustBe expectedEnhancedStatus

    actualSubmission.actions.size mustBe 1
    val notificationSummaries = actualSubmission.actions.head.notifications.value
    notificationSummaries.size mustBe expectedNotificationSummaries

    val notificationSummary = notificationSummaries.head
    notificationSummary.dateTimeIssued mustBe actualSubmission.enhancedStatusLastUpdated
    notificationSummary.enhancedStatus mustBe actualSubmission.latestEnhancedStatus
    actualSubmission

  }
}
