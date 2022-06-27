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

package uk.gov.hmrc.exports.base

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.{FakeRequest, Helpers}
import stubs.ExternalServicesConfig.{Host, Port}
import stubs.MockGenericDownstreamService
import testdata.ExportsTestData.{authToken, eori}
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, TestModule}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames}

import scala.reflect.ClassTag

trait IntegrationTestSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite with ExportsDeclarationBuilder with MockGenericDownstreamService {
  val databaseName = "test-customs-declare-exports"

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule))
      .configure(
        Map(
          "microservice.services.auth.host" -> Host,
          "microservice.services.auth.port" -> Port,
          "microservice.services.customs-data-store.host" -> Host,
          "microservice.services.customs-data-store.port" -> Port,
          "microservice.services.customs-declarations.host" -> Host,
          "microservice.services.customs-declarations.port" -> Port,
          "microservice.services.customs-declarations.submit-uri" -> "/",
          "microservice.services.customs-declarations.bearer-token" -> authToken,
          "microservice.services.customs-declarations.api-version" -> "1.0",
          "microservice.services.customs-declarations-information.host" -> Host,
          "microservice.services.customs-declarations-information.port" -> Port,
          "microservice.services.customs-declarations-information.fetch-mrn-status" -> "/mrn/ID/status",
          "microservice.services.customs-declarations-information.api-version" -> "1.0",
          "microservice.services.hmrc-email.host" -> Host,
          "microservice.services.hmrc-email.port" -> Port,
          "mongodb.uri" -> s"mongodb://localhost/$databaseName"
        )
      )
      .build()

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Bearer some-token")))

  override protected def beforeAll(): Unit = {
    super.beforeAll
    startMockServer
  }

  override protected def afterEach(): Unit = {
    resetMockServer
    super.afterEach
  }

  override protected def afterAll() {
    stopMockServer
    super.afterAll
  }

  val enrolments = Some(s"""{
      | "allEnrolments" : [
      |   {
      |     "key" : "HMRC-CUS-ORG",
      |     "identifiers" : [
      |       {
      |         "key" : "EORINumber",
      |         "value" : "$eori"
      |       }
      |     ],
      |     "state" : "Activated"
      |   }
      | ]
      |}""".stripMargin)

  def getWithAuth(call: Call): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(Helpers.GET, call.url).withHeaders(HeaderNames.authorisation -> "Bearer some-token")

  def instanceOf[T](implicit classTag: ClassTag[T]): T = app.injector.instanceOf[T]
}
