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

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.models.{DeclarationSearch, DeclarationSort, Page, Paginated}
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService, WcoSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class DeclarationServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private val declarationRepository = mock[DeclarationRepository]
  private val wcoSubmissionService = mock[WcoSubmissionService]
  private val submissionService = mock[SubmissionService]
  private val service = new DeclarationService(declarationRepository, wcoSubmissionService, submissionService)
  private val hc = mock[HeaderCarrier]
  private val ec = Implicits.global

  "Create" should {
    "delegate to the repository" when {
      "declaration status is COMPLETE" in {
        val submission = mock[Submission]
        val declaration = mock[ExportsDeclaration]
        val persistedDeclaration = mock[ExportsDeclaration]
        given(declaration.status).willReturn(DeclarationStatus.COMPLETE)
        given(declarationRepository.create(declaration)).willReturn(Future.successful(persistedDeclaration))
        given(wcoSubmissionService.submit(declaration)(hc, ec)).willReturn(Future.successful(submission))
        given(submissionService.create(submission)).willReturn(Future.successful(submission))

        service.create(declaration)(hc, ec).futureValue mustBe persistedDeclaration

        verify(declarationRepository).create(declaration)
        verify(wcoSubmissionService).submit(declaration)(hc, ec)
        verify(submissionService).create(submission)
      }

      "declaration status is not COMPLETE" in {
        val declaration = mock[ExportsDeclaration]
        val persistedDeclaration = mock[ExportsDeclaration]
        given(declaration.status).willReturn(DeclarationStatus.DRAFT)
        given(declarationRepository.create(declaration)).willReturn(Future.successful(persistedDeclaration))

        service.create(declaration)(hc, ec).futureValue mustBe persistedDeclaration

        verify(declarationRepository).create(declaration)
        verifyZeroInteractions(wcoSubmissionService)
        verifyZeroInteractions(submissionService)
      }
    }
  }

  "Update" should {
    "delegate to the repository" when {
      "declaration status is COMPLETE" in {
        val submission = mock[Submission]
        val declaration = mock[ExportsDeclaration]
        val persistedDeclaration = mock[ExportsDeclaration]
        given(declaration.status).willReturn(DeclarationStatus.COMPLETE)
        given(declarationRepository.update(declaration)).willReturn(Future.successful(Some(persistedDeclaration)))
        given(wcoSubmissionService.submit(declaration)(hc, ec)).willReturn(Future.successful(submission))
        given(submissionService.create(submission)).willReturn(Future.successful(submission))

        service.update(declaration)(hc, ec).futureValue mustBe Some(persistedDeclaration)

        verify(declarationRepository).update(declaration)
        verify(wcoSubmissionService).submit(declaration)(hc, ec)
        verify(submissionService).create(submission)
      }

      "declaration status is not COMPLETE" in {
        val declaration = mock[ExportsDeclaration]
        val persistedDeclaration = mock[ExportsDeclaration]
        given(declaration.status).willReturn(DeclarationStatus.DRAFT)
        given(declarationRepository.update(declaration)).willReturn(Future.successful(Some(persistedDeclaration)))

        service.update(declaration)(hc, ec).futureValue mustBe Some(persistedDeclaration)

        verify(declarationRepository).update(declaration)
        verifyZeroInteractions(wcoSubmissionService)
        verifyZeroInteractions(submissionService)
      }
    }
  }

  "Delete" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      given(declarationRepository.delete(any[ExportsDeclaration])).willReturn(Future.successful((): Unit))

      service.deleteOne(declaration).futureValue

      verify(declarationRepository).delete(declaration)
    }
  }

  "Find by EORI" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      val search = mock[DeclarationSearch]
      val page = mock[Page]
      val sort = mock[DeclarationSort]
      given(declarationRepository.find(search, page, sort)).willReturn(Future.successful(Paginated(declaration)))

      service.find(search, page, sort).futureValue mustBe Paginated(declaration)
    }
  }

  "Find by ID & EORI" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      given(declarationRepository.find("id", "eori")).willReturn(Future.successful(Some(declaration)))

      service.findOne("id", "eori").futureValue mustBe Some(declaration)
    }
  }

}
