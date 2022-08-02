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

import com.mongodb.client.MongoCollection
import org.bson.Document
import org.mongodb.scala.model.Filters._
import play.api.Logging
import uk.gov.hmrc.mongo.play.json.Codecs
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.repositories._
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionRequest}
import uk.gov.hmrc.exports.mongo.ExportsClient

import java.time._
import javax.inject.{Inject, Singleton}
import scala.collection.convert.DecorateAsScala
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeAncientSubmissionsJob @Inject() (
  appConfig: AppConfig,
  exportsClient: ExportsClient,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  unparsedNotificationRepository: UnparsedNotificationWorkItemRepository,
  parsedNotificationRepository: ParsedNotificationRepository,
  transactionalOps: PurgeSubmissionsTransactionalOps
)(implicit ec: ExecutionContext)
    extends ScheduledJob with Logging with DecorateAsScala {

  val submissionCollection: MongoCollection[Document] = exportsClient.db.getCollection(submissionRepository.collectionName)
  val declarationCollection: MongoCollection[Document] = exportsClient.db.getCollection(declarationRepository.collectionName)
  val notificationCollection: MongoCollection[Document] = exportsClient.db.getCollection(parsedNotificationRepository.collectionName)
  val unparsedNotificationCollection: MongoCollection[Document] = exportsClient.db.getCollection(unparsedNotificationRepository.collectionName)

  private val jobConfig = appConfig.purgeAncientSubmissions
  private val clock = appConfig.clock

  override val name: String = "PurgeAncientSubmissions"
  override def interval: FiniteDuration = jobConfig.interval
  override def firstRunTime: Option[LocalTime] = Some(jobConfig.elapseTime)

  val latestStatus = "latestEnhancedStatus"
  val statusLastUpdated = "enhancedStatusLastUpdated"

  val expiryDate = Codecs.toBson(ZonedDateTime.now(clock).minusDays(180))

  val latestStatusLookup =
    or(
      equal(latestStatus, "GOODS_HAVE_EXITED"),
      equal(latestStatus, "DECLARATION_HANDLED_EXTERNALLY"),
      equal(latestStatus, "CANCELLED"),
      equal(latestStatus, "REJECTED")
    )

  val olderThanDate = lte(statusLastUpdated, expiryDate)

  override def execute(): Future[Unit] = {

    implicit val formatNotification: Format[ParsedNotification] = ParsedNotification.format
    implicit val formatDeclaration: Format[ExportsDeclaration] = ExportsDeclaration.Mongo.format

    val submissions: List[Submission] = submissionCollection
      .find(and(olderThanDate, latestStatusLookup))
      .asScala
      .flatMap { document =>
        Json.parse(document.toJson).asOpt[Submission]
      }
      .toList

    val declarations: List[ExportsDeclaration] = submissions.flatMap { submission =>
      declarationCollection
        .find(equal("id", submission.uuid))
        .asScala
        .flatMap { document =>
          Json.parse(document.toJson).asOpt[ExportsDeclaration]
        }
    }

    def notifications[A](collection: MongoCollection[Document])(implicit format: Format[A]): List[A] = submissions.flatMap { submission =>
      collection
        .find(in("actionId", submission.actions.filter(_.requestType == SubmissionRequest).map(_.id): _*))
        .asScala
        .flatMap { document =>
          Json.parse(document.toJson).asOpt[A]
        }
        .toList
    }

    val parsedNotifications: List[ParsedNotification] = notifications[ParsedNotification](notificationCollection)
    val unparsedNotifications: List[UnparsedNotification] = notifications[UnparsedNotification](unparsedNotificationCollection)

    transactionalOps.removeSubmissionAndNotifications(submissions, declarations, parsedNotifications, unparsedNotifications) map { removed =>
      logger.info(s"${removed.sum} records removed from purge of submissions")
    }

  }

}
