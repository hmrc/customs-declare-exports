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

import component.uk.gov.hmrc.exports.syntax.{Precondition, ScenarioContext}
import uk.gov.hmrc.exports.repositories.NotificationRepository
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

import scala.concurrent.Future

import testdata.NotificationTestData._

object `Notification came earlier than request is finished` extends Precondition {
  override def name: String = "Notification came earlier than request is finished"

  override def execute(context: ScenarioContext): ScenarioContext = {
    val conversationId = context.get[String] // FIXME converstationId as value type
    val repo = context.get[NotificationRepository]
    val notification = exampleNotification(conversationId)
    val notifications = Seq(notification)
    when(repo.findNotificationsByActionId(any())).thenReturn(Future.successful(notifications))
    context.updated(notification)
  }
}
