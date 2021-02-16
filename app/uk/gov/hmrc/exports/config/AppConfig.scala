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

package uk.gov.hmrc.exports.config

import java.time.{Clock, LocalTime}

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.config.AppConfig.JobConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject()(val configuration: Configuration, val environment: Environment, servicesConfig: ServicesConfig) {

  lazy val clock: Clock = Clock.systemUTC()

  private def loadConfig(key: String): String =
    configuration
      .getOptional[String](key)
      .getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val mongodbUri: String = configuration.get[String]("mongodb.uri")

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

  lazy val customsDeclarationsInformationBaseUrl = servicesConfig.baseUrl("customs-declarations-information")
  lazy val fetchMrnStatus = servicesConfig.getString("microservice.services.customs-declarations-information.fetch-mrn-status")

  lazy val cdiApiVersion = servicesConfig.getString("microservice.services.customs-declarations-information.api-version")

  lazy val cdiClientID = servicesConfig.getString("microservice.services.customs-declarations-information.client-id")

  lazy val cdiBearerToken = servicesConfig.getString("microservice.services.customs-declarations-information.bearer-token")

  lazy val customsDataStoreBaseUrl: String = servicesConfig.baseUrl("customs-data-store")

  lazy val verifiedEmailPath: String = configuration.get[String]("microservice.services.customs-data-store.verified-email-path")

  lazy val emailServiceBaseUrl: String = servicesConfig.baseUrl("hmrc-email")

  lazy val sendEmailPath: String = configuration.get[String]("microservice.services.hmrc-email.send-email-path")
}

object AppConfig {
  case class JobConfig(elapseTime: LocalTime, interval: FiniteDuration)
}
