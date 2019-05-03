/*
 * Copyright 2019 HM Revenue & Customs
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
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._

@Singleton
class ExportsMetrics @Inject()(metrics: Metrics) {

  val timers = Map(notificationMetric -> metrics.defaultRegistry.timer(s"$notificationMetric.timer"))

  val counters = Map(notificationMetric -> metrics.defaultRegistry.counter(s"$notificationMetric.counter"))

  def startTimer(feature: String): Context = timers(feature).time()

  def incrementCounter(feature: String): Unit = counters(feature).inc()

}

object MetricIdentifiers {
  val notificationMetric = "submission.notification"
}
