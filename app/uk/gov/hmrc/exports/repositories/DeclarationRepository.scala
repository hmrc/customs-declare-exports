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

package uk.gov.hmrc.exports.repositories

import com.mongodb.client.model.Indexes.{ascending, compoundIndex, descending}
import com.mongodb.client.model.ReturnDocument
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Updates.{combine, set, unset}
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions}
import play.api.libs.json.Json
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timers
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.{DeclarationMeta, DeclarationStatus, ExportsDeclaration, TransportCountry}
import uk.gov.hmrc.exports.repositories.DeclarationRepository.meta
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class DeclarationRepository @Inject() (
  appConfig: AppConfig,
  mongoComponent: MongoComponent,
  metrics: ExportsMetrics,
  countriesService: CountriesService
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ExportsDeclaration](
      mongoComponent = mongoComponent,
      collectionName = "declarations",
      domainFormat = ExportsDeclaration.Mongo.format,
      indexes = DeclarationRepository.indexes,
      replaceIndexes = appConfig.replaceIndexesOfDeclarationRepository
    ) with RepositoryOps[ExportsDeclaration] {

  override def classTag: ClassTag[ExportsDeclaration] = implicitly[ClassTag[ExportsDeclaration]]
  override val executionContext: ExecutionContext = ec

  def deleteExpiredDraft(expiryDate: Instant): Future[Long] = {
    import DeclarationMeta.Mongo.instantFormat
    removeEvery(Json.obj(s"$meta.status" -> DeclarationStatus.DRAFT.toString, s"$meta.updatedDateTime" -> Json.obj("$lte" -> expiryDate)))
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
      } yield Paginated(currentPageElements = results.map(convertTransportCountryToCode), page = page, total = total)
    }
  }

  def markStatusAsComplete(eori: Eori, id: String, submissionId: String): Future[Option[ExportsDeclaration]] =
    collection
      .findOneAndUpdate(
        filter = BsonDocument(Json.obj("eori" -> eori.value, "id" -> id).toString),
        update = combine(set(s"$meta.status", DeclarationStatus.COMPLETE.toString), set(s"$meta.associatedSubmissionId", submissionId)),
        options = FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.BEFORE)
      )
      .toFutureOption()

  def findOne(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    metrics
      .timeAsyncCall(Timers.declarationFindSingleTimer) {
        findOne(Json.obj("eori" -> eori, "id" -> id))
      }
      .map(_.map(convertTransportCountryToCode))

  def revertStatusToDraft(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
    findOneAndUpdate(
      filter = BsonDocument(Json.obj("eori" -> declaration.eori, "id" -> declaration.id).toString),
      update = combine(set(s"$meta.status", DeclarationStatus.DRAFT.toString), unset(s"$meta.associatedSubmissionId"))
    )

  def revertStatusToAmendmentDraft(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
    findOneAndUpdate(
      filter = BsonDocument(Json.obj("eori" -> declaration.eori, "id" -> declaration.id).toString),
      update = set(s"$meta.status", DeclarationStatus.AMENDMENT_DRAFT.toString)
    )

  // TODO Remove once migration has been performed for TransportCountry field in CEDS-5606
  private def convertTransportCountryToCode(declaration: ExportsDeclaration): ExportsDeclaration = {
    val potentialNameOrCode = declaration.transport.transportCrossingTheBorderNationality.flatMap(_.countryCode)
    val maybeCode = potentialNameOrCode.flatMap(countriesService.getCountryCode)
    maybeCode.fold(declaration)(code =>
      declaration.copy(transport = declaration.transport.copy(transportCrossingTheBorderNationality = Some(TransportCountry(Some(code)))))
    )
  }
}

object DeclarationRepository {

  val meta = "declarationMeta"

  val indexes: Seq[IndexModel] = List(
    // Used for getting a declaration owned by a given user
    IndexModel(ascending("eori", "id"), IndexOptions().name("eoriAndIdIdx").unique(true)),
    // Used for getting a draft declaration that was copy of another owned by a given user
    IndexModel(ascending("eori", s"$meta.parentDeclarationId", s"$meta.status"), IndexOptions().name("eoriAndParentDecIdIdx")),
    // ?? Used for dashboard ??
    IndexModel(ascending("eori", s"$meta.updatedDateTime", s"$meta.status"), IndexOptions().name("eoriAndUpdateTimeAndStatusIdx")),
    // Used for pulling draft declarations for user in date order
    IndexModel(ascending("eori", s"$meta.createdDateTime", s"$meta.status"), IndexOptions().name("eoriAndCreateTimeAndStatusIdx")),
    // Use for deleting draft decs older than X
    IndexModel(compoundIndex(descending(s"$meta.updatedDateTime"), ascending(s"$meta.status")), IndexOptions().name("statusAndUpdateIdx"))
  )
}
