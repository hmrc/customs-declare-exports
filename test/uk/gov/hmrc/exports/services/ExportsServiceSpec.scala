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

package uk.gov.hmrc.exports.services

import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{times, verify, verifyZeroInteractions, when}
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status._
import play.api.mvc.Result
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, NodeSeq}

class ExportsServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait SetUp {
    val mockDeclarationConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
    val mockSubmissionRepo: SubmissionRepository = mock[SubmissionRepository]
    val testObj = new ExportsService(mockDeclarationConnector, mockSubmissionRepo)
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  }

  "ExportService" should {
    "handle submission" should {
      "call Connector, persist submission and return conversationId" in new SetUp() {
        val eori = ""
        val lrn = "LRN123456"
        val ducr = "DUCR456456"
        val conversationId = "123456789"
        val xmlNode: Elem = <someXml></someXml>
        when(
          mockDeclarationConnector
            .submitDeclaration(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(CustomsDeclarationsResponse(ACCEPTED, Some(conversationId))))
        when(mockSubmissionRepo.save(any[Submission])).thenReturn(Future.successful(true))
        val result: Result = await(testObj.handleSubmission(eori, ducr, lrn, xmlNode))

        status(result) shouldBe ACCEPTED

        verify(mockDeclarationConnector, times(1))
          .submitDeclaration(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        verify(mockSubmissionRepo, times(1)).save(any[Submission])
      }

      "return internal server error when no conversation ID is returned from the connector" in new SetUp() {
        val eori = ""
        val lrn = "LRN123456"
        val ducr = "DUCR456456"
        val conversationId = "123456789"
        val xmlNode: Elem = <someXml></someXml>
        when(
          mockDeclarationConnector
            .submitDeclaration(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(CustomsDeclarationsResponse(ACCEPTED, None)))

        when(mockSubmissionRepo.save(any[Submission])).thenReturn(Future.successful(false))
        val result: Result = await(testObj.handleSubmission(eori, ducr, lrn, xmlNode))

        status(result) shouldBe INTERNAL_SERVER_ERROR

        verify(mockDeclarationConnector, times(1))
          .submitDeclaration(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        verifyZeroInteractions(mockSubmissionRepo)
      }

      "call Connector, and return internal server Error when persist submission fails" in new SetUp() {
        val eori = ""
        val lrn = "LRN123456"
        val ducr = "DUCR456456"
        val conversationId = "123456789"
        val xmlNode: Elem = <someXml></someXml>
        when(
          mockDeclarationConnector
            .submitDeclaration(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(CustomsDeclarationsResponse(ACCEPTED, Some(conversationId))))

        when(mockSubmissionRepo.save(any[Submission])).thenReturn(Future.successful(false))
        val result: Result = await(testObj.handleSubmission(eori, ducr, lrn, xmlNode))

        status(result) shouldBe INTERNAL_SERVER_ERROR

        verify(mockDeclarationConnector, times(1))
          .submitDeclaration(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        verify(mockSubmissionRepo, times(1)).save(any[Submission])
      }
    }

    "handle Cancellation" should {
      "call Connector, persist cancellation and return conversationId" in new SetUp() {
        val eori = "GB1767676678"
        val mrn = "DUCR456456"
        val conversationId = "123456789"
        val xmlNode: Elem = <someXml></someXml>
        when(
          mockDeclarationConnector
            .submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(CustomsDeclarationsResponse(ACCEPTED, Some(conversationId))))

        when(mockSubmissionRepo.cancelDeclaration(any[String], any[String]))
          .thenReturn(Future.successful(CancellationRequested))
        val result = testObj.handleCancellation(eori, mrn, xmlNode).futureValue

        result shouldBe Right(CancellationRequested)

        verify(mockDeclarationConnector, times(1))
          .submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        verify(mockSubmissionRepo, times(1)).cancelDeclaration(any[String], any[String])
      }

      "return Internal Server error when connector fails" in new SetUp() {
        val eori = "GB1767676678"
        val mrn = "DUCR456456"
        val conversationId = "123456789"
        val xmlNode: Elem = <someXml></someXml>
        when(
          mockDeclarationConnector
            .submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(CustomsDeclarationsResponse(BAD_REQUEST, None)))

        val result = testObj.handleCancellation(eori, mrn, xmlNode).futureValue

        status(result.left.get) shouldBe INTERNAL_SERVER_ERROR

        verify(mockDeclarationConnector, times(1))
          .submitCancellation(any[String], any[NodeSeq])(any[HeaderCarrier], any[ExecutionContext])
        verifyZeroInteractions(mockSubmissionRepo)
      }

    }

  }

}
