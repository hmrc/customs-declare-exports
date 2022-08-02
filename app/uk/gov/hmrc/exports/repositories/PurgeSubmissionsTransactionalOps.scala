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

import org.bson.conversions.Bson
import org.mongodb.scala.ClientSession
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeSubmissionsTransactionalOps @Inject() (
  val mongoComponent: MongoComponent,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  notificationRepository: ParsedNotificationRepository,
  unparsedNotificationRespository: UnparsedNotificationWorkItemRepository,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Transactions with Logging {

  protected implicit val tc = TransactionConfiguration.strict

  protected lazy val nonTransactionalSession = mongoComponent.client.startSession().toFuture

  def removeSubmissionAndNotifications(
    submissions: Seq[Submission],
    declarations: Seq[ExportsDeclaration],
    parsedNotification: Seq[ParsedNotification],
    unparsedNotification: Seq[UnparsedNotification]
  ): Future[Seq[Long]] =
    if (appConfig.useTransactionalDBOps)
      withSessionAndTransaction[Seq[Long]](startRemoveOp(_, submissions, declarations, parsedNotification, unparsedNotification)).recover {
        case e: Exception =>
          logger.warn(s"There was an error while writing to the DB => ${e.getMessage}", e)
          Seq.empty
      }
    else nonTransactionalSession.flatMap(startRemoveOp(_, submissions, declarations, parsedNotification, unparsedNotification))

  private def startRemoveOp(
    session: ClientSession,
    submissions: Seq[Submission],
    declarations: Seq[ExportsDeclaration],
    parsedNotification: Seq[ParsedNotification],
    unparsedNotification: Seq[UnparsedNotification]
  ): Future[Seq[Long]] =
    for {
      submissionsRemoved <- submissionRepository.removeEvery(session, Filters.in("uuid", submissions.map(_.uuid)))
      declarationsRemoved <- removeDeclarations(declarations: Seq[ExportsDeclaration], session)
      notificationsRemoved <- notificationRepository.removeEvery(session, Filters.in("actionId", parsedNotification.map(_.actionId): _*))
      unparsedNotification <- removeUnparsedNotifications(session, Filters.in("actionId", unparsedNotification.map(_.actionId): _*))
    } yield Seq(submissionsRemoved, declarationsRemoved, notificationsRemoved, unparsedNotification)

  private def removeDeclarations(declarations: Seq[ExportsDeclaration], session: ClientSession): Future[Long] =
    declarationRepository.removeEvery(session, Filters.in("id", declarations.map(_.id)))

  private def removeUnparsedNotifications(session: ClientSession, filter: Bson): Future[Long] =
    unparsedNotificationRespository.collection.deleteMany(session, filter).toFuture.map(_.getDeletedCount)

}
