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

package uk.gov.hmrc.exports.repositories

import org.mongodb.scala.ClientSession
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeSubmissionsTransactionalOps @Inject() (
  val mongoComponent: MongoComponent,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  notificationRepository: ParsedNotificationRepository,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends TransactionsOps with Logging {

  def removeSubmissionAndNotifications(
    declaration: Option[ExportsDeclaration],
    notifications: Seq[ParsedNotification],
    submission: Submission
  ): Future[Unit] =
    if (appConfig.useTransactionalDBOps)
      withSessionAndTransaction[Unit](startRemoveOp(_, notifications, declaration, submission)).recover { case e: Exception =>
        logger.warn(s"There was an error while writing to the DB => ${e.getMessage}", e)
        None
      }
    else nonTransactionalSession.flatMap(startRemoveOp(_, notifications, declaration, submission))

  private def startRemoveOp(
    session: ClientSession,
    notifications: Seq[ParsedNotification],
    declaration: Option[ExportsDeclaration],
    submission: Submission
  ): Future[Unit] =
    for {
      notificationsRemoved <- notificationRepository.removeEvery(session, Filters.in("actionId", notifications.map(_.actionId): _*))
//      declarationsRemoved <- declarationRepository.removeEvery(session, Filters.in("id", declaration.map(_.id)))
      submissionsRemoved <- submissionRepository.removeEvery(session, Filters.in("uuid", submission.uuid))
    } yield {
      println(">>>>>>>>>>")
      println(">>>>>>>>>>" + notificationsRemoved)
      println(">>>>>>>>>>" + submissionsRemoved)
      println(">>>>>>>>>>")
    }

}
