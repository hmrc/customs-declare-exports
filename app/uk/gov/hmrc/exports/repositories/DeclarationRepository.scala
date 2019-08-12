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

package uk.gov.hmrc.exports.repositories

import javax.inject.Inject
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

class DeclarationRepository @Inject()(mc: ReactiveMongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends ReactiveRepository[ExportsDeclaration, BSONObjectID](
      "declarations",
      mc.mongoConnector.db,
      ExportsDeclaration.Mongo.format,
      objectIdFormats
    ) {
  def update(submittedDeclaration: Any) = ???

  def find(id: String, eori: String): Future[Option[ExportsDeclaration]] =
    super.find("id" -> id, "eori" -> eori).map(_.headOption)

  def find(eori: String): Future[Seq[ExportsDeclaration]] = super.find("eori" -> eori).map(_.toSeq)

  def create(declaration: ExportsDeclaration): Future[ExportsDeclaration] =
    super.insert(declaration).map(_ => declaration)

  def delete(declaration: ExportsDeclaration): Future[Unit] =
    super
      .remove("id" -> declaration.id, "eori" -> declaration.eori)
      .map(_ => Unit)

}
