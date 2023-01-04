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

package uk.gov.hmrc.exports.base

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationService

import scala.concurrent.Future
import scala.util.control.NoStackTrace

object UnitTestMockBuilder extends MockitoSugar {

  def buildCustomsDeclarationsConnectorMock: CustomsDeclarationsConnector = {
    val customsDeclarationsConnectorMock: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
    when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(Future.successful("conversation-id"))
    when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conversation-id"))
    customsDeclarationsConnectorMock
  }

  def buildExportsMetricsMock: ExportsMetrics = {
    val exportsMetricsMock: ExportsMetrics = mock[ExportsMetrics]
    when(exportsMetricsMock.startTimer(any())).thenCallRealMethod()
    exportsMetricsMock
  }

  def buildSubmissionRepositoryMock: SubmissionRepository = {
    val submissionRepositoryMock: SubmissionRepository = mock[SubmissionRepository]
    when(submissionRepositoryMock.addAction(any[String](), any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.findAll(any())).thenReturn(Future.successful(Seq.empty))
    when(submissionRepositoryMock.create(any[Submission])).thenReturn(Future.successful(mock[Submission]))
    submissionRepositoryMock
  }

  def buildNotificationRepositoryMock: ParsedNotificationRepository = {
    val notificationRepositoryMock: ParsedNotificationRepository = mock[ParsedNotificationRepository]

    when(notificationRepositoryMock.findAll(any[String], any[String])).thenReturn(Future.successful(Seq.empty))
    when(notificationRepositoryMock.findNotifications(any())).thenReturn(Future.successful(Seq.empty))

    val failure = Future.failed[ParsedNotification](new RuntimeException("Test Exception message") with NoStackTrace)
    when(notificationRepositoryMock.create(any[ParsedNotification])).thenReturn(failure)

    notificationRepositoryMock
  }

  def buildNotificationServiceMock: NotificationService = {
    val notificationServiceMock: NotificationService = mock[NotificationService]
    when(notificationServiceMock.findAllNotificationsSubmissionRelated(any())).thenReturn(Future.successful(Seq.empty))
    notificationServiceMock
  }
}
