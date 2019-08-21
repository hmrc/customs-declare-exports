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

package unit.uk.gov.hmrc.exports.controllers.actions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}
import play.api.mvc.{AnyContent, Request, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import unit.uk.gov.hmrc.exports.base.AuthTestSupport

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatorSpec
  extends WordSpec with MustMatchers with AuthTestSupport with BeforeAndAfterAll with Results {

  import play.api.test.Helpers._

  val system = ActorSystem(this.suiteName)

  val materializer = ActorMaterializer()(system)

  override protected def afterAll(): Unit = {
    await(system.terminate())
    super.afterAll()
  }

  val authoenticator = new Authenticator(mockAuthConnector, stubPlayBodyParsers(materializer))(ExecutionContext.global)

  val action: Request[AnyContent] => Future[Result] = _ => Future.successful(Ok(""))

  "Authenticator" should {
    "pass control futher" when {
      "there is EORI number" in {
        withAuthorizedUser()
        val outcome = authoenticator.invokeBlock(FakeRequest(), action)
        status(outcome) mustBe OK
      }
    }
    "return error response and log it" when {
      "there is no EORI in enrollments" in {
        userWithoutEori()
        val outcome = authoenticator.invokeBlock(FakeRequest(), action)
        status(outcome) mustBe UNAUTHORIZED
      }
      "there is no enrollments" in {
        withUnauthorizedUser(InsufficientEnrolments())
        val outcome = authoenticator.invokeBlock(FakeRequest(), action)
        status(outcome) mustBe UNAUTHORIZED
      }
      "there was authorisation error" in {
        withUnauthorizedUser(new AuthorisationException("test") { /* Anonymouse class */ })
        val outcome = authoenticator.invokeBlock(FakeRequest(), action)
        status(outcome) mustBe UNAUTHORIZED
      }
      "something went wrong in connector internals" in {
        when(mockAuthConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new InterruptedException("test")))
        val outcome = authoenticator.invokeBlock(FakeRequest(), action)
        status(outcome) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
