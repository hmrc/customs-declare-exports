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

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.{DeclarationSearch, Page, Paginated}
import uk.gov.hmrc.exports.repositories.DeclarationRepository

import scala.concurrent.Future

class DeclarationService @Inject()(declarationRepository: DeclarationRepository) {

  /*
   * For now this just delegates to the repository,
   * eventually it will judge based on a status whether the declaration is a draft, or for submitting.
   */
  def create(declaration: ExportsDeclaration): Future[ExportsDeclaration] = declarationRepository.create(declaration)

  def update(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] = declarationRepository.update(declaration)

  def find(search: DeclarationSearch, pagination: Page): Future[Paginated[ExportsDeclaration]] = declarationRepository.find(search, pagination)

  def findOne(id: String, eori: String): Future[Option[ExportsDeclaration]] = declarationRepository.find(id, eori)

  def deleteOne(declaration: ExportsDeclaration): Future[Unit] = declarationRepository.delete(declaration)

}
