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

package uk.gov.hmrc.exports.scheduler.jobs

import org.mongodb.scala.model.Filters._
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.repositories._
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time._
import javax.inject.{Inject, Singleton}
import scala.collection.convert.DecorateAsScala
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeAncientSubmissionsJob @Inject() (
  val appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  transactionalOps: PurgeSubmissionsTransactionalOps
)(implicit ec: ExecutionContext)
    extends ScheduledJob with Logging with DecorateAsScala {

  override val name: String = "PurgeAncientSubmissions"

  override def interval: FiniteDuration = jobConfig.interval
  override def firstRunTime: Option[LocalTime] = Some(jobConfig.elapseTime)

  private val jobConfig = appConfig.purgeAncientSubmissions
  private val clock: Clock = appConfig.clock

  private val latestStatus = "latestEnhancedStatus"
  private val statusLastUpdated = "enhancedStatusLastUpdated"

  private val expiryDate = Codecs.toBson(ZonedDateTime.now(clock).minusDays(180))

  private val latestStatusLookup =
    in(latestStatus, List("GOODS_HAVE_EXITED", "DECLARATION_HANDLED_EXTERNALLY", "CANCELLED", "EXPIRED_NO_ARRIVAL", "ERRORS"): _*)

  private val olderThanDate = lte(statusLastUpdated, expiryDate)

  override def execute(): Future[Unit] =
    submissionRepository.findAll(and(olderThanDate, latestStatusLookup)) flatMap { submissions =>
      transactionalOps.removeSubmissionAndNotifications(submissions) map { removed =>
        logger.info(s"${removed.sum} records removed linked to ancient submissions")
      }

    }

}
