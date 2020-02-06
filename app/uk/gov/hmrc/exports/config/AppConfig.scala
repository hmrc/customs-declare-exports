/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.config

import java.time.{Clock, LocalTime}

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.mongobee.MongobeeConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, val environment: Environment, servicesConfig: ServicesConfig) {

  MongobeeConfig(loadConfig("mongodb.uri"))

  lazy val clock: Clock = Clock.systemUTC()

  private def loadConfig(key: String): String =
    runModeConfiguration
      .getOptional[String](key)
      .getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val authUrl: String = servicesConfig.baseUrl("auth")

  lazy val loginUrl: String = loadConfig("urls.login")

  lazy val customsDeclarationsBaseUrl: String = servicesConfig.baseUrl("customs-declarations")

  lazy val customsDeclarationsApiVersion: String =
    servicesConfig.getString("microservice.services.customs-declarations.api-version")

  lazy val submitDeclarationUri: String =
    servicesConfig.getString("microservice.services.customs-declarations.submit-uri")

  lazy val cancelDeclarationUri: String =
    servicesConfig.getString("microservice.services.customs-declarations.cancel-uri")

  lazy val notificationBearerToken: String =
    servicesConfig.getString("microservice.services.customs-declarations.bearer-token")

  lazy val developerHubClientId: String =
    servicesConfig.getString("microservice.services.customs-declarations.client-id")

  lazy val draftTimeToLive: FiniteDuration =
    servicesConfig.getDuration("draft.timeToLive").asInstanceOf[FiniteDuration]

  lazy val purgeDraftDeclarations: JobConfig = JobConfig(
    LocalTime.parse(servicesConfig.getString("scheduler.purge-draft-declarations.run-time")),
    servicesConfig.getDuration("scheduler.purge-draft-declarations.interval").asInstanceOf[FiniteDuration]
  )
}

case class JobConfig(elapseTime: LocalTime, interval: FiniteDuration)
