/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.exports.base.{IntegrationTestSpec, MockMetrics}
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.services.DeclarationService.{CREATED, FOUND}

import scala.concurrent.ExecutionContext.global

class DeclarationServiceISpec extends IntegrationTestSpec with MockMetrics {

  private val declarationRepository = instanceOf[DeclarationRepository]

  private val declarationService = new DeclarationService(declarationRepository)(global)

  val eori = Eori("eori")
  val parentId = "parentId"

  override def beforeEach(): Unit = {
    declarationRepository.removeAll.futureValue
    super.beforeEach()
  }

  "DeclarationService.findOrCreateDraftFromParent" should {

    "return None" when {

      "a draft declaration with 'parentDeclarationId' equal to the given parentId was NOT found and" when {
        "a 'parent' declaration was NOT found too" in {
          val result = declarationService.findOrCreateDraftFromParent(eori, parentId, ERRORS, false)
          result.futureValue mustBe None
        }
      }

      "a declaration with id equal to the given parentId was found but hasn't 'COMPLETE' status" in {
        val declaration = aDeclaration(withEori(eori), withStatus(DRAFT))
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        val result = declarationService.findOrCreateDraftFromParent(eori, declaration.id, ERRORS, true)
        result.futureValue mustBe None
      }
    }

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to provided parentId was found" when {

      "a submitted (not amended yet) declaration is rejected" in {
        val declaration = aDeclaration(withEori(eori), withParentDeclarationId(parentId), withStatus(DRAFT))
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        val result = declarationService.findOrCreateDraftFromParent(eori, parentId, ERRORS, false)
        result.futureValue.value mustBe (FOUND, declaration.id)
      }

      "a rejected declaration has been amended" in {
        val declaration = aDeclaration(withEori(eori), withParentDeclarationId(parentId), withStatus(AMENDMENT_DRAFT))
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        val result = declarationService.findOrCreateDraftFromParent(eori, parentId, ERRORS, true)
        result.futureValue.value mustBe (FOUND, declaration.id)
      }
    }

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to provided parentId was created" when {

      "a submitted (not amended yet) declaration is rejected" in {
        val declaration = aDeclaration(withEori(eori))
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        val result = declarationService.findOrCreateDraftFromParent(eori, declaration.id, CLEARED, false).futureValue.value
        result._1 mustBe CREATED

        val meta = declarationRepository.get("id", result._2).futureValue.declarationMeta
        meta.parentDeclarationId.value mustBe declaration.id
        meta.parentDeclarationEnhancedStatus.value mustBe CLEARED
        meta.status mustBe DRAFT
        meta.associatedSubmissionId mustBe None
      }

      "a declaration has been amended" in {
        val submissionId = "123445667"
        val declaration = aDeclaration(withEori(eori), withAssociatedSubmissionId(Some(submissionId)))
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        val result = declarationService.findOrCreateDraftFromParent(eori, declaration.id, ERRORS, true).futureValue.value
        result._1 mustBe CREATED

        val meta = declarationRepository.get("id", result._2).futureValue.declarationMeta
        meta.parentDeclarationId.value mustBe declaration.id
        meta.parentDeclarationEnhancedStatus.value mustBe ERRORS
        meta.status mustBe AMENDMENT_DRAFT
        meta.associatedSubmissionId mustBe Some(submissionId)
      }
    }
  }

  "DeclarationService.findDraftByParent" should {
    "return None" when {
      "a draft declaration with 'parentDeclarationId' equal to the given parentId was NOT found" in {
        val result = declarationService.findDraftByParent(eori, parentId)
        result.futureValue mustBe None
      }

      "a draft declaration with 'parentDeclarationId' equal to the given parentId was found BUT " when {
        for (
          enhancedStaus <- Seq(
            ADDITIONAL_DOCUMENTS_REQUIRED,
            AMENDED,
            AWAITING_EXIT_RESULTS,
            CANCELLED,
            CLEARED,
            CUSTOMS_POSITION_DENIED,
            CUSTOMS_POSITION_GRANTED,
            DECLARATION_HANDLED_EXTERNALLY,
            EXPIRED_NO_ARRIVAL,
            EXPIRED_NO_DEPARTURE,
            GOODS_ARRIVED,
            GOODS_ARRIVED_MESSAGE,
            GOODS_HAVE_EXITED,
            QUERY_NOTIFICATION_MESSAGE,
            RECEIVED,
            RELEASED,
            UNDERGOING_PHYSICAL_CHECK,
            WITHDRAWN,
            PENDING,
            REQUESTED_CANCELLATION,
            ON_HOLD,
            UNKNOWN
          )
        )
          s"declaration's ParentDeclarationEnhancedStatus is not 'ERROR' but '$enhancedStaus'" in {
            val declaration = aDeclaration(withEori(eori), withParentDeclarationId(parentId), withParentDeclarationEnhancedStatus(enhancedStaus))
            declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

            val result = declarationService.findDraftByParent(eori, parentId)
            result.futureValue mustBe None
          }
      }

      "a draft declaration with 'parentDeclarationId' equal to the given parentId was found BUT " when {
        for (status <- Seq(INITIAL, COMPLETE))
          s"declaration's status is not 'DRAFT' or 'AMENDMENT_DRAFT' but '$status'" in {
            val declaration = aDeclaration(
              withEori(eori),
              withParentDeclarationId(parentId),
              withParentDeclarationEnhancedStatus(EnhancedStatus.ERRORS),
              withStatus(status)
            )
            declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

            val result = declarationService.findDraftByParent(eori, parentId)
            result.futureValue mustBe None
          }
      }
    }

    "return Some declaration" when {
      for (status <- Seq(DRAFT, AMENDMENT_DRAFT))
        // scalastyle:off
        s"a draft declaration with a matching 'parentDeclarationId' and a parentEnhanced status of 'ERROR' and parentDeclarationEnhancedStatus of '$status' existis" in {
          // scalastyle:on
          val declaration = aDeclaration(
            withEori(eori),
            withParentDeclarationId(parentId),
            withParentDeclarationEnhancedStatus(EnhancedStatus.ERRORS),
            withStatus(status)
          )
          declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

          val result = declarationService.findDraftByParent(eori, parentId)
          result.futureValue mustBe Some(declaration)
        }
    }
  }
}
