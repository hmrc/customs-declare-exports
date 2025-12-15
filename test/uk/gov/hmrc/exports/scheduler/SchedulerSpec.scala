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

package uk.gov.hmrc.exports.scheduler

import org.apache.pekko.actor.{ActorSystem, Cancellable}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.scheduler.jobs.ScheduledJob

import java.time.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

class SchedulerSpec extends UnitSpec {

  private val job = mock[ScheduledJob]
  private val util = mock[SchedulerDateUtil]
  private val actorSystem = mock[ActorSystem]
  private val applicationLifecycle = mock[ApplicationLifecycle]
  private val appConfig = mock[AppConfig]
  private val internalScheduler = mock[org.apache.pekko.actor.Scheduler]
  private val zone: ZoneId = ZoneOffset.UTC
  private val clock = Clock.fixed(instant("2018-12-25T12:00:00"), zone)

  private def instant(datetime: String) =
    LocalDateTime.parse(datetime).atZone(zone).toInstant

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(appConfig.clock) thenReturn clock
    when(actorSystem.scheduler) thenReturn internalScheduler
    when(
      internalScheduler.scheduleWithFixedDelay(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any[ExecutionContext])
    ) thenReturn runTheJobImmediately
    when(job.execute()) thenReturn Future.successful(())
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(job, internalScheduler)
  }

  "Scheduler" should {

    "Run job with valid schedule" in {
      // when
      when(job.interval) thenReturn 60.seconds
      when(job.firstRunTime) thenReturn Some(LocalTime.parse("12:00"))
      when(job.name) thenReturn "name"
      when(util.nextRun(any[LocalTime], any[FiniteDuration])) thenReturn instant("2018-12-25T12:00:30")

      // When
      whenTheSchedulerStarts()

      // Then
      val schedule = theSchedule
      schedule.interval mustBe 60.seconds
      schedule.initialDelay mustBe 30.seconds

      verify(job).execute()
    }

    "Run job immediately" in {
      // when
      when(job.interval) thenReturn 60.seconds
      when(job.firstRunTime) thenReturn None
      when(job.name) thenReturn "name"

      // When
      whenTheSchedulerStarts()

      // Then
      val schedule = theSchedule
      schedule.interval mustBe 60.seconds
      schedule.initialDelay mustBe 0.seconds

      verify(job).execute()
    }

    "Delay to schedule job when a run date in the past" in {
      // when
      when(job.interval) thenReturn 3.seconds
      when(job.firstRunTime) thenReturn Some(LocalTime.parse("12:00"))
      when(job.name) thenReturn "name"
      when(util.nextRun(any[LocalTime], any[FiniteDuration])) thenReturn instant("2018-12-25T11:59:59")

      // When
      whenTheSchedulerStarts()

      // Then
      val schedule = theSchedule
      schedule.interval mustBe 3.seconds
      schedule.initialDelay mustBe 1.seconds

      verify(job).execute()
    }
  }

  private def runTheJobImmediately: Answer[Cancellable] = (invocation: InvocationOnMock) => {
    val arg: Runnable = invocation.getArgument(2)
    if (arg != null) {
      arg.run
    }
    Cancellable.alreadyCancelled
  }

  private def theSchedule: Schedule = {
    val intervalCaptor: ArgumentCaptor[FiniteDuration] = ArgumentCaptor.forClass(classOf[FiniteDuration])
    val initialDelayCaptor: ArgumentCaptor[FiniteDuration] = ArgumentCaptor.forClass(classOf[FiniteDuration])
    verify(internalScheduler).scheduleWithFixedDelay(initialDelayCaptor.capture(), intervalCaptor.capture())(any[Runnable])(any[ExecutionContext])
    Schedule(initialDelayCaptor.getValue, intervalCaptor.getValue)
  }

  private def whenTheSchedulerStarts(withJobs: Set[ScheduledJob] = Set(job)): Scheduler =
    new Scheduler(actorSystem, applicationLifecycle, appConfig, util, ScheduledJobs(withJobs))

  private case class Schedule(initialDelay: FiniteDuration, interval: FiniteDuration)
}
