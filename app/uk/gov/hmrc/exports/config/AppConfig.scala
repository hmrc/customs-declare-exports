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

package uk.gov.hmrc.exports.config

import com.google.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

@Singleton
class AppConfig @Inject()(override val runModeConfiguration: Configuration, val environment: Environment)
    extends ServicesConfig with AppName {

  override protected def mode: Mode = environment.mode

  override protected def appNameConfiguration: Configuration =
    runModeConfiguration

  private def loadConfig(key: String): String =
    runModeConfiguration
      .getString(key)
      .getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val authUrl: String = baseUrl("auth")

  lazy val loginUrl: String = loadConfig("urls.login")

  lazy val customsDeclarationsBaseUrl: String = baseUrl("customs-declarations")

  lazy val customsDeclarationsApiVersion: String =
    getString("microservice.services.customs-declarations.api-version")

  lazy val submitDeclarationUri: String = getString("microservice.services.customs-declarations.submit-uri")

  lazy val cancelDeclarationUri: String = getString("microservice.services.customs-declarations.cancel-uri")

  lazy val notificationBearerToken: String = getString("microservice.services.customs-declarations.bearer-token")

  lazy val developerHubClientId: String = getString("microservice.services.customs-declarations.client-id")
}
