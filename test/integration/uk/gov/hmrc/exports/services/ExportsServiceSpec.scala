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

package integration.uk.gov.hmrc.exports.services

import integration.uk.gov.hmrc.exports.base.IntegrationTestSpec
import integration.uk.gov.hmrc.exports.stubs.CustomsDeclarationsAPIService
import integration.uk.gov.hmrc.exports.util.CustomsDeclarationsAPIConfig
import integration.uk.gov.hmrc.exports.util.ExternalServicesConfig.{AuthToken, Host, Port}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.exports.services.ExportsService
import uk.gov.hmrc.http.HeaderCarrier
import util.ExportsTestData

import scala.concurrent.Future
import scala.xml.XML

class ExportsServiceSpec
    extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar with CustomsDeclarationsAPIService
    with ExportsTestData {

  val mockSubmissionRepository: SubmissionRepository = mock[SubmissionRepository]

  def overrideModules: Seq[GuiceableModule] = Nil

  override implicit lazy val app: Application =
    GuiceApplicationBuilder()
      .overrides(overrideModules: _*)
      .overrides(bind[SubmissionRepository].to(mockSubmissionRepository))
      .configure(
        Map(
          "microservice.services.customs-declarations.host" -> Host,
          "microservice.services.customs-declarations.port" -> Port,
          "microservice.services.customs-declarations.submit-uri" -> CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
          "microservice.services.customs-declarations.bearer-token" -> AuthToken
        )
      )
      .build()

  private lazy val exportsService = app.injector.instanceOf[ExportsService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def withSubmissionPersisted(result: Boolean): Unit =
    when(mockSubmissionRepository.save(any())).thenReturn(Future.successful(result))

  // TODO: seems like ExportService handle persisting / cancelling, as before not sure about cancellation
  // TODO: what other cases we could cover - I have done tests for persist/not persist and some errors on the way
  "Export Service" should {

    "save submission in DB" when {

      "it is persisted" in {

        startSubmissionService(ACCEPTED)
        withSubmissionPersisted(true)

        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(randomSubmitDeclaration.toXml)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")
      }
    }

    "do not save submission in DB" when {

      "it is not persisted" in {

        startSubmissionService(ACCEPTED)
        withSubmissionPersisted(false)

        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(randomSubmitDeclaration.toXml)
          )
        )

        contentAsString(result) should be("Failed saving submission")
      }

      "it is Not Accepted (BAD_REQUEST)" in {

        startSubmissionService(BAD_REQUEST)
        withSubmissionPersisted(false)

        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(randomSubmitDeclaration.toXml)
          )
        )

        contentAsString(result) should be("Non Accepted status returned by Customs Declaration Service")
      }

      "it is Not Accepted (NOT_FOUND)" in {

        startSubmissionService(NOT_FOUND)
        withSubmissionPersisted(false)

        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(randomSubmitDeclaration.toXml)
          )
        )

        contentAsString(result) should be("Non Accepted status returned by Customs Declaration Service")
      }

      "it is Not Accepted (UNAUTHORIZED)" in {

        startSubmissionService(UNAUTHORIZED)
        withSubmissionPersisted(false)

        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(randomSubmitDeclaration.toXml)
          )
        )

        contentAsString(result) should be("Non Accepted status returned by Customs Declaration Service")
      }

      "it is Not Accepted (INTERNAL_SERVER_ERROR)" in {

        startSubmissionService(INTERNAL_SERVER_ERROR)
        withSubmissionPersisted(false)

        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(randomSubmitDeclaration.toXml)
          )
        )

        contentAsString(result) should be("Non Accepted status returned by Customs Declaration Service")
      }
    }
  }

}
