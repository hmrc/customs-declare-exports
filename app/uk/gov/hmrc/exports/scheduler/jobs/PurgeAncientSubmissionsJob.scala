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

package uk.gov.hmrc.exports.scheduler.jobs

import org.mongodb.scala.bson.BsonDocument
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.repositories.{JobRunRepository, PurgeSubmissionsTransactionalOps, SubmissionRepository}
import uk.gov.hmrc.exports.util.TimeUtils

import java.time.{LocalTime, ZonedDateTime}
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeAncientSubmissionsJob @Inject() (
  appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  transactionalOps: PurgeSubmissionsTransactionalOps,
  jobRunRepository: JobRunRepository
)(implicit @Named("backgroundTasksExecutionContext") ec: ExecutionContext)
    extends ScheduledJob with Logging {

  override def firstRunTime: Option[LocalTime] = Some(appConfig.purgeAncientSubmissions.elapseTime)
  override def interval: FiniteDuration = appConfig.purgeAncientSubmissions.interval

  def expiryDate: ZonedDateTime = {
    val days = 180L
    val oneMilliSec = 1000000
    TimeUtils.now().minusDays(days).withNano(oneMilliSec)
  }

  override def execute(): Future[Unit] =
    jobRunRepository.isFirstRunInTheDay(name).flatMap {
      case true =>
        logger.info(s"Starting PurgeAncientSubmissionsJob. Removing Submissions having 'enhancedStatusLastUpdated' older than $expiryDate")
        submissionRepository.findAll(filter) flatMap { submissions =>
          transactionalOps.removeSubmissionAndNotifications(submissions) map { removed =>
            logger.info(s"Finishing PurgeAncientSubmissionsJob - ${removed.sum} records removed linked to ancient submissions")
          }
        }

      case false => Future.successful(())
    }

  private def filter = {
    val expiryDateAsString = Json.toJson(expiryDate).toString
    val statusesToRemove =
      List(CANCELLED, DECLARATION_HANDLED_EXTERNALLY, ERRORS, EXPIRED_NO_ARRIVAL, EXPIRED_NO_DEPARTURE, GOODS_HAVE_EXITED, WITHDRAWN)
    val jsonString =
      s"""
         |{
         |  "latestEnhancedStatus": { "$$in": [ ${statusesToRemove.map(s => s""""$s"""").mkString(",")} ] },
         |  "enhancedStatusLastUpdated": { "$$lte": ${expiryDateAsString} }
         |}""".stripMargin
    BsonDocument(jsonString)
  }
}
