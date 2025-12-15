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

package uk.gov.hmrc.exports.metrics

import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.{Counter, Monitor, Timer}
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExportsMetrics @Inject() (metrics: Metrics) {

  private val registry = metrics.defaultRegistry

  def startTimer(timer: Timer): Context = registry.timer(timer.path).time()

  def incrementCounter(counter: Counter): Unit = registry.counter(counter.path).inc()

  def timeCall[A](timer: Timer)(block: => A): A = {
    val timerContext: Context = startTimer(timer)
    val result = block
    timerContext.stop()
    result
  }

  def timeAsyncCall[A](timer: Timer)(block: ()  => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    val timerContext: Context = startTimer(timer)
    val timerRunning: AtomicBoolean = new AtomicBoolean(true)

    try {
      val result = block()

      result.foreach { _ =>
        if (timerRunning.compareAndSet(true, false)) {
          timerContext.stop()
        }
      }

      result.failed.foreach { _ =>
        if (timerRunning.compareAndSet(true, false)) {
          timerContext.stop()
        }
      }

      result
    } catch {
      case NonFatal(e) =>
        if (timerRunning.compareAndSet(true, false)) {
          timerContext.stop()
        }
        throw e
    }
  }

  def timeAsyncCall[A](monitor: Monitor)(block: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    incrementCounter(monitor.callCounter)
    val timerContext: Context = startTimer(monitor.timer)
    val timerRunning: AtomicBoolean = new AtomicBoolean(true)

    try {
      val result = block

      result.foreach { _ =>
        if (timerRunning.compareAndSet(true, false)) {
          timerContext.stop()
          incrementCounter(monitor.completionCounter)
        }
      }

      result.failed.foreach { _ =>
        if (timerRunning.compareAndSet(true, false)) {
          timerContext.stop()
          incrementCounter(monitor.failureCounter)
        }
      }

      result
    } catch {
      case NonFatal(e) =>
        if (timerRunning.compareAndSet(true, false)) {
          timerContext.stop()
          incrementCounter(monitor.failureCounter)
        }
        throw e
    }
  }

}

object ExportsMetrics {

  case class Timer(name: String) { val path = s"$name.timer" }
  case class Counter(name: String) { val path = s"$name.counter" }

  case class Monitor(name: String) {
    val path: String = s"$name.monitor"
    val timer: Timer = Timer(path)
    val callCounter: Counter = Counter(path)
    val completionCounter: Counter = Counter(s"$path.success")
    val failureCounter: Counter = Counter(s"$path.failed")
  }

  val submissionMonitor = Monitor(SubmissionMetric.wholeSubmission)
  val amendmentMonitor = Monitor(AmendmentMetric.wholeSubmission)

  object Timers {
    val notificationTimer: Timer = Timer(NotificationMetric.notificationReceiptHandling)

    val upstreamCustomsDeclarationsTimer: Timer = Timer(UpstreamMetric.customsDeclarations)

    val declarationFindAllTimer: Timer = Timer(DeclarationMetric.findAll)
    val declarationFindSingleTimer: Timer = Timer(DeclarationMetric.findSingle)

    val submissionProduceMetaDataTimer: Timer = Timer(SubmissionMetric.produceMetaData)
    val submissionConvertToXmlTimer: Timer = Timer(SubmissionMetric.convertToXml)
    val submissionFindOrCreateSubmissionTimer: Timer = Timer(SubmissionMetric.findOrCreateSubmission)
    val submissionSendToDecApiTimer: Timer = Timer(SubmissionMetric.sendToDecApi)

    val amendmentProduceMetaDataTimer: Timer = Timer(AmendmentMetric.produceMetaData)
    val amendmentConvertToXmlTimer: Timer = Timer(AmendmentMetric.convertToXml)
    val amendmentSendToDecApiTimer: Timer = Timer(AmendmentMetric.sendToDecApi)
    val amendmentUpdateDeclarationStatusTimer: Timer = Timer(AmendmentMetric.updateDeclarationStatus)
    val amendmentAddSubmissionActionTimer: Timer = Timer(AmendmentMetric.addSubmissionAction)
  }

  object Counters {
    val notificationCounter: Counter = Counter(NotificationMetric.notificationReceiptHandling)
  }
}
