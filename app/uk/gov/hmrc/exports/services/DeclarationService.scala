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

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.{AMENDMENT_DRAFT, COMPLETE, DRAFT}
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.services.DeclarationService.{CREATED, FOUND}

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationService @Inject() (declarationRepository: DeclarationRepository)(implicit ec: ExecutionContext) extends Logging {

  def create(declaration: ExportsDeclaration): Future[ExportsDeclaration] =
    declarationRepository.create(declaration)

  def findOrCreateDraftFromParent(
    eori: Eori,
    parentId: String,
    enhancedStatus: EnhancedStatus,
    isAmendment: Boolean
  ): Future[Option[(Boolean, String)]] = {

    val filter = Json.obj(
      "eori" -> eori,
      "declarationMeta.parentDeclarationId" -> parentId,
      "declarationMeta.status" -> (if (isAmendment) AMENDMENT_DRAFT else DRAFT).toString
    )

    declarationRepository.findOne(filter).flatMap {
      case Some(declaration) => Future.successful(Some(FOUND -> declaration.id))

      case _ =>
        declarationRepository.findOne(Json.obj("eori" -> eori, "id" -> parentId)).flatMap {
          case Some(declaration) if declaration.declarationMeta.status == COMPLETE =>
            createDraft(declaration, parentId, enhancedStatus, isAmendment)

          case Some(declaration) if isAlreadyDraft(declaration, isAmendment) =>
            Future.successful(Some(FOUND -> declaration.id))

          case Some(declaration) =>
            val status = s"an unexpected status(${declaration.declarationMeta.status.toString})"
            logger.error(s"The declaration($parentId) to create a draft from was found but why it was in $status??")
            Future.successful(None)

          case _ =>
            logger.error(s"The declaration($parentId) to create a draft from was not found??")
            Future.successful(None)
        }
    }
  }

  private def createDraft(
    declaration: ExportsDeclaration,
    parentId: String,
    enhancedStatus: EnhancedStatus,
    isAmendment: Boolean
  ): Future[Some[(Boolean, String)]] = {
    val now = Instant.now
    val associatedSubmissionId = if (isAmendment) declaration.declarationMeta.associatedSubmissionId else None
    val declarationMeta = declaration.declarationMeta
      .copy(
        parentDeclarationId = Some(parentId),
        parentDeclarationEnhancedStatus = Some(enhancedStatus),
        status = if (isAmendment) AMENDMENT_DRAFT else DRAFT,
        createdDateTime = now,
        updatedDateTime = now,
        associatedSubmissionId = associatedSubmissionId
      )

    declarationRepository
      .create(declaration.copy(id = UUID.randomUUID.toString, declarationMeta = declarationMeta))
      .map(declaration => Some(CREATED -> declaration.id))
  }

  private def isAlreadyDraft(declaration: ExportsDeclaration, isAmendment: Boolean): Boolean =
    (declaration.declarationMeta.status, isAmendment) match {
      case (AMENDMENT_DRAFT, true) => true
      case (DRAFT, false)          => true
      case _                       => false
    }

  def fetchPage(search: DeclarationSearch, page: Page, sort: DeclarationSort): Future[(Seq[ExportsDeclaration], Long)] =
    declarationRepository.fetchPage(search, page, sort)

  def findDraftByParent(eori: Eori, parentId: String): Future[Option[ExportsDeclaration]] = {
    val filter = Json.obj(
      "eori" -> eori,
      "declarationMeta.parentDeclarationId" -> parentId,
      "declarationMeta.status" -> Json.obj("$in" -> Json.arr(AMENDMENT_DRAFT.toString, DRAFT.toString)),
      "declarationMeta.parentDeclarationEnhancedStatus" -> EnhancedStatus.ERRORS.toString
    )
    declarationRepository.findOne(filter)
  }

  def findOne(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    declarationRepository.findOne(eori, id)

  def deleteOne(declaration: ExportsDeclaration): Future[Boolean] =
    declarationRepository.removeOne(Json.obj("id" -> declaration.id, "eori" -> declaration.eori))

  def markCompleted(eori: Eori, id: String, submissionId: String): Future[Option[ExportsDeclaration]] =
    declarationRepository.markStatusAsComplete(eori, id, submissionId)

  def update(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
    declarationRepository.findOneAndReplace(
      Json.obj("id" -> declaration.id, "eori" -> declaration.eori, "status" -> Json.obj("$ne" -> COMPLETE.toString)),
      declaration,
      createIfNotExists = false,
      returnPreviousDocument = false
    )
}

object DeclarationService {

  val CREATED = true
  val FOUND = false
}
