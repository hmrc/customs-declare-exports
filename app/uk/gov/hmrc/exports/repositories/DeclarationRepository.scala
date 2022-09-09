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
import com.mongodb.client.model.ReturnDocument
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions}
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
class DeclarationRepository @Inject() (appConfig: AppConfig, mongoComponent: MongoComponent, metrics: ExportsMetrics)(implicit ec: ExecutionContext)
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
          .toFuture()
        total <- collection.countDocuments(filter).toFuture()
      } yield Paginated(currentPageElements = results, page = page, total = total)
    }
  }

  def markStatusAsComplete(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    collection
      .findOneAndUpdate(
        filter = BsonDocument(Json.obj("eori" -> eori.value, "id" -> id).toString),
        update = set("status", DeclarationStatus.COMPLETE.toString),
        options = FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.BEFORE)
      )
      .toFutureOption()

  def findOne(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    metrics.timeAsyncCall(Timers.declarationFindSingleTimer) {
      findOne(Json.obj("eori" -> eori, "id" -> id))
    }

  def revertStatusToDraft(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
    findOneAndUpdate(
      filter = BsonDocument(Json.obj("eori" -> declaration.eori, "id" -> declaration.id).toString),
      update = set("status", DeclarationStatus.DRAFT.toString)
    )
}

object DeclarationRepository {

  val indexes: Seq[IndexModel] = List(
    // Used for getting a declaration owned by a given user
    IndexModel(ascending("eori", "id"), IndexOptions().name("eoriAndIdIdx").unique(true)),
    // Used for getting a draft declaration that was copy of another owned by a given user
    IndexModel(ascending("eori", "parentDeclarationId", "status"), IndexOptions().name("eoriAndParentDecIdIdx")),
    // ?? Used for dashboard ??
    IndexModel(ascending("eori", "updatedDateTime", "status"), IndexOptions().name("eoriAndUpdateTimeAndStatusIdx")),
    // Used for pulling draft declarations for user in date order
    IndexModel(ascending("eori", "createdDateTime", "status"), IndexOptions().name("eoriAndCreateTimeAndStatusIdx")),
    // Use for deleting draft decs older than X
    IndexModel(compoundIndex(descending("updatedDateTime"), ascending("status")), IndexOptions().name("statusAndUpdateIdx"))
  )
}
