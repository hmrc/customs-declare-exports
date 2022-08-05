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

package uk.gov.hmrc.exports.scheduler.jobs

import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.{and, equal, in, lte}
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.time._
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeAncientSubmissionsJob @Inject() (
  val mongoComponent: MongoComponent,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  parsedNotificationRepository: ParsedNotificationRepository,
  unparsedNotificationRepository: UnparsedNotificationWorkItemRepository,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends ScheduledJob with Logging with Transactions {

  override val name: String = "PurgeAncientSubmissions"

  override def interval: FiniteDuration = appConfig.purgeAncientSubmissions.interval
  override def firstRunTime: Option[LocalTime] = Some(appConfig.purgeAncientSubmissions.elapseTime)

  private implicit val tc = TransactionConfiguration.strict

  private lazy val nonTransactionalSession = mongoComponent.client.startSession().toFuture

  override def execute(): Future[Unit] = {
    val expiryDate = Codecs.toBson(ZonedDateTime.now(appConfig.clock).minusDays(180))
    val olderThanDate = lte("enhancedStatusLastUpdated", expiryDate)

    val latestStatusLookup =
      in("latestEnhancedStatus", List("GOODS_HAVE_EXITED", "DECLARATION_HANDLED_EXTERNALLY", "CANCELLED"): _*)

    submissionRepository.findAll(and(olderThanDate, latestStatusLookup)).flatMap { submissions =>
      if (submissions.isEmpty) Future.unit
      else {
        logger.info(s"Found ${submissions.size} Submission documents older than 180 days")
        if (appConfig.useTransactionalDBOps)
          withSessionAndTransaction[Unit](removeOlderDocuments(_, submissions)).recover {
            case e: Exception =>
              logger.warn(s"There was an error while reading/writing to the DB => ${e.getMessage}", e)
          }
        else nonTransactionalSession.flatMap(removeOlderDocuments(_, submissions))
      }
    }
  }

  private def removeOlderDocuments(session: ClientSession, submissions: Seq[Submission]): Future[Unit] = {
    for {
      submissionsRemoved <- submissionRepository.removeEvery(session, in("uuid", submissions.map(_.uuid): _*))
      declarationsRemoved <- removeDeclarations(session, submissions)
      parsedNotifications <- removeParsedNotifications(session, submissions)
        definedNotifications = filterOutNullValues(parsedNotifications)
      unparsedNotificationsRemoved <- removeUnparsedNotifications(session, definedNotifications)
    }
    yield {
      if (submissionsRemoved > 0L) logger.info(s"Removed $submissionsRemoved Submission documents")
      if (declarationsRemoved > 0L) logger.info(s"Removed $declarationsRemoved Declaration documents")
      if (definedNotifications.size > 0L) logger.info(s"Removed ${definedNotifications.size} ParsedNotification documents")
      if (unparsedNotificationsRemoved > 0L) logger.info(s"Removed $unparsedNotificationsRemoved UnparsedNotification documents")
    }
  }

  private def removeDeclarations(session: ClientSession, submissions: Seq[Submission]): Future[Long] = {
    val collection = declarationRepository.collection
    Future.sequence(submissions.map { submission =>
      val filter = and(equal("eori", submission.eori), equal("id", submission.uuid))
      collection.deleteOne(session, filter).toFuture.map(_.getDeletedCount)
    }).map(_.foldLeft(0L)(_ + _))
  }

  private def removeParsedNotifications(session: ClientSession, submissions: Seq[Submission]): Future[Seq[ParsedNotification]] = {
    val collection = parsedNotificationRepository.collection
    Future.sequence(submissions.flatMap { submission =>
      submission.actions.map { action =>
        val filter = BsonDocument("actionId" -> action.id)
        collection.findOneAndDelete(session, filter).toFuture
      }
    })
  }

  private def removeUnparsedNotifications(session: ClientSession, parsedNotifications: Seq[ParsedNotification]): Future[Long] = {
    val definedNotifications = filterOutNullValues(parsedNotifications)
    val filter = in("item.id", definedNotifications.map(_.unparsedNotificationId.toString): _*)
    unparsedNotificationRepository.collection.deleteMany(session, filter).toFuture.map(_.getDeletedCount)
  }

  private def filterOutNullValues[T](seq: Seq[T]): Seq[T] = seq.filter(Option(_).isDefined)
}
