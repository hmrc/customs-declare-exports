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

package uk.gov.hmrc.exports.services

import play.api.libs.json.Json
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.{AMENDMENT_DRAFT, COMPLETE, DRAFT}
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.DeclarationService.{CREATED, FOUND}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationService @Inject() (declarationRepository: DeclarationRepository, submissionRepository: SubmissionRepository) {

  def create(declaration: ExportsDeclaration): Future[ExportsDeclaration] =
    declarationRepository.create(declaration)

  def findOrCreateDraftForAmend(eori: Eori, submissionId: String)(implicit ec: ExecutionContext): Future[Option[(Boolean, String)]] = {
    def findOrCreateDraftFromSubmission(submission: Submission): Future[Option[(Boolean, String)]] = {
      val filter = Json.obj(
        "eori" -> eori,
        "declarationMeta.parentDeclarationId" -> submission.latestDecId,
        "declarationMeta.status" -> AMENDMENT_DRAFT.toString
      )
      declarationRepository.findOne(filter).flatMap {
        case Some(declaration) => Future.successful(Some(FOUND -> declaration.id))

        case _ =>
          for {
            declaration <- declarationRepository.get(Json.obj("eori" -> eori, "id" -> submission.latestDecId))
            result <- declarationRepository
              .create(
                declaration.copy(
                  id = UUID.randomUUID.toString,
                  declarationMeta = declaration.declarationMeta.copy(
                    parentDeclarationId = submission.latestDecId,
                    parentDeclarationEnhancedStatus = submission.latestEnhancedStatus,
                    status = AMENDMENT_DRAFT
                  )
                )
              )
              .map(declaration => Some(CREATED -> declaration.id))
          } yield result
      }
    }

    submissionRepository.findById(eori.value, submissionId).flatMap {
      case Some(submission) => findOrCreateDraftFromSubmission(submission)
      case _                => Future.successful(None)
    }
  }

  def findOrCreateDraftFromParent(eori: Eori, parentId: String)(implicit ec: ExecutionContext): Future[(Boolean, String)] = {
    val filter = Json.obj("eori" -> eori, "declarationMeta.parentDeclarationId" -> parentId, "declarationMeta.status" -> DRAFT.toString)
    declarationRepository.findOne(filter).flatMap {
      case Some(declaration) => Future.successful((FOUND, declaration.id))

      case _ =>
        for {
          declaration <- declarationRepository.get(Json.obj("eori" -> eori, "id" -> parentId))
          submission <- submissionRepository.findById(eori.value, parentId)
          result <- declarationRepository
            .create(
              declaration.copy(
                id = UUID.randomUUID.toString,
                declarationMeta = declaration.declarationMeta.copy(
                  parentDeclarationId = Some(parentId),
                  parentDeclarationEnhancedStatus = submission.flatMap(_.latestEnhancedStatus),
                  status = DRAFT
                )
              )
            )
            .map(declaration => (CREATED, declaration.id))
        } yield result
    }
  }

  def find(search: DeclarationSearch, pagination: Page, sort: DeclarationSort): Future[Paginated[ExportsDeclaration]] =
    declarationRepository.find(search, pagination, sort)

  def findOne(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    declarationRepository.findOne(eori, id)

  def deleteOne(declaration: ExportsDeclaration): Future[Boolean] =
    declarationRepository.removeOne(Json.obj("id" -> declaration.id, "eori" -> declaration.eori))

  def update(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
    declarationRepository.findOneAndReplace(
      Json.obj("id" -> declaration.id, "eori" -> declaration.eori, "status" -> Json.obj("$ne" -> COMPLETE.toString)),
      declaration,
      false
    )
}

object DeclarationService {

  val CREATED = true
  val FOUND = false
}
