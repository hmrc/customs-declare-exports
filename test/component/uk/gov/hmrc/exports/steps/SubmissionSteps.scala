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

package uk.gov.hmrc.exports.steps

import scala.concurrent.Future

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqm}
import org.mockito.Mockito.{never, verify, verifyNoInteractions, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.must.Matchers
import reactivemongo.core.errors.GenericDatabaseException
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.exports.syntax.{Postcondition, Precondition, ScenarioContext}

object `User does not try submit declaration earlier` extends Precondition {
  def name = "User does not try submit declaration earlier"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[SubmissionRepository]
    when(repo.findOrCreate(any(), any(), any())).thenAnswer(new Answer[Future[Submission]] {
      override def answer(invocation: InvocationOnMock): Future[Submission] =
        Future.successful(invocation.getArgument(2))
    })
    context
  }
}

object `Database add action without problems` extends Precondition {
  import uk.gov.hmrc.exports.models.declaration.submissions.{Action => SubmissionAction}
  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[SubmissionRepository]
    when(repo.addAction(any[Submission](), any())).thenAnswer(new Answer[Future[Submission]] {
      override def answer(invocation: InvocationOnMock): Future[Submission] = {
        val submission = invocation.getArgument[Submission](0)
        val action = invocation.getArgument[SubmissionAction](1)
        Future.successful(submission.copy(actions = submission.actions :+ action))
      }
    })
    context
  }

  override def name: String = "Database add action without problems"
}

object `Submission was created` extends Postcondition {
  import org.mockito.ArgumentMatchers.{eq => eqm}
  override def execute(context: ScenarioContext): ScenarioContext = {
    val declaration = context.get[ExportsDeclaration]
    val eori = context.get[Eori]
    val repo = context.get[SubmissionRepository]
    verify(repo).findOrCreate(eqm(eori), eqm(declaration.id), any())
    context
  }

  def name = "Submission was created"
}

object `Submission has request action` extends Postcondition with Matchers {
  import uk.gov.hmrc.exports.models.declaration.submissions.{SubmissionRequest, Action => SubmissionAction}
  override def execute(context: ScenarioContext): ScenarioContext = {
    val declaration = context.get[ExportsDeclaration]
    val repo = context.get[SubmissionRepository]
    val submissionCaptor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
    val actionCaptor: ArgumentCaptor[SubmissionAction] = ArgumentCaptor.forClass(classOf[SubmissionAction])
    verify(repo).addAction(submissionCaptor.capture(), actionCaptor.capture())
    submissionCaptor.getValue.uuid mustEqual declaration.id
    actionCaptor.getValue.requestType mustEqual SubmissionRequest
    context
  }

  override def name: String = "Submission has request action"
}

object `No submission was created` extends Postcondition {
  override def name: String = "No submission was created"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[SubmissionRepository]
    verifyNoInteractions(repo)
    context
  }
}

object `Submission has no action` extends Postcondition {
  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[SubmissionRepository]
    verify(repo, never()).addAction(any[Submission](), any())
    context
  }

  override def name: String = "Submission has no action"
}

object `Submission save rise error` extends Precondition {
  override def name: String = "Submission could not be saved"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[SubmissionRepository]
    when(repo.findOrCreate(any(), any(), any())).thenReturn(Future.failed(GenericDatabaseException("Test", None)))
    context
  }
}

object `Submission action save rise error` extends Precondition {

  override def name: String = "Submission action save rise error"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val repo = context.get[SubmissionRepository]
    when(repo.addAction(any[Submission](), any())).thenReturn(Future.failed(GenericDatabaseException("Test", None)))
    context
  }
}

object `Submission was updated for mrn` extends Postcondition with Eventually {
  override def name: String = "Submission was updated for mrn"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val notification: Notification = context.get[Notification]
    notification.details.map { details =>
      val repo = context.get[SubmissionRepository]
      val conversationId = context.get[String]
      eventually {
        verify(repo).updateMrn(eqm(conversationId), eqm(details.mrn))
      }
    }
    context
  }
}
