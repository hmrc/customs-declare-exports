/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.migrations.changelogs.cache
import java.util

import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, elemMatch, exists, not, size, eq => feq}
import org.mongodb.scala.model.Updates.set
import play.api.Logger
import uk.gov.hmrc.exports.migrations.changelogs.MigrationInformation

import scala.collection.JavaConversions.{mapAsJavaMap, seqAsJavaList, _}

class ChangeRoutingCountriesStructure extends CacheMigrationDefinition {

  private val logger = Logger(this.getClass)

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = "CEDS-2247 Change routing countries structure", order = 5, author = "Patryk Rudnicki")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info("Applying 'CEDS-2247 Change routing countries structure' db migration... ")

    val documents: Iterable[Document] = getDeclarationsCollection(db).find(
      and(
        not(elemMatch("locations.routingCountries", exists("code", true))),
        exists("locations.routingCountries"),
        not(size("locations.routingCountries", 0))
      )
    )

    documents.foreach { document =>
      val documentId = document.get(INDEX_ID).asInstanceOf[String]
      val eori = document.get(INDEX_EORI).asInstanceOf[String]

      val routingCountries = document.get("locations", classOf[Document]).get("routingCountries", classOf[util.List[String]]).toSeq

      val codesList = seqAsJavaList(routingCountries.map(code => mapAsJavaMap(Map("code" -> code))))

      logger.info("Updating document Id [" + documentId + "]")
      getDeclarationsCollection(db).updateOne(and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori)), set("locations.routingCountries", codesList))
    }

    logger.info("Applying 'CEDS-2247 Change destination country structure' db migration... Done.")
  }
}
