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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import util.ExternalServicesConfig._
import util.stubs.CustomsDeclarationsAPIService
import util.{CustomsDeclarationsAPIConfig, ExportsTestData}

import scala.concurrent.Future
import scala.xml.XML

class SubmissionServiceSpec
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
          "microservice.services.customs-declarations.bearer-token" -> authToken
        )
      )
      .build()

  private lazy val exportsService = app.injector.instanceOf[SubmissionService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def withSubmissionPersisted(result: Boolean): Unit =
    when(mockSubmissionRepository.save(any())).thenReturn(Future.successful(result))

  def withDeclarationCancelled(result: CancellationStatus): Unit =
    when(mockSubmissionRepository.cancelDeclaration(any(), any())).thenReturn(Future.successful(result))

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

    // TODO: add status for submission actually cancelled, as we are missing it
    "handle submission cancellation" when {

      "it is cancelled for the first time" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(ACCEPTED)

        withSubmissionPersisted(true)
        withDeclarationCancelled(CancellationRequested)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "FAKE_MRN", XML.loadString(declaration)))

        cancel.right.get should be(CancellationRequested)
      }

      "it is already cancelled" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(ACCEPTED)

        withSubmissionPersisted(true)
        withDeclarationCancelled(CancellationRequestExists)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "FAKE_MRN", XML.loadString(declaration)))

        cancel.right.get should be(CancellationRequestExists)
      }

      "non existing submission is cancelled" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(ACCEPTED)

        withSubmissionPersisted(true)
        withDeclarationCancelled(MissingDeclaration)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "", XML.loadString(declaration)))

        cancel.right.get should be(MissingDeclaration)
      }

      "it is Not Accepted (BAD_REQUEST)" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(BAD_REQUEST)

        withSubmissionPersisted(true)
        withDeclarationCancelled(CancellationRequested)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "FAKE_MRN", XML.loadString(declaration)))

        cancel.left.get.header.status should be(INTERNAL_SERVER_ERROR)
      }

      "it is Not Accepted (NOT_FOUND)" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(NOT_FOUND)

        withSubmissionPersisted(true)
        withDeclarationCancelled(CancellationRequested)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "FAKE_MRN", XML.loadString(declaration)))

        cancel.left.get.header.status should be(INTERNAL_SERVER_ERROR)
      }

      "it is Not Accepted (UNAUTHORIZED)" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(UNAUTHORIZED)

        withSubmissionPersisted(true)
        withDeclarationCancelled(CancellationRequested)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "FAKE_MRN", XML.loadString(declaration)))

        cancel.left.get.header.status should be(INTERNAL_SERVER_ERROR)
      }

      "it is Not Accepted (INTERNAL_SERVER_ERROR)" in {

        startSubmissionService(ACCEPTED)
        startCancellationService(INTERNAL_SERVER_ERROR)

        withSubmissionPersisted(true)
        withDeclarationCancelled(CancellationRequested)

        val declaration = randomSubmitDeclaration.toXml

        // persist
        val result: Result = await(
          exportsService.handleSubmission(
            declarantEoriValue,
            Some(declarantDucrValue),
            declarantLrnValue,
            XML.loadString(declaration)
          )
        )

        contentAsString(result) should be("{\"status\":202,\"message\":\"Submission response saved\"}")

        // cancel
        val cancel: Either[Result, CancellationStatus] =
          await(exportsService.handleCancellation(declarantEoriValue, "FAKE_MRN", XML.loadString(declaration)))

        cancel.left.get.header.status should be(INTERNAL_SERVER_ERROR)
      }
    }
  }
}
