/*
 * Copyright 2021 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import akka.stream.Materializer
import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.filters.csrf.{CSRFConfig, CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.WcoSubmissionService
import uk.gov.hmrc.http.SessionKeys

trait CustomsExportsBaseSpec extends UnitSpec with GuiceOneAppPerSuite with AuthTestSupport {

  SharedMetricRegistries.clear()

  val mockSubmissionRepository: SubmissionRepository = mock[SubmissionRepository]
  val mockNotificationsRepository: NotificationRepository = mock[NotificationRepository]
  val mockDeclarationsApiConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  val mockWcoSubmissionService: WcoSubmissionService = mock[WcoSubmissionService]

  def injector: Injector = app.injector

  def randomConversationId: String = UUID.randomUUID().toString

  val cfg: CSRFConfig = injector.instanceOf[CSRFConfigProvider].get

  protected def component[T: ClassTag]: T = app.injector.instanceOf[T]

  val token: String = injector.instanceOf[CSRFFilter].tokenProvider.generateToken

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  val metrics: ExportsMetrics = injector.instanceOf[ExportsMetrics]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def wsClient: WSClient = injector.instanceOf[WSClient]

  override lazy val app: Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(mockAuthConnector),
        bind[SubmissionRepository].to(mockSubmissionRepository),
        bind[WcoSubmissionService].to(mockWcoSubmissionService),
        bind[NotificationRepository].to(mockNotificationsRepository),
        bind[CustomsDeclarationsConnector].to(mockDeclarationsApiConnector)
      )
      .build()

  implicit val mat: Materializer = app.materializer

  implicit val ec: ExecutionContext = global

  implicit lazy val patience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.milliseconds) // be more patient than the default

  protected def postRequest(uri: String, body: JsValue, headers: Map[String, String] = Map.empty): FakeRequest[AnyContentAsJson] = {
    val session: Map[String, String] =
      Map(SessionKeys.sessionId -> s"session-${UUID.randomUUID()}")

    FakeRequest("POST", uri)
      .withHeaders((Map(cfg.headerName -> token) ++ headers).toSeq: _*)
      .withSession(session.toSeq: _*)
      .withJsonBody(body)
  }

  protected def getSubmissionByID(submission: Option[Submission]): OngoingStubbing[Future[Option[Submission]]] =
    when(mockSubmissionRepository.findSubmissionByUuid(any(), any())).thenReturn(Future.successful(submission))

  protected def withSubmissions(submissions: Seq[Submission]): OngoingStubbing[Future[Seq[Submission]]] =
    when(mockSubmissionRepository.findAllSubmissionsForEori(any())).thenReturn(Future.successful(submissions))

  protected def withoutNotifications(): OngoingStubbing[Future[Seq[Notification]]] =
    when(mockNotificationsRepository.findNotificationsByActionId(any())).thenReturn(Future.successful(Seq.empty))

  protected def withNotification(notifications: Seq[Notification]): OngoingStubbing[Future[Seq[Notification]]] =
    when(mockNotificationsRepository.findNotificationsByActionId(any()))
      .thenReturn(Future.successful(notifications))

}
