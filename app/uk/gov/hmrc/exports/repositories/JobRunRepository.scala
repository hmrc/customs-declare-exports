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

package uk.gov.hmrc.exports.repositories

import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import uk.gov.hmrc.exports.models.JobRun
import uk.gov.hmrc.exports.util.TimeUtils.{defaultTimeZone, instant}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.temporal.ChronoField.DAY_OF_MONTH
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class JobRunRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[JobRun](
      mongoComponent = mongoComponent,
      collectionName = "scheduledJobs",
      domainFormat = JobRun.format,
      indexes = JobRunRepository.indexes
    ) with RepositoryOps[JobRun] {

  override def classTag: ClassTag[JobRun] = implicitly[ClassTag[JobRun]]
  override val executionContext: ExecutionContext = ec

  def isFirstRunInTheDay(job: String): Future[Boolean] = {
    val replacement = JobRun(job, instant())
    findOneAndReplace("job", job, replacement, createIfNotExists = true, returnPreviousDocument = true).map {
      _.fold(true)(_.lastRun.atZone(defaultTimeZone).get(DAY_OF_MONTH) != replacement.lastRun.atZone(defaultTimeZone).get(DAY_OF_MONTH))
    }
  }
}

private object JobRunRepository {

  val indexes: Seq[IndexModel] = List(IndexModel(ascending("job"), IndexOptions().name("jobs").unique(true)))
}
