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

import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, JobRunRepository}

import java.time.{Instant, LocalTime}
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeDraftDeclarationsJob @Inject() (appConfig: AppConfig, declarationRepository: DeclarationRepository, jobRunRepository: JobRunRepository)(
  implicit @Named("backgroundTasksExecutionContext") ec: ExecutionContext
) extends ScheduledJob with Logging {

  private[jobs] lazy val expireDuration = appConfig.draftTimeToLive

  override def firstRunTime: Option[LocalTime] = Some(appConfig.purgeDraftDeclarations.elapseTime)
  override def interval: FiniteDuration = appConfig.purgeDraftDeclarations.interval

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
