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

package uk.gov.hmrc.exports.scheduler.jobs

import java.time._

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.repositories.DeclarationRepository

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeDraftDeclarationsJob @Inject()(appConfig: AppConfig, declarationRepository: DeclarationRepository)(implicit ec: ExecutionContext)
    extends ScheduledJob {

  private val jobConfig = appConfig.purgeDraftDeclarations
  private val expireDuration = appConfig.draftTimeToLive
  private val clock = appConfig.clock

  private val logger = Logger(this.getClass)

  override val name: String = "PurgeDraftDeclarations"
  override def interval: FiniteDuration = jobConfig.interval
  override def firstRunTime: LocalTime = jobConfig.elapseTime

  override def execute(): Future[Unit] = {

    val expiryDate = Instant.now(clock).minusSeconds(expireDuration.toSeconds)
    for {
      count <- declarationRepository.deleteExpiredDraft(expiryDate)
      _ = logger.info(s"${name}Job: Purged $count items updated before $expiryDate")
    } yield ()
  }

}
