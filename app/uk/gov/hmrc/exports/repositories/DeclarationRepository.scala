/*
 * Copyright 2022 HM Revenue & Customs
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

import com.mongodb.client.model.Indexes.{ascending, compoundIndex, descending}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.Json
import repositories.RepositoryOps
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timers
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class DeclarationRepository @Inject()(appConfig: AppConfig, mongoComponent: MongoComponent, metrics: ExportsMetrics)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ExportsDeclaration](
      mongoComponent = mongoComponent,
      collectionName = "declarations",
      domainFormat = ExportsDeclaration.Mongo.format,
      indexes = DeclarationRepository.indexes,
      replaceIndexes = appConfig.replaceIndexesOfDeclarationRepository
    ) with RepositoryOps[ExportsDeclaration] {

  override def classTag: ClassTag[ExportsDeclaration] = implicitly[ClassTag[ExportsDeclaration]]
  override val executionContext = ec

  def deleteExpiredDraft(expiryDate: Instant): Future[Long] = {
    import ExportsDeclaration.Mongo.formatInstant
    removeEvery(Json.obj("status" -> DeclarationStatus.DRAFT.toString, "updatedDateTime" -> Json.obj("$lte" -> expiryDate)))
  }

  def find(search: DeclarationSearch, page: Page, sort: DeclarationSort): Future[Paginated[ExportsDeclaration]] = {
    val filter = BsonDocument(Json.toJson(search).toString)

    metrics.timeAsyncCall(Timers.declarationFindAllTimer) {
      for {
        results <- collection
          .find(filter)
          .sort(BsonDocument(Json.obj(sort.by.toString -> sort.direction.id).toString))
          .skip((page.index - 1) * page.size)
          .batchSize(page.size)
          .limit(page.size)
          .toFuture
        total <- collection.countDocuments(filter).toFuture
      } yield {
        Paginated(currentPageElements = results, page = page, total = total)
      }
    }
  }

  def findOne(id: String, eori: Eori): Future[Option[ExportsDeclaration]] =
    metrics.timeAsyncCall(Timers.declarationFindSingleTimer) {
      findOne(Json.obj("id" -> id, "eori" -> eori))
    }

  def markStatusAsComplete(id: String, eori: Eori): Future[Option[ExportsDeclaration]] =
    collection
      .findOneAndUpdate(
        filter = BsonDocument(Json.obj("id" -> id, "eori" -> eori.value).toString),
        update = set("status", DeclarationStatus.COMPLETE.toString)
      )
      .toFutureOption

  def revertStatusToDraft(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
    findOneAndUpdate(Json.obj("id" -> declaration.id, "eori" -> declaration.eori), Json.obj("status" -> DeclarationStatus.DRAFT.toString))
}

object DeclarationRepository {

  val indexes: Seq[IndexModel] = List(
    IndexModel(ascending("eori", "id"), IndexOptions().name("eoriAndIdIdx").unique(true)),
    IndexModel(ascending("eori", "updatedDateTime", "status"), IndexOptions().name("eoriAndUpdateTimeAndStatusIdx")),
    IndexModel(ascending("eori", "createdDateTime", "status"), IndexOptions().name("eoriAndCreateTimeAndStatusIdx")),
    IndexModel(compoundIndex(descending("updatedDateTime"), ascending("status")), IndexOptions().name("statusAndUpdateIdx"))
  )
}
