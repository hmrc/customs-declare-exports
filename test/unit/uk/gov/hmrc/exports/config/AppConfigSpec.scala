/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalTime

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.config.AppConfig.JobConfig

import scala.concurrent.duration._

class AppConfigSpec extends AnyFunSuite with Matchers with GuiceOneAppPerSuite {

  val mongodbUri = "mongodb://localhost:27017/customs-declare-exports"
  val authUrl = "http://localhost:8500"
  val loginUrl = "http://localhost:9949/auth-login-stub/gg-sign-in"
  val customsDeclarationsBaseUrl = "http://localhost:6790"
  val customsDeclarationsApiVersion = "1.0"
  val submitDeclarationUri = "/"
  val cancelDeclarationUri = "/cancellation-requests"
  val notificationBearerToken = "Bearer customs-declare-exports"
  val developerHubClientId = "customs-declare-exports"
  val draftTimeToLive: FiniteDuration = 30.days
  val purgeDraftDeclarations = JobConfig(LocalTime.of(23, 30), 1.day)
  val sendEmailsJobInterval: FiniteDuration = 5.minutes
  val notificationReattemptInterval: FiniteDuration = 60.seconds
  val sendEmailPagerDutyAlertTriggerDelay: FiniteDuration = 1.day
  val consideredFailedBeforeWorkItem: FiniteDuration = 4.minutes
  val customsDeclarationsInformationBaseUrl = "http://localhost:9834"
  val fetchMrnStatus = "/mrn/ID/status"
  val cdiApiVersion = "1.0"
  val cdiClientID = "customs-declare-exports"
  val cdiBearerToken = "Bearer customs-declare-exports"
  val customsDataStoreBaseUrl = "http://localhost:6790"
  val verifiedEmailPath = "/customs-data-store/eori/EORI/verified-email"
  val emailServiceBaseUrl = "http://localhost:8300"
  val sendEmailPath = "/hmrc/email"

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val appConfig = app.injector.instanceOf[AppConfig]

  test(s"mongodbUri must be $mongodbUri")(appConfig.mongodbUri mustBe mongodbUri)

  test(s"authUrl must be $authUrl")(appConfig.authUrl mustBe authUrl)

  test(s"loginUrl must be $loginUrl")(appConfig.loginUrl mustBe loginUrl)

  test(s"customsDeclarationsBaseUrl must be $customsDeclarationsBaseUrl") {
    appConfig.customsDeclarationsBaseUrl mustBe customsDeclarationsBaseUrl
  }

  test(s"customsDeclarationsApiVersion must be $customsDeclarationsApiVersion") {
    appConfig.customsDeclarationsApiVersion mustBe customsDeclarationsApiVersion
  }

  test(s"submitDeclarationUri must be $submitDeclarationUri")(appConfig.submitDeclarationUri mustBe submitDeclarationUri)

  test(s"cancelDeclarationUri must be $cancelDeclarationUri")(appConfig.cancelDeclarationUri mustBe cancelDeclarationUri)

  test(s"notificationBearerToken must be $notificationBearerToken")(appConfig.notificationBearerToken mustBe notificationBearerToken)

  test(s"developerHubClientId must be $developerHubClientId")(appConfig.developerHubClientId mustBe developerHubClientId)

  test(s"draftTimeToLive must be $draftTimeToLive")(appConfig.draftTimeToLive mustBe draftTimeToLive)

  test(s"purgeDraftDeclarations must be $purgeDraftDeclarations") {
    appConfig.purgeDraftDeclarations.elapseTime mustBe purgeDraftDeclarations.elapseTime
    appConfig.purgeDraftDeclarations.interval mustBe purgeDraftDeclarations.interval
  }

  test(s"sendEmailsJobInterval must be $sendEmailsJobInterval") {
    appConfig.sendEmailsJobInterval mustBe sendEmailsJobInterval
  }

  test(s"notificationReattemptInterval must be $notificationReattemptInterval") {
    appConfig.notificationReattemptInterval mustBe notificationReattemptInterval
  }

  test(s"consideredFailedBeforeWorkItem must be $consideredFailedBeforeWorkItem") {
    appConfig.consideredFailedBeforeWorkItem mustBe consideredFailedBeforeWorkItem
  }

  test(s"sendEmailPagerDutyAlertTriggerDelay must be $sendEmailPagerDutyAlertTriggerDelay") {
    appConfig.sendEmailPagerDutyAlertTriggerDelay mustBe sendEmailPagerDutyAlertTriggerDelay
  }

  test(s"customsDeclarationsInformationBaseUrl must be $customsDeclarationsInformationBaseUrl") {
    appConfig.customsDeclarationsInformationBaseUrl mustBe customsDeclarationsInformationBaseUrl
  }

  test(s"fetchMrnStatus must be $fetchMrnStatus")(appConfig.fetchMrnStatus mustBe fetchMrnStatus)

  test(s"cdiApiVersion must be $cdiApiVersion")(appConfig.cdiApiVersion mustBe cdiApiVersion)

  test(s"cdiClientID must be $cdiClientID")(appConfig.cdiClientID mustBe cdiClientID)

  test(s"cdiBearerToken must be $cdiBearerToken")(appConfig.cdiBearerToken mustBe cdiBearerToken)

  test(s"customsDataStoreBaseUrl must be $customsDataStoreBaseUrl")(appConfig.customsDataStoreBaseUrl mustBe customsDataStoreBaseUrl)

  test(s"verifiedEmailPath must be $verifiedEmailPath")(appConfig.verifiedEmailPath mustBe verifiedEmailPath)

  test(s"emailServiceBaseUrl must be $emailServiceBaseUrl")(appConfig.emailServiceBaseUrl mustBe emailServiceBaseUrl)

  test(s"sendEmailPath must be $sendEmailPath")(appConfig.sendEmailPath mustBe sendEmailPath)
}
