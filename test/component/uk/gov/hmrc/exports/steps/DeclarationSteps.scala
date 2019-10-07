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

package component.uk.gov.hmrc.exports.steps

import java.util.UUID

import component.uk.gov.hmrc.exports.steps.`User has completed declaration`.{aDeclaration, withChoice, withConsignmentReferences, withContainerData, withEori, withId, withStatus}
import component.uk.gov.hmrc.exports.syntax.{Action, Precondition, ScenarioContext}
import org.scalatest.concurrent.{AbstractPatienceConfiguration, IntegrationPatience, PatienceConfiguration}
import play.api.Application
import play.api.test.FakeRequest
import uk.gov.hmrc.exports.models.{Choice, Eori}
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration, Seal, TransportInformationContainer}
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import util.stubs.CustomsDeclarationsAPIService
import util.testdata.ExportsDeclarationBuilder
import util.testdata.ExportsTestData.{ValidHeaders, declarantLrnValue}

import scala.concurrent.{Await, Future}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.mvc.Result

import scala.util.control.NonFatal

object `User has completed declaration` extends Precondition with ExportsDeclarationBuilder {

  def name = "User has completed declaration"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val eori = context.get[Eori]
    val declaration = aDeclaration(
      withChoice(Choice.StandardDec),
      withId("id"),
      withEori(eori),
      withStatus(DeclarationStatus.DRAFT),
      withConsignmentReferences(lrn = declarantLrnValue),
      withContainerData(TransportInformationContainer("container123", Seq(Seal("seal1"))))
    )
    val repo = context.get[DeclarationRepository]
    when(repo.find(any(), any())).thenReturn(Future.successful(Some(declaration)))
    when(repo.update(any()), atLeastOnce()).thenAnswer(new Answer[Future[Option[ExportsDeclaration]]]{
      override def answer(invocation: InvocationOnMock): Future[Option[ExportsDeclaration]] = Future.successful(Some(invocation.getArgument(0)))
    })
    context.updated(declaration)
  }
}

object `User has no declaration` extends Precondition {
  override def name: String = "User has no declaration"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[DeclarationRepository]
    when(repo.find(any(), any())).thenReturn(Future.successful(None))
    context
  }
}

object `User has pre-submitted declaration` extends Precondition {
  override def name: String = "User has pre-submitted declaration"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val eori = context.get[Eori]
    val declaration = aDeclaration(
      withChoice(Choice.StandardDec),
      withId("id"),
      withEori(eori),
      withStatus(DeclarationStatus.COMPLETE),
      withConsignmentReferences(lrn = declarantLrnValue),
      withContainerData(TransportInformationContainer("container123", Seq(Seal("seal1"))))
    )
    val repo = context.get[DeclarationRepository]
    when(repo.find(any(), any())).thenReturn(Future.successful(Some(declaration)))
    context.updated(declaration)
  }
}

object `User perform declaration submission` extends Action with PatienceConfiguration with IntegrationPatience {
  import play.api.test.Helpers._
  override def execute(context: ScenarioContext): ScenarioContext = {
    val declarationId = context.maybe[ExportsDeclaration].map(_.id).getOrElse(UUID.randomUUID().toString)
    val endpoint = s"/declarations/${declarationId}/submission"
    val request = FakeRequest("POST", endpoint).withHeaders(ValidHeaders.toSeq: _*)
    val application = context.get[Application]
    val outcome = route(application, request).get
    val response: Result = try {
      Await.result(outcome, patienceConfig.timeout)
    } catch {
      case NonFatal(e) => Await.result(application.errorHandler.onServerError(request, e), patienceConfig.timeout)
    }
    context.updated(response)
  }
  def name = "User perform declaration submission"
}
