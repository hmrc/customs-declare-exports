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

import org.mongodb.scala.bson.BsonDocument
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.repositories._

import java.time._
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeAncientSubmissionsJob @Inject() (
  appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  transactionalOps: PurgeSubmissionsTransactionalOps
)(implicit ec: ExecutionContext)
    extends ScheduledJob with Logging {

  override val name: String = "PurgeAncientSubmissions"

  override def interval: FiniteDuration = appConfig.purgeAncientSubmissions.interval

  override def firstRunTime: Option[LocalTime] = Some(appConfig.purgeAncientSubmissions.elapseTime)

  val expiryDate = ZonedDateTime.now(appConfig.clock).minusDays(180)

  override def execute(): Future[Unit] = {
    logger.info(s"Starting PurgeAncientSubmissionsJob. Removing Submissions having 'enhancedStatusLastUpdated' older than $expiryDate")
    submissionRepository.findAll(filter) flatMap { submissions =>
      transactionalOps.removeSubmissionAndNotifications(submissions) map { removed =>
        logger.info(s"Finishing PurgeAncientSubmissionsJob - ${removed.sum} records removed linked to ancient submissions")
      }
    }
  }

  private lazy val filter = {
    val statusesToRemove =
      List(CANCELLED, DECLARATION_HANDLED_EXTERNALLY, ERRORS, EXPIRED_NO_ARRIVAL, EXPIRED_NO_DEPARTURE, GOODS_HAVE_EXITED, WITHDRAWN)
    BsonDocument(s"""
         |{
         |  "latestEnhancedStatus": { "$$in": [ ${statusesToRemove.map(s => s""""$s"""").mkString(",")} ] },
         |  "enhancedStatusLastUpdated": { "$$lte": "${expiryDate}" }
         |}""".stripMargin)
  }
}
