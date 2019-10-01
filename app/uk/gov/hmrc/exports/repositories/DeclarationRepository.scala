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

import java.time._

import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{QueryOpts, ReadConcern, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

class DeclarationRepository @Inject()(mc: ReactiveMongoComponent, appConfig: AppConfig, metrics: Metrics)(
  implicit ec: ExecutionContext
) extends ReactiveRepository[ExportsDeclaration, BSONObjectID](
      "declarations",
      mc.mongoConnector.db,
      ExportsDeclaration.Mongo.format,
      objectIdFormats
    ) {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("eori" -> IndexType.Ascending, "id" -> IndexType.Ascending), Some("eoriAndIdIdx"), unique = true),
    Index(
      Seq("eori" -> IndexType.Ascending, "updateDateTime" -> IndexType.Ascending, "status" -> IndexType.Ascending),
      Some("eoriAndUpdateTimeAndStatusIdx")
    ),
    Index(
      Seq("eori" -> IndexType.Ascending, "createDateTime" -> IndexType.Ascending, "status" -> IndexType.Ascending),
      Some("eoriAndCreateTimeAndStatusIdx")
    ),
    Index(Seq("updatedDateTime" -> IndexType.Descending, "status" -> IndexType.Ascending), Some("statusAndUpdateIdx"))
  )

  private val findDeclarationTimer = metrics.defaultRegistry.timer("mongo.declaration.find")

  def find(id: String, eori: Eori): Future[Option[ExportsDeclaration]] = {
    val findStopwatch = findDeclarationTimer.time()
    super.find("id" -> id, "eori" -> eori.value).map(_.headOption).andThen {
      case _ => findStopwatch.stop()
    }
  }

  private val getDeclarationsTimer = metrics.defaultRegistry.timer("mongo.declarations.find")

  def find(
    search: DeclarationSearch,
    pagination: Page,
    sort: DeclarationSort
  ): Future[Paginated[ExportsDeclaration]] = {
    val query = Json.toJson(search).as[JsObject]
    val findStopwatch = getDeclarationsTimer.time()
    for {
      results <- collection
        .find(query, projection = None)(
          ImplicitBSONHandlers.JsObjectDocumentWriter,
          ImplicitBSONHandlers.JsObjectDocumentWriter
        )
        .sort(Json.obj(sort.by.toString -> sort.direction.id))
        .options(QueryOpts(skipN = (pagination.index - 1) * pagination.size, batchSizeN = pagination.size))
        .cursor[ExportsDeclaration](ReadPreference.primaryPreferred)
        .collect(maxDocs = pagination.size, FailOnError[List[ExportsDeclaration]]())
        .map(_.toSeq)
      total <- collection.count(Some(query), limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
    } yield {
      findStopwatch.stop()
      Paginated(results = results, page = pagination, total = total)
    }
  }

  def create(declaration: ExportsDeclaration): Future[ExportsDeclaration] =
    super.insert(declaration).map(_ => declaration)

  private val updateDeclarationTimer = metrics.defaultRegistry.timer("mongo.declaration.update")

  def update(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] = {
    val updateStopwatch = updateDeclarationTimer.time()
    super
      .findAndUpdate(
        Json.obj("id" -> declaration.id, "eori" -> declaration.eori),
        Json.toJson(declaration).as[JsObject],
        fetchNewObject = true,
        upsert = false
      )
      .map(_.value.map(_.as[ExportsDeclaration]))
      .andThen {
        case _ => updateStopwatch.stop()
      }
  }

  def delete(declaration: ExportsDeclaration): Future[Unit] =
    super
      .remove("id" -> declaration.id, "eori" -> declaration.eori)
      .map(_ => Unit)

  def deleteExpiredDraft(expiryDate: Instant): Future[Int] = {
    import ExportsDeclaration.Mongo.formatInstant
    super
      .remove("status" -> DeclarationStatus.DRAFT, "updatedDateTime" -> Json.obj("$lte" -> expiryDate))
      .map(res => res.n)
  }

}
