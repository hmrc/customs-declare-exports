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

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.{Guice, Injector}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.{GoodsLocation, TransportInformationContainer}
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.{SubmissionStatus, _}
import uk.gov.hmrc.exports.models.{LocalReferenceNumber, SubmissionRequestHeaders}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.MetaDataBuilder
import uk.gov.hmrc.exports.services.{SubmissionService, WcoMapperService}
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.exports.base.UnitTestMockBuilder.{buildCustomsDeclarationsConnectorMock, buildNotificationRepositoryMock, buildSubmissionRepositoryMock}
import util.testdata.{ExportsDeclarationBuilder, NotificationTestData}
import util.testdata.ExportsTestData._
import util.testdata.SubmissionTestData._
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.xml.NodeSeq

class SubmissionServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers with ExportsDeclarationBuilder with Eventually {

  val injector: Injector = Guice.createInjector()

  private trait Test {
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
    val customsDeclarationsConnectorMock: CustomsDeclarationsConnector = buildCustomsDeclarationsConnectorMock
    val submissionRepositoryMock: SubmissionRepository = buildSubmissionRepositoryMock
    val notificationRepositoryMock: NotificationRepository = buildNotificationRepositoryMock
    val metaDataBuilder: MetaDataBuilder = mock[MetaDataBuilder]
    val wcoMapperService: WcoMapperService = mock[WcoMapperService]
    val submissionService = new SubmissionService(
      customsDeclarationsConnector = customsDeclarationsConnectorMock,
      submissionRepository = submissionRepositoryMock,
      notificationRepository = notificationRepositoryMock,
      metaDataBuilder = injector.getInstance(classOf[MetaDataBuilder]),
      wcoMapperService = injector.getInstance(classOf[WcoMapperService])
    )(ExecutionContext.global)
  }

  "cancel" should {
    val submission = Submission("id", "eori", "lrn", None, "ducr")
    val submissionCancelled = Submission("id", "eori", "lrn", None, "ducr", Seq(Action("conv-id", CancellationRequest)))
    val cancellation = SubmissionCancellation("ref-id", "mrn", "description", "reason")

    "submit and delegate to repository" when {
      "submission exists" in new Test {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any[String](), any())).thenReturn(Future.successful(Some(submission)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequested
      }

      "submission is missing" in new Test {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))

        submissionService.cancel("eori", cancellation).futureValue mustBe MissingDeclaration
      }

      "submission exists and previously cancelled" in new Test {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepositoryMock.findSubmissionByMrn(any()))
          .thenReturn(Future.successful(Some(submissionCancelled)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequestExists
      }
    }
  }

  "submit" when {
    val declaration = aDeclaration(
      withConsignmentReferences(),
      withDestinationCountries(),
      withGoodsLocation(GoodsLocation("PL", "type", "id", Some("a"), Some("b"), Some("c"), Some("d"), Some("e"))),
      withWarehouseIdentification(Some("a"), Some("b"), Some("c"), Some("d")),
      withOfficeOfExit("id", Some("office"), Some("code")),
      withContainerData(TransportInformationContainer("id", Seq.empty)),
      withTotalNumberOfItems(Some("123"), Some("123")),
      withNatureOfTransaction("nature"),
      withItems(3)
    )
    "request to upstream service is successful" should {
      "create request submission action" in new Test {
        val conversationId = UUID.randomUUID().toString
        private val firstSubmission: Submission = mock[Submission]
        when(submissionRepositoryMock.findOrCreate(any(), any(), any())).thenReturn(Future.successful(firstSubmission))
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(Future.successful(conversationId))
        when(submissionRepositoryMock.addAction(any[Submission](), any())).thenReturn(Future.successful(mock[Submission]))

        val submission = submissionService.submit(declaration).futureValue

        val actionCaptor: ArgumentCaptor[Action] = ArgumentCaptor.forClass(classOf[Action])
        verify(submissionRepositoryMock).addAction(meq(firstSubmission), actionCaptor.capture())
        val action = actionCaptor.getValue
        action.id mustEqual conversationId
        action.requestType mustEqual SubmissionRequest
      }
    }
    "request to upstream service failed" should {
      "keep action list empty" in new Test {
        private val firstSubmission: Submission = mock[Submission]
        when(submissionRepositoryMock.findOrCreate(any(), any(), any())).thenReturn(Future.successful(firstSubmission))
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(Future.failed(new Exception()))
        when(submissionRepositoryMock.addAction(any[Submission](), any())).thenReturn(Future.successful(mock[Submission]))

        Await.ready(submissionService.submit(declaration), patienceConfig.timeout)

        verify(submissionRepositoryMock, never()).addAction(any[Submission](), any())
      }
    }

    "there are notification with conversation id" should {
      "update submission with linked mrn" in new Test {
        val conversationId = UUID.randomUUID().toString
        private val firstSubmission: Submission = mock[Submission]
        when(submissionRepositoryMock.findOrCreate(any(), any(), any())).thenReturn(Future.successful(firstSubmission))
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(Future.successful(conversationId))
        when(submissionRepositoryMock.addAction(any[Submission](), any())).thenReturn(Future.successful(firstSubmission))

        private val notification: Notification = NotificationTestData.exampleNotification(conversationId)
        private val notifications: Seq[Notification] = Seq(notification)
        when(notificationRepositoryMock.findNotificationsByActionId(conversationId)).thenReturn(Future.successful(notifications))

        submissionService.submit(declaration).futureValue

        eventually {
          verify(submissionRepositoryMock).updateMrn(conversationId, notification.mrn)
        }
      }
    }
  }

  "SubmissionService on getAllSubmissionsForUser" should {

    "return empty list if SubmissionRepository does not find any Submission" in new Test {
      submissionService.getAllSubmissionsForUser(eori).futureValue must equal(Seq.empty)
    }

    "return all submissions returned by SubmissionRepository" in new Test {
      when(submissionRepositoryMock.findAllSubmissionsForEori(meq(eori)))
        .thenReturn(Future.successful(Seq(submission, submission_2)))

      val returnedSubmissions = submissionService.getAllSubmissionsForUser(eori).futureValue

      returnedSubmissions.size must equal(2)
      returnedSubmissions must contain(submission)
      returnedSubmissions must contain(submission_2)
    }

    "call SubmissionRepository.findAllSubmissionsForUser, passing EORI provided" in new Test {
      submissionService.getAllSubmissionsForUser(eori).futureValue

      verify(submissionRepositoryMock, times(1)).findAllSubmissionsForEori(meq(eori))
    }
  }

  "SubmissionService on getSubmission" should {

    "return empty Option if SubmissionRepository does not find any Submission" in new Test {
      submissionService.getSubmission(eori, uuid).futureValue must equal(None)
    }

    "return submission returned by SubmissionRepository" in new Test {
      when(submissionRepositoryMock.findSubmissionByUuid(meq(eori), meq(uuid)))
        .thenReturn(Future.successful(Some(submission)))

      val returnedSubmission = submissionService.getSubmission(eori, uuid).futureValue

      returnedSubmission must be(defined)
      returnedSubmission.get must equal(submission)
    }

    "call SubmissionRepository.findSubmissionByUuid, passing UUID provided" in new Test {
      submissionService.getSubmission(eori, uuid).futureValue

      verify(submissionRepositoryMock, times(1)).findSubmissionByUuid(meq(eori), meq(uuid))
    }
  }
}

object SubmissionServiceSpec {
  val validatedHeadersSubmissionRequest: SubmissionRequestHeaders =
    SubmissionRequestHeaders(LocalReferenceNumber(lrn), Some(ducr))

  val submissionXml: NodeSeq = <submissionXml></submissionXml>
  val cancellationXml: NodeSeq = <cancellationXml></cancellationXml>
}
