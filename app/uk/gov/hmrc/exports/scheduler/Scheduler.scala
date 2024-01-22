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

package uk.gov.hmrc.exports.scheduler

import org.apache.pekko.actor.{ActorSystem, Cancellable}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.scheduler.jobs.ScheduledJob

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Scheduler @Inject() (
  actorSystem: ActorSystem,
  applicationLifecycle: ApplicationLifecycle,
  appConfig: AppConfig,
  schedulerDateUtil: SchedulerDateUtil,
  scheduledJobs: ScheduledJobs
)(implicit ec: ExecutionContext)
    extends Logging {

  private val runningJobs: Iterable[Cancellable] = scheduledJobs.jobs.map { job =>
    val initialDelay = calcInitialDelay(job)
    val interval = s"${job.interval.length} ${job.interval.unit}"
    logger.info(
      s"Scheduling job [${job.name}] to run periodically at [${job.firstRunTime}] with initialDelay of [$initialDelay] and interval [$interval]"
    )
    actorSystem.scheduler.scheduleWithFixedDelay(initialDelay, job.interval)(new Runnable() {
      override def run(): Unit =
        job.execute().map { _ =>
          logger.info(s"Scheduled Job [${job.name}]: Completed Successfully")
        } recover { case t: Throwable =>
          logger.error(s"Scheduled Job [${job.name}]: Failed", t)
        }
    })
  }

  applicationLifecycle.addStopHook(() => Future.successful(runningJobs.foreach(_.cancel())))

  private def calcInitialDelay(job: ScheduledJob): FiniteDuration =
    job.firstRunTime match {
      case Some(firstRunTimeValue) =>
        val nextRunDate = schedulerDateUtil.nextRun(firstRunTimeValue, job.interval)
        durationUntil(nextRunDate)

      case None => FiniteDuration(0, TimeUnit.SECONDS)
    }

  private def durationUntil(datetime: Instant): FiniteDuration = {
    val now = Instant.now(appConfig.clock)

    if (datetime.isBefore(now)) FiniteDuration(now.until(now.plusSeconds(1), ChronoUnit.SECONDS), TimeUnit.SECONDS)
    else FiniteDuration(now.until(datetime, ChronoUnit.SECONDS), TimeUnit.SECONDS)
  }
}
