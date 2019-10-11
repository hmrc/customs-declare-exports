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

package uk.gov.hmrc.exports.scheduler

import java.time._

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class PurgeDraftDeclarationsJob @Inject()(appConfig: AppConfig, declarationRepository: DeclarationRepository) extends ScheduledJob {

  private implicit val config: AppConfig = appConfig
  private implicit val carrier: HeaderCarrier = HeaderCarrier()
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
