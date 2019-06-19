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
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.http.Status._
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.{LocalReferenceNumber, ValidatedHeadersSubmissionRequest}
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.exports.base.UnitTestMockBuilder.{buildCustomsDeclarationsConnectorMock, buildSubmissionRepositoryMock}
import util.testdata.ExportsTestData._
import util.testdata.SubmissionTestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class SubmissionServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers {

  import SubmissionServiceSpec._

  private trait Test {
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
    val customsDeclarationsConnectorMock: CustomsDeclarationsConnector = buildCustomsDeclarationsConnectorMock
    val submissionRepositoryMock: SubmissionRepository = buildSubmissionRepositoryMock
    val submissionService = new SubmissionService(
      customsDeclarationsConnector = customsDeclarationsConnectorMock,
      submissionRepository = submissionRepositoryMock
    )
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
      submissionService.getSubmission(uuid).futureValue must equal(None)
    }

    "return submission returned by SubmissionRepository" in new Test {
      when(submissionRepositoryMock.findSubmissionByUuid(meq(uuid))).thenReturn(Future.successful(Some(submission)))

      val returnedSubmission = submissionService.getSubmission(uuid).futureValue

      returnedSubmission must be(defined)
      returnedSubmission.get must equal(submission)
    }

    "call SubmissionRepository.findSubmissionByUuid, passing UUID provided" in new Test {
      submissionService.getSubmission(uuid).futureValue

      verify(submissionRepositoryMock, times(1)).findSubmissionByUuid(meq(uuid))
    }
  }

  "SubmissionService on getSubmissionByConversationId" should {

    "return empty Option if SubmissionRepository does not find any Submission" in new Test {
      submissionService.getSubmissionByConversationId(conversationId).futureValue must equal(None)
    }

    "return submission returned by SubmissionRepository" in new Test {
      when(submissionRepositoryMock.findSubmissionByConversationId(meq(conversationId)))
        .thenReturn(Future.successful(Some(submission)))

      val returnedSubmission = submissionService.getSubmissionByConversationId(conversationId).futureValue

      returnedSubmission must be(defined)
      returnedSubmission.get must equal(submission)
    }

    "call SubmissionRepository.findSubmissionByUuid, passing UUID provided" in new Test {
      submissionService.getSubmissionByConversationId(conversationId).futureValue

      verify(submissionRepositoryMock, times(1)).findSubmissionByConversationId(meq(conversationId))
    }
  }

  "SubmissionService on save" when {

    "everything works correctly" should {

      "return Either.Right with proper message" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.save(any())).thenReturn(Future.successful(true))

        val submissionResult =
          submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        submissionResult must equal(Right("Submission response saved"))
      }

      "call CustomsDeclarationsConnector and SubmissionRepository afterwards" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.save(any())).thenReturn(Future.successful(true))

        submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        val inOrder: InOrder = Mockito.inOrder(customsDeclarationsConnectorMock, submissionRepositoryMock)
        inOrder.verify(customsDeclarationsConnectorMock, times(1)).submitDeclaration(any(), any())(any())
        inOrder.verify(submissionRepositoryMock, times(1)).save(any())
      }

      "call CustomsDeclarationsConnector with EORI and xml provided" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )

        submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        verify(customsDeclarationsConnectorMock, times(1))
          .submitDeclaration(meq(eori), meq(submissionXml))(any())
      }

      "call SubmissionRepository with correctly built Submission (no MRN present)" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.save(any())).thenReturn(Future.successful(true))

        submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        val submissionCaptor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
        verify(submissionRepositoryMock, times(1)).save(submissionCaptor.capture())

        submissionCaptor.getValue.uuid mustNot be(empty)
        submissionCaptor.getValue.eori must equal(eori)
        submissionCaptor.getValue.lrn must equal(lrn)
        submissionCaptor.getValue.mrn mustNot be(defined)
        submissionCaptor.getValue.ducr must be(defined)
        submissionCaptor.getValue.ducr.get must equal(ducr)
        submissionCaptor.getValue.actions mustNot be(empty)
        submissionCaptor.getValue.actions.head.requestType must equal(SubmissionRequest)
        submissionCaptor.getValue.actions.head.conversationId must equal(conversationId)
      }
    }

    "CustomsDeclarationsConnector on submitDeclaration returns status other than ACCEPTED" should {

      "return Either.Left with proper message" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = BAD_REQUEST, conversationId = None)))

        val submissionResult =
          submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        submissionResult must equal(Left("Non Accepted status returned by Customs Declarations Service"))
      }

      "not call SubmissionRepository" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = BAD_REQUEST, conversationId = None)))

        submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        verifyZeroInteractions(submissionRepositoryMock)
      }
    }

    "SubmissionRepository returns false on save" should {

      "return Either.Left with proper message" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.save(any())).thenReturn(Future.successful(false))

        val submissionResult =
          submissionService.save(eori, validatedHeadersSubmissionRequest)(submissionXml).futureValue

        submissionResult must equal(Left("Failed saving submission"))
      }
    }
  }

  "SubmissionService on cancelDeclaration" when {

    "everything works correctly" should {

      "return Either.Right with proper message" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any()))
          .thenReturn(Future.successful(Some(cancelledSubmission)))

        val cancellationResult = submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        cancellationResult must equal(Right(CancellationRequested))
      }

      "call CustomsDeclarationsConnector and SubmissionRepository twice afterwards" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any()))
          .thenReturn(Future.successful(Some(cancelledSubmission)))

        submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        val inOrder: InOrder = Mockito.inOrder(customsDeclarationsConnectorMock, submissionRepositoryMock)
        inOrder.verify(customsDeclarationsConnectorMock, times(1)).submitCancellation(any(), any())(any())
        inOrder.verify(submissionRepositoryMock, times(1)).findSubmissionByMrn(any())
        inOrder.verify(submissionRepositoryMock, times(1)).addAction(any(), any())
      }

      "call CustomsDeclarationsConnector with XML provided" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any()))
          .thenReturn(Future.successful(Some(cancelledSubmission)))

        submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        verify(customsDeclarationsConnectorMock, times(1)).submitCancellation(meq(eori), meq(cancellationXml))(any())
      }

      "call SubmissionRepository.findSubmissionByMrn with MRN provided" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any()))
          .thenReturn(Future.successful(Some(cancelledSubmission)))

        submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        verify(submissionRepositoryMock, times(1)).findSubmissionByMrn(meq(mrn))
      }

      "call SubmissionRepository.addAction with MRN provided and correctly built Action" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any()))
          .thenReturn(Future.successful(Some(cancelledSubmission)))

        submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        val actionCaptor: ArgumentCaptor[Action] = ArgumentCaptor.forClass(classOf[Action])
        verify(submissionRepositoryMock, times(1)).addAction(meq(mrn), actionCaptor.capture())
        val actionAdded = actionCaptor.getValue
        actionAdded.requestType must equal(CancellationRequest)
        actionAdded.conversationId must equal(conversationId)
      }
    }

    "CustomsDeclarationsConnector on submitDeclaration returns status other than ACCEPTED" should {

      "return Either.Left with proper message" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = BAD_REQUEST, conversationId = None)))

        val cancellationResult = submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        cancellationResult must equal(Left("Non Accepted status returned by Customs Declarations Service"))

      }

      "not call SubmissionRepository" in new Test {
        when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(status = BAD_REQUEST, conversationId = None)))

        val cancellationResult = submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        verifyZeroInteractions(submissionRepositoryMock)
      }
    }

    "SubmissionRepository on findSubmissionByMrn returns empty Option" should {

      "return MissingDeclaration cancellation status" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))

        val cancellationResult = submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        cancellationResult must equal(Right(MissingDeclaration))
      }

      "not call SubmissionRepository.addAction" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))

        submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        verify(submissionRepositoryMock, never()).addAction(any(), any())
      }
    }

    "SubmissionRepository on findSubmissionByMrn returns Submission that has been already cancelled" should {

      "return CancellationRequestExists cancellation status" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any()))
          .thenReturn(Future.successful(Some(cancelledSubmission)))

        val cancellationResult = submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        cancellationResult must equal(Right(CancellationRequestExists))
      }

      "not call SubmissionRepository.addAction" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))

        submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        verify(submissionRepositoryMock, never()).addAction(any(), any())
      }
    }

    "SubmissionRepository on addAction returns None" should {

      "return MissingDeclaration cancellation status" in new Test {
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(
          Future.successful(CustomsDeclarationsResponse(status = ACCEPTED, conversationId = Some(conversationId)))
        )
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any())).thenReturn(Future.successful(None))

        val cancellationResult = submissionService.cancelDeclaration(eori, mrn)(cancellationXml).futureValue

        cancellationResult must equal(Right(MissingDeclaration))
      }
    }
  }

}

object SubmissionServiceSpec {
  val validatedHeadersSubmissionRequest: ValidatedHeadersSubmissionRequest =
    ValidatedHeadersSubmissionRequest(LocalReferenceNumber(lrn), Some(ducr))

  val submissionXml: NodeSeq = <submissionXml></submissionXml>
  val cancellationXml: NodeSeq = <cancellationXml></cancellationXml>
}
