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

package uk.gov.hmrc.exports

import com.codahale.metrics.SharedMetricRegistries
import play.api.Application
import play.api.http.Status.{NOT_FOUND, _}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, ParsedNotificationRepository, SubmissionRepository}

class ExportsSubmissionReceivedSpec
    extends TypedFeatureSpec with ScalaFutures with IntegrationPatience with WireMockRunner with MockitoSugar with GuiceOneAppPerSuite
    with BeforeAndAfterAll with BeforeAndAfterEach {

  val mockSubmissionRepository: SubmissionRepository = mock[SubmissionRepository]
  val mockDeclarationRepository: DeclarationRepository = mock[DeclarationRepository]
  val mockNotificationsRepository: ParsedNotificationRepository = mock[ParsedNotificationRepository]

  override def fakeApplication: Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
      .overrides(bind[SubmissionRepository].toInstance(mockSubmissionRepository))
      .overrides(bind[ParsedNotificationRepository].toInstance(mockNotificationsRepository))
      .overrides(bind[DeclarationRepository].toInstance(mockDeclarationRepository))
      .configure(
        Map(
          "microservice.services.auth.host" -> ExternalServicesConfig.Host,
          "microservice.services.auth.port" -> ExternalServicesConfig.Port,
          "microservice.services.customs-declarations.host" -> ExternalServicesConfig.Host,
          "microservice.services.customs-declarations.port" -> ExternalServicesConfig.Port,
          "microservice.services.customs-declarations.submit-uri" -> CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
          "microservice.services.customs-declarations.bearer-token" -> dummyToken,
          "microservice.services.customs-declarations.api-version" -> CustomsDeclarationsAPIConfig.apiVersion,
          "auditing.enabled" -> false,
          "auditing.consumer.baseUri.host" -> ExternalServicesConfig.Host,
          "auditing.consumer.baseUri.port" -> ExternalServicesConfig.Port,
          "play.ws.timeout.request" -> "2s"
        )
      )
      .build()
  }

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def afterEach(): Unit = {
    reset(mockSubmissionRepository)
    reset(mockDeclarationRepository)
    reset(mockNotificationsRepository)
    resetMockServer()
    super.afterEach()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override def prepareContext(context: ScenarioContext): ScenarioContext =
    context
      .updated(patienceConfig)
      .updated(app)
      .updated(mockDeclarationRepository)
      .updated(mockSubmissionRepository)
      .updated(mockNotificationsRepository)

  feature("Export Service should handle user submissions when") {

    Scenario("an authorised user successfully submits a customs declaration") {
      _.Given(`Authorized user`)
        .And(`User has completed declaration`)
        .And(`User does not try submit declaration earlier`)
        .And(`Database add action without problems`)
        .And(`Customs declaration is fully operational`)
        .When(`User perform declaration submission`)
        .Then(`Declaration is submitted to customs-declarations`)
        .And(`Submission was created`)
        .And(`Submission has request action`)
        .And(`User has been authorized`)
    }

    Scenario("an authorised user tires to submit non existing declaration") {
      _.Given(`Authorized user`)
        .And(`User has no declaration`)
        .When(`User perform declaration submission`)
        .Then(`Result status` is NOT_FOUND)
        .And(`No submission was created`)
        .And(`No submission is posted on customs-declarations`)
        .And(`User has been authorized`)
    }

    Scenario("authorized user tries to submit pre-submitted declaration") {
      _.Given(`Authorized user`)
        .And(`User has pre-submitted declaration`)
        .When(`User perform declaration submission`)
        .Then(`Result status` is CONFLICT)
        .And(`No submission was created`)
        .And(`No submission is posted on customs-declarations`)
        .And(`User has been authorized`)
    }

    Seq(INTERNAL_SERVER_ERROR, BAD_GATEWAY, GATEWAY_TIMEOUT, NOT_FOUND, UNAUTHORIZED, BAD_REQUEST).foreach { upstreamCode =>
      Scenario(s"an authorised user tries to submit declaration, but the submission service returns $upstreamCode") {
        _.Given(`Authorized user`)
          .And(`User has completed declaration`)
          .And(`Customs declaration response` is upstreamCode)
          .When(`User perform declaration submission`)
          .Then(`Result status` is INTERNAL_SERVER_ERROR)
          .And(`Submission was created`)
          .And(`Declaration is submitted to customs-declarations`)
          .And(`Submission has no action`)
          .And(`User has been authorized`)
      }
    }

    Scenario("an authorised user tries to submit declaration, but submissions service is down") {
      _.Given(`Authorized user`)
        .And(`User has completed declaration`)
        .And(`Customs declaration does not response`)
        .When(`User perform declaration submission`)
        .Then(`Result status` is INTERNAL_SERVER_ERROR)
        .And(`Submission was created`)
        .And(`Submission has no action`)
        .And(`User has been authorized`)
    }

    Scenario("an authorised user tries to submit declaration, but database could not save submission") {
      _.Given(`Authorized user`)
        .And(`User has completed declaration`)
        .And(`User does not try submit declaration earlier`)
        .And(`Customs declaration is fully operational`)
        .And(`Submission save rise error`)
        .When(`User perform declaration submission`)
        .Then(`Result status` is INTERNAL_SERVER_ERROR)
        .And(`User has been authorized`)
    }

    Scenario("an authorised user tries to submit declaration, but database could not save submission action") {
      _.Given(`Authorized user`)
        .And(`User has completed declaration`)
        .And(`User does not try submit declaration earlier`)
        .And(`Customs declaration is fully operational`)
        .And(`Submission action save rise error`)
        .When(`User perform declaration submission`)
        .Then(`Result status` is INTERNAL_SERVER_ERROR)
        .And(`Submission was created`)
        .And(`User has been authorized`)
    }

    Scenario("upstream notifies about success earlier then send back result of successful request") {
      _.Given(`Authorized user`)
        .And(`User has completed declaration`)
        .And(`User does not try submit declaration earlier`)
        .And(`Database add action without problems`)
        .And(`Customs declaration is fully operational`)
        .And(`Notification came earlier than request is finished`)
        .When(`User perform declaration submission`)
        .Then(`Declaration is submitted to customs-declarations`)
        .And(`Submission was created`)
        .And(`Submission has request action`)
        .And(`Submission was updated for mrn`)
        .And(`User has been authorized`)
    }

    Scenario("an unauthorised user try to submit declaration") {
      _.Given(`Unauthorized user`)
        .When(`User perform declaration submission`)
        .Then(`Result status` is UNAUTHORIZED)
        .And(`User has been authorized`)
    }
  }
}
