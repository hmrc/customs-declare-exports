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

package unit.uk.gov.hmrc.exports.config

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Mode.Test
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

class AppConfigSpec extends WordSpec with Matchers with MockitoSugar {
  private val validAppConfig: Config =
    ConfigFactory.parseString("""
      |urls.login="http://localhost:9949/auth-login-stub/gg-sign-in"
      |mongodb.uri="mongodb://localhost:27017/customs-declare-exports"
      |microservice.services.auth.host=localhostauth
      |microservice.services.auth.port=9988
      |microservice.services.customs-declarations.host=remotedec-api
      |microservice.services.customs-declarations.port=6000
      |microservice.services.customs-declarations.api-version=1.0
      |microservice.services.customs-declarations.submit-uri=/declarations
      |microservice.services.customs-declarations.cancel-uri=/declarations/cancel
      |microservice.services.customs-declarations.bearer-token=Bearer DummyBearerToken
    """.stripMargin)

  private val invalidAppConfig: Config = ConfigFactory.parseString("""
      |mongodb.uri="mongodb://localhost:27017/customs-declare-exports"
      |""".stripMargin)
  private val emptyAppConfig: Config = ConfigFactory.parseString("")
  private val validServicesConfiguration = Configuration(validAppConfig)
  private val invalidServicesConfiguration = Configuration(invalidAppConfig)

  val environment = Environment.simple()

  private def runMode(conf: Configuration): RunMode = new RunMode(conf, Test)
  private def servicesConfig(conf: Configuration) = new ServicesConfig(conf, runMode(conf))
  private def appConfig(conf: Configuration) = new AppConfig(conf, environment, servicesConfig(conf))

  "AppConfig" should {
    "return config as object model when configuration is valid" in {
      val configService: AppConfig = appConfig(validServicesConfiguration)

      configService.authUrl shouldBe "http://localhostauth:9988"
      configService.loginUrl shouldBe "http://localhost:9949/auth-login-stub/gg-sign-in"
      configService.customsDeclarationsApiVersion shouldBe "1.0"
      configService.submitDeclarationUri shouldBe "/declarations"
      configService.cancelDeclarationUri shouldBe "/declarations/cancel"
      configService.customsDeclarationsBaseUrl shouldBe "http://remotedec-api:6000"
      configService.notificationBearerToken shouldBe "Bearer DummyBearerToken"
    }

    "throw an exception when mandatory configuration is invalid" in {
      val configService: AppConfig = appConfig(invalidServicesConfiguration)

      val caught: RuntimeException = intercept[RuntimeException](configService.authUrl)
      caught.getMessage shouldBe "Could not find config auth.host"

      val caught1: RuntimeException = intercept[RuntimeException](configService.customsDeclarationsBaseUrl)
      caught1.getMessage shouldBe "Could not find config customs-declarations.host"

      val caught2: Exception = intercept[Exception](configService.loginUrl)
      caught2.getMessage shouldBe "Missing configuration key: urls.login"

      val caught3: RuntimeException = intercept[RuntimeException](configService.customsDeclarationsApiVersion)
      caught3.getMessage shouldBe "Could not find config key 'microservice.services.customs-declarations.api-version'"

      val caught4: RuntimeException = intercept[RuntimeException](configService.submitDeclarationUri)
      caught4.getMessage shouldBe "Could not find config key 'microservice.services.customs-declarations.submit-uri'"

      val caught5: RuntimeException = intercept[RuntimeException](configService.notificationBearerToken)
      caught5.getMessage shouldBe "Could not find config key 'microservice.services.customs-declarations.bearer-token'"

      val caught6: RuntimeException = intercept[RuntimeException](configService.cancelDeclarationUri)
      caught6.getMessage shouldBe "Could not find config key 'microservice.services.customs-declarations.cancel-uri'"
    }

    "developerHubClientId" should {
      val appName = "customs-declare-exports"
      val clientId = UUID.randomUUID.toString

      "return the configured value when explicitly set" in {
        val configService: AppConfig =
          appConfig(
            Configuration(
              "appName" -> appName,
              "microservice.services.customs-declarations.client-id" -> clientId,
              "mongodb.uri" -> "mongodb://localhost:27017/customs-declare-exports"
            )
          )

        configService.developerHubClientId shouldBe clientId
      }
    }
  }
}
