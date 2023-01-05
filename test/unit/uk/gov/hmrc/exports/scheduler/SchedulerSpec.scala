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

import akka.actor.{ActorSystem, Cancellable}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.BDDMockito.given
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.scheduler.jobs.ScheduledJob

import java.time.{LocalTime, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SchedulerSpec extends UnitSpec {

  private val job = mock[ScheduledJob]
  private val util = mock[SchedulerDateUtil]
  private val actorSystem = mock[ActorSystem]
  private val applicationLifecycle = mock[ApplicationLifecycle]
  private val config = mock[AppConfig]
  private val internalScheduler = mock[akka.actor.Scheduler]
  private val zone: ZoneId = ZoneOffset.UTC
  private val now: Instant = "2018-12-25T12:00:00"
  private val clock = Clock.fixed(now, zone)

  private def instant(datetime: String) =
    LocalDateTime.parse(datetime).atZone(zone).toInstant

  private implicit def string2Instant: String => Instant = { datetime =>
    instant(datetime)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    given(config.clock) willReturn clock
    given(actorSystem.scheduler) willReturn internalScheduler
    given(
      internalScheduler.scheduleWithFixedDelay(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any[ExecutionContext])
    ) will runTheJobImmediately
    given(job.execute()) willReturn Future.successful(())
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(job, internalScheduler)
  }

  "Scheduler" should {

    "Run job with valid schedule" in {
      // Given
      given(job.interval) willReturn 60.seconds
      given(job.firstRunTime) willReturn Some(LocalTime.parse("12:00"))
      given(job.name) willReturn "name"
      given(util.nextRun(any[LocalTime], any[FiniteDuration])) willReturn "2018-12-25T12:00:30"

      // When
      whenTheSchedulerStarts()

      // Then
      val schedule = theSchedule
      schedule.interval mustBe 60.seconds
      schedule.initialDelay mustBe 30.seconds

      verify(job).execute()
    }

    "Run job immediately" in {
      // Given
      given(job.interval) willReturn 60.seconds
      given(job.firstRunTime) willReturn None
      given(job.name) willReturn "name"

      // When
      whenTheSchedulerStarts()

      // Then
      val schedule = theSchedule
      schedule.interval mustBe 60.seconds
      schedule.initialDelay mustBe 0.seconds

      verify(job).execute()
    }

    "Delay to schedule job given a run date in the past" in {
      // Given
      given(job.interval) willReturn 3.seconds
      given(job.firstRunTime) willReturn Some(LocalTime.parse("12:00"))
      given(job.name) willReturn "name"
      given(util.nextRun(any[LocalTime], any[FiniteDuration])).willReturn("2018-12-25T11:59:59")

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
    new Scheduler(actorSystem, applicationLifecycle, config, util, ScheduledJobs(withJobs))

  private case class Schedule(initialDelay: FiniteDuration, interval: FiniteDuration)
}
