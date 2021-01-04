/*
 * Copyright 2021 HM Revenue & Customs
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
import javax.inject.{Inject, Provider}
import play.api.inject._
import play.api.{Configuration, Environment}

class Module extends play.api.inject.Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(bind[ScheduledJobs].toProvider[ScheduledJobProvider], bind[Scheduler].toSelf.eagerly())
}

class ScheduledJobProvider @Inject()(purgeDraftDeclarations: PurgeDraftDeclarationsJob) extends Provider[ScheduledJobs] {
  override def get(): ScheduledJobs = ScheduledJobs(Set(purgeDraftDeclarations))
}
