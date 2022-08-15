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

  def removeSubmissionAndNotifications(submissions: Seq[Submission]): Future[Seq[Long]] =
    if (appConfig.useTransactionalDBOps)
      withSessionAndTransaction[Seq[Long]](startRemovals(_, submissions)).recover { case e: Exception =>
        logger.warn(s"There was an error while writing to the DB => ${e.getMessage}", e)
        Seq.empty
      }
    else nonTransactionalSession.flatMap(startRemovals(_, submissions))

  private def startRemovals(session: ClientSession, submissions: Seq[Submission]): Future[Seq[Long]] =
    for {
      unparsedNotificationRemoved <- removeUnparsedNotifications(submissions, session)
      notificationsRemoved <- removeParsedNotifications(submissions, session)
      declarationsRemoved <- removeDeclarations(submissions, session)
      submissionsRemoved <- removeSubmissions(submissions, session)
    } yield Seq(submissionsRemoved, declarationsRemoved, notificationsRemoved, unparsedNotificationRemoved)

  private def removeSubmissions(submissions: Seq[Submission], session: ClientSession): Future[Long] = {
    val filter = Filters.in("uuid", submissions.map(_.uuid): _*)
    logger.debug(s"Attempting to remove submissions: $filter")
    submissionRepository.removeEvery(session, filter)
  }

  private def removeDeclarations(submissions: Seq[Submission], session: ClientSession): Future[Long] = {
    val filter = Filters.and(Filters.in("id", submissions.map(_.uuid): _*), Filters.in("eori", submissions.map(_.eori): _*))
    logger.debug(s"Attempting to remove declarations: $filter")
    declarationRepository.removeEvery(session, filter)
  }

  private def removeParsedNotifications(submissions: Seq[Submission], session: ClientSession): Future[Long] = {
    val filter = Filters.in("actionId", submissions.flatMap(_.actions.map(_.id)): _*)
    logger.debug(s"Attempting to remove notifications: $filter")
    notificationRepository.removeEvery(session, filter)
  }

  private def removeUnparsedNotifications(submissions: Seq[Submission], session: ClientSession): Future[Long] =
    notificationRepository.findNotifications(submissions.flatMap(_.actions.map(_.id))).flatMap { notifications =>
      val filter = Filters.in("item.id", notifications.map(_.unparsedNotificationId.toString): _*)
      logger.debug(s"Attempting to remove unparsed notifications: $filter")

      unparsedNotificationRespository.collection
        .deleteMany(session, filter)
        .toFuture
        .map(_.getDeletedCount)

    }

}
