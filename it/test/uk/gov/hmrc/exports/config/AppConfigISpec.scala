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

package uk.gov.hmrc.exports.config

import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.base.{IntegrationTestSpec, TestModule}
import uk.gov.hmrc.exports.config.AppConfig.JobConfig

import java.time.LocalTime
import scala.concurrent.duration._

class AppConfigISpec extends IntegrationTestSpec {

  private val mongodbUri = "mongodb://localhost:27017/customs-declare-exports"
  private val authUrl = "http://localhost:8500"
  private val customsDeclarationsBaseUrl = "http://localhost:6790"
  private val customsDeclarationsApiVersion = "1.0"
  private val submitDeclarationUri = "/"
  private val cancelDeclarationUri = "/cancellation-requests"
  private val amendDeclarationUri = "/amend"
  private val notificationBearerToken = "Bearer customs-declare-exports"
  private val developerHubClientId = "customs-declare-exports"
  private val draftTimeToLive: FiniteDuration = 30.days
  private val purgeDraftDeclarations = JobConfig(LocalTime.of(23, 30), 1.day)
  private val sendEmailsJobInterval: FiniteDuration = 1.hours
  private val notificationReattemptInterval: FiniteDuration = 60.seconds
  private val consideredFailedBeforeWorkItem: FiniteDuration = 4.minutes
  private val customsDeclarationsInformationBaseUrl = "http://localhost:9834"
  private val fetchMrnStatus = "/mrn/ID/status"
  private val cdiApiVersion = "1.0"
  private val cdiClientID = "customs-declare-exports"
  private val cdiBearerToken = "Bearer customs-declare-exports"
  private val customsDataStoreBaseUrl = "http://localhost:6790"
  private val verifiedEmailPath = "/customs-data-store/eori/EORI/verified-email"
  private val emailServiceBaseUrl = "http://localhost:8300"
  private val sendEmailPath = "/hmrc/email"

  override lazy val app = GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).build()

  private val appConfig = app.injector.instanceOf[AppConfig]

  assert(appConfig.mongodbUri == mongodbUri, s"mongodbUri must be $mongodbUri")

  assert(appConfig.authUrl == authUrl, s"authUrl must be $authUrl")

  assert(appConfig.customsDeclarationsBaseUrl == customsDeclarationsBaseUrl, s"customsDeclarationsBaseUrl must be $customsDeclarationsBaseUrl")

  assert(
    appConfig.customsDeclarationsApiVersion == customsDeclarationsApiVersion,
    s"customsDeclarationsApiVersion must be $customsDeclarationsApiVersion"
  )

  assert(appConfig.submitDeclarationUri == submitDeclarationUri, s"submitDeclarationUri must be $submitDeclarationUri")

  assert(appConfig.cancelDeclarationUri == cancelDeclarationUri, s"cancelDeclarationUri must be $cancelDeclarationUri")

  assert(appConfig.amendDeclarationUri == amendDeclarationUri, s"amendDeclarationUri must be $amendDeclarationUri")

  assert(appConfig.notificationBearerToken == notificationBearerToken, s"notificationBearerToken must be $notificationBearerToken")

  assert(appConfig.developerHubClientId == developerHubClientId, s"developerHubClientId must be $developerHubClientId")

  assert(appConfig.draftTimeToLive == draftTimeToLive, s"draftTimeToLive must be $draftTimeToLive")

  assert(
    appConfig.purgeDraftDeclarations.elapseTime == purgeDraftDeclarations.elapseTime,
    s"purgeDraftDeclarations.elapseTime must be ${purgeDraftDeclarations.elapseTime}"
  )
  assert(
    appConfig.purgeDraftDeclarations.interval == purgeDraftDeclarations.interval,
    s"purgeDraftDeclarations.interval must be ${purgeDraftDeclarations.interval}"
  )

  assert(appConfig.sendEmailsJobInterval == sendEmailsJobInterval, s"sendEmailsJobInterval must be $sendEmailsJobInterval")

  assert(
    appConfig.notificationReattemptInterval == notificationReattemptInterval,
    s"notificationReattemptInterval must be $notificationReattemptInterval"
  )

  assert(
    appConfig.consideredFailedBeforeWorkItem == consideredFailedBeforeWorkItem,
    s"consideredFailedBeforeWorkItem must be $consideredFailedBeforeWorkItem"
  )

  assert(
    appConfig.customsDeclarationsInformationBaseUrl == customsDeclarationsInformationBaseUrl,
    s"customsDeclarationsInformationBaseUrl must be $customsDeclarationsInformationBaseUrl"
  )

  assert(appConfig.fetchMrnStatus == fetchMrnStatus, s"fetchMrnStatus must be $fetchMrnStatus")

  assert(appConfig.cdiApiVersion == cdiApiVersion, s"cdiApiVersion must be $cdiApiVersion")

  assert(appConfig.cdiClientID == cdiClientID, s"cdiClientID must be $cdiClientID")

  assert(appConfig.cdiBearerToken == cdiBearerToken, s"cdiBearerToken must be $cdiBearerToken")

  assert(appConfig.customsDataStoreBaseUrl == customsDataStoreBaseUrl, s"customsDataStoreBaseUrl must be $customsDataStoreBaseUrl")

  assert(appConfig.verifiedEmailPath == verifiedEmailPath, s"verifiedEmailPath must be $verifiedEmailPath")

  assert(appConfig.emailServiceBaseUrl == emailServiceBaseUrl, s"emailServiceBaseUrl must be $emailServiceBaseUrl")

  assert(appConfig.sendEmailPath == sendEmailPath, s"sendEmailPath must be $sendEmailPath")
}
