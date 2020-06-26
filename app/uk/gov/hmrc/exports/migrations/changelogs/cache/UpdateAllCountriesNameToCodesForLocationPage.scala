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

import com.mongodb.client.MongoDatabase
import org.bson.{BsonType, Document}
import org.mongodb.scala.model.Filters.{`type`, and, exists, regex, eq => feq, ne => fne}
import org.mongodb.scala.model.Updates.set
import play.api.Logger
import uk.gov.hmrc.exports.migrations.changelogs.MigrationInformation
import uk.gov.hmrc.exports.services.CountriesService

import scala.collection.JavaConversions._

class UpdateAllCountriesNameToCodesForLocationPage extends CacheMigrationDefinition {

  private val logger = Logger(this.getClass)

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = "CEDS-2231 Change country name to country code for location page", order = 2, author = "Patryk Rudnicki")

  override def migrationFunction(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2231 Change country name to country code for location page' db migration... ")

    val documents: Iterable[Document] = getDeclarationsCollection(db).find(
      and(
        exists("locations.goodsLocation.country"),
        `type`("locations.goodsLocation.country", BsonType.STRING),
        fne("locations.goodsLocation.country", ""),
        regex("locations.goodsLocation.country", "^.{3,}$")
      )
    )

    documents.foreach { document =>
      val documentId = document.get(INDEX_ID).asInstanceOf[String]
      val eori = document.get(INDEX_EORI).asInstanceOf[String]

      val countryName = document.get("locations", classOf[Document]).get("goodsLocation", classOf[Document]).get("country").asInstanceOf[String]

      logger.info("Updating [" + countryName + "] for document Id [" + documentId + "]")
      getDeclarationsCollection(db)
        .updateOne(and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori)), set("locations.goodsLocation.country", getCountryCode(countryName)))
    }

    logger.info("Applying 'CEDS-2231 Change country name to country code for location page' db migration... Done.")
  }

  private def getCountryCode(countryName: String): String = {
    val service = new CountriesService
    service.allCountriesAsJava.toStream
      .find(country => country.countryName == countryName)
      .map(_.countryCode)
      .getOrElse(countryName)
  }

}
