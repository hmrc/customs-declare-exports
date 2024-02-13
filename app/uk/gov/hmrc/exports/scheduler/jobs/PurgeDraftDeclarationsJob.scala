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

package uk.gov.hmrc.exports.scheduler.jobs

import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, JobRunRepository}

import java.time._
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeDraftDeclarationsJob @Inject() (appConfig: AppConfig, declarationRepository: DeclarationRepository, jobRunRepository: JobRunRepository)(
  implicit ec: ExecutionContext
) extends ScheduledJob with Logging {

  private lazy val jobConfig = appConfig.purgeDraftDeclarations
  private lazy val expireDuration = appConfig.draftTimeToLive

  override val name: String = "PurgeDraftDeclarations"
  override def interval: FiniteDuration = jobConfig.interval
  override def firstRunTime: Option[LocalTime] = Some(jobConfig.elapseTime)

  override def execute(): Future[Unit] =
    jobRunRepository.isFirstRunInTheDay(name).flatMap {
      case true =>
        logger.info("Starting PurgeDraftDeclarationsJob execution...")
        val expiryDate = Instant.now(appConfig.clock).truncatedTo(ChronoUnit.MILLIS).minusSeconds(expireDuration.toSeconds)
        for {
          count <- declarationRepository.deleteExpiredDraft(expiryDate)
          _ = logger.info(s"Finishing ${name}Job: Purged $count items updated before $expiryDate")
        } yield ()

      case false => Future.successful(())
    }
}
