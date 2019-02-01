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

package uk.gov.hmrc.exports.base

import java.util.UUID

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.libs.concurrent.Execution.Implicits
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfig, CSRFConfigProvider, CSRFFilter}
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.repositories.{MovementNotificationsRepository, MovementsRepository, NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.exports.repositories.{MovementNotificationsRepository, NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait CustomsExportsBaseSpec
    extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures with AuthTestSupport {

  val mockSubmissionRepository = mock[SubmissionRepository]
  val mockNotificationsRepository = mock[NotificationsRepository]
  val mockMovementNotificationsRepository = mock[MovementNotificationsRepository]
  val mockMovementsRepository = mock[MovementsRepository]
  val mockMNetrics = mock[ExportsMetrics]

  def injector: Injector = app.injector

  val cfg: CSRFConfig = injector.instanceOf[CSRFConfigProvider].get

  protected def component[T: ClassTag]: T = app.injector.instanceOf[T]

  val token: String = injector.instanceOf[CSRFFilter].tokenProvider.generateToken

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def wsClient: WSClient = injector.instanceOf[WSClient]

  override lazy val app: Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(mockAuthConnector),
        bind[SubmissionRepository].to(mockSubmissionRepository),
        bind[NotificationsRepository].to(mockNotificationsRepository),
        bind[MovementNotificationsRepository].to(mockMovementNotificationsRepository),
        bind[MovementsRepository].to(mockMovementsRepository),
        bind[ExportsMetrics].to(mockMNetrics)
      )
      .build()

  implicit val mat: Materializer = app.materializer

  implicit val ec: ExecutionContext = Implicits.defaultContext

  implicit lazy val patience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.milliseconds) // be more patient than the default

  protected def postRequest(
    uri: String,
    body: JsValue,
    headers: Map[String, String] = Map.empty
  ): FakeRequest[AnyContentAsJson] = {
    val session: Map[String, String] = Map(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.userId -> "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"
    )

    val tags = Map(Token.NameRequestTag -> cfg.tokenName, Token.RequestTag -> token)

    FakeRequest("POST", uri)
      .withHeaders((Map(cfg.headerName -> token) ++ headers).toSeq: _*)
      .withSession(session.toSeq: _*)
      .copyFakeRequest(tags = tags)
      .withJsonBody(body)
  }

  protected def withDataSaved(ok: Boolean) = {
    when(mockSubmissionRepository.save(any())).thenReturn(Future.successful(ok))
    when(mockMovementsRepository.save(any())).thenReturn(Future.successful(ok))
  }

  protected def getSubmission(submission: Option[Submission]) =
    when(mockSubmissionRepository.getByConversationId(any())).thenReturn(Future.successful(submission))

  protected def withMovements(movements: Seq[MovementSubmissions]) =
    when(mockMovementsRepository.findByEori(any())).thenReturn(Future.successful(movements))

  protected def withSubmissionUpdated(ok: Boolean) =
    when(mockSubmissionRepository.updateSubmission(any())).thenReturn(Future.successful(ok))

  protected def withSubmissions(submissions: Seq[Submission]) =
    when(mockSubmissionRepository.findByEori(any())).thenReturn(Future.successful(submissions))

  protected def withNotification(notification: Option[DeclarationNotification]) =
    when(mockNotificationsRepository.getByConversationId(any())).thenReturn(Future.successful(notification))

  protected def withSubmissionNotification(notification: Option[DeclarationNotification]) =
    when(mockNotificationsRepository.getByEoriAndConversationId(any(), any()))
      .thenReturn(Future.successful(notification))

  protected def withNotificationSaved(ok: Boolean) =
    when(mockNotificationsRepository.save(any())).thenReturn(Future.successful(ok))

  protected def haveNotifications(notifications: Seq[DeclarationNotification]) =
    when(mockNotificationsRepository.findByEori(any())).thenReturn(Future.successful(notifications))

  protected def withMovementNotificationSaved(ok: Boolean) =
    when(mockMovementNotificationsRepository.save(any())).thenReturn(Future.successful(ok))

  protected def withCancellationRequest(status: CancellationStatus) =
    when(mockSubmissionRepository.cancelDeclaration(any(), any())).thenReturn(Future.successful(status))
}
