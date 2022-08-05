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

import org.bson.Document
import org.bson.json.{JsonMode, JsonWriterSettings}
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
  val appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  unparsedNotificationRepository: UnparsedNotificationWorkItemRepository,
  parsedNotificationRepository: ParsedNotificationRepository,
  transactionalOps: PurgeSubmissionsTransactionalOps
)(implicit ec: ExecutionContext)
    extends ScheduledJob with ExportsClient with Logging with DecorateAsScala {

  val submissionCollection = db.getCollection(submissionRepository.collectionName)
  val declarationCollection = db.getCollection(declarationRepository.collectionName)
  val notificationCollection = db.getCollection(parsedNotificationRepository.collectionName)
  val unparsedNotificationCollection = db.getCollection(unparsedNotificationRepository.collectionName)

  override val name: String = "PurgeAncientSubmissions"

  override def interval: FiniteDuration = jobConfig.interval
  override def firstRunTime: Option[LocalTime] = Some(jobConfig.elapseTime)

  private def jsonSettings(mode: JsonMode) = JsonWriterSettings.builder.outputMode(mode).build

  private val jobConfig = appConfig.purgeAncientSubmissions
  private val clock: Clock = appConfig.clock

  private val latestStatus = "latestEnhancedStatus"
  private val statusLastUpdated = "enhancedStatusLastUpdated"

  private val expiryDate = Codecs.toBson(ZonedDateTime.now(clock).minusDays(180))

  private val latestStatusLookup =
    in(latestStatus, List("GOODS_HAVE_EXITED", "DECLARATION_HANDLED_EXTERNALLY", "CANCELLED"): _*)

  private val olderThanDate = lte(statusLastUpdated, expiryDate)

  override def execute(): Future[Unit] = {

    implicit val formatNotification: Format[ParsedNotification] = ParsedNotification.format
    import ExportsDeclaration.Mongo._

    val submissions: List[Submission] = submissionCollection
      .find(and(olderThanDate, latestStatusLookup))
      .asScala
      .flatMap {
        parseJsonFromDocument[Submission]
      }
      .toList

    logger.info(s"Submissions found older than 180 days: ${submissions.size}")

    val declarations: List[ExportsDeclaration] = declarationCollection
      .find(in("id", submissions.map(_.uuid): _*))
      .asScala
      .flatMap {
        parseJsonFromDocument[ExportsDeclaration]
      }
      .toList

    logger.info(s"Declarations found linked to submissions: ${declarations.size}")

    val parsedNotifications: List[ParsedNotification] = notificationCollection
      .find(in("actionId", submissions.flatMap(_.actions.map(_.id)): _*))
      .asScala
      .flatMap {
        parseJsonFromDocument[ParsedNotification]
      }
      .toList

    logger.info(s"Parsed notifications found linked to submissions: ${parsedNotifications.size}")

    val unparsedNotifications: List[UnparsedNotification] =
      unparsedNotificationCollection
        .find(in("item.id", parsedNotifications.map(_.unparsedNotificationId.toString): _*))
        .asScala
        .map { document =>
          Json.parse(document.toJson)("item").as[UnparsedNotification]
        }
        .toList

    logger.info(s"Unparsed found linked to submissions: ${unparsedNotifications.size}")

    transactionalOps.removeSubmissionAndNotifications(submissions, declarations, parsedNotifications, unparsedNotifications) map { removed =>
      logger.info(s"${removed.sum} records removed linked to ancient submissions")
    }

  }

  private def parseJsonFromDocument[A](document: Document)(implicit format: Format[A]): Option[A] =
    Json.parse(document.toJson(jsonSettings(JsonMode.EXTENDED))).asOpt[A]

}
