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
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.{QueryOpts, ReadConcern, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.{DeclarationSearch, Page, Paginated}
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

  def find(id: String, eori: String): Future[Option[ExportsDeclaration]] =
    super.find("id" -> id, "eori" -> eori).map(_.headOption)

  def find(search: DeclarationSearch, pagination: Page): Future[Paginated[ExportsDeclaration]] = {
    val query = Json.toJson(search).as[JsObject]
    for {
      results <- collection
        .find(query, projection = None)(ImplicitBSONHandlers.JsObjectDocumentWriter, ImplicitBSONHandlers.JsObjectDocumentWriter)
        .options(QueryOpts(skipN = (pagination.index -1) * pagination.size, batchSizeN = pagination.size))
        .cursor[ExportsDeclaration](ReadPreference.primaryPreferred)
        .collect(maxDocs = pagination.size, FailOnError[List[ExportsDeclaration]]())
        .map(_.toSeq)
      total <- collection.count(Some(query), limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
    } yield Paginated(results = results, page = pagination, total = total)
  }

  def create(declaration: ExportsDeclaration): Future[ExportsDeclaration] =
    super.insert(declaration).map(_ => declaration)

  def update(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] = super.findAndUpdate(
    Json.obj("id" -> declaration.id, "eori" -> declaration.eori),
    Json.toJson(declaration).as[JsObject],
    fetchNewObject = true,
    upsert = false
  ).map(_.value.map(_.as[ExportsDeclaration]))

  def delete(declaration: ExportsDeclaration): Future[Unit] =
    super
      .remove("id" -> declaration.id, "eori" -> declaration.eori)
      .map(_ => Unit)

}
