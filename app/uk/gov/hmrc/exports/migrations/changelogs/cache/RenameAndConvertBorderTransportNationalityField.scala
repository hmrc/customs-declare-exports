/*
 * Copyright 2024 HM Revenue & Customs
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
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, equal, exists}
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.services.CountriesService

import scala.jdk.CollectionConverters._

class RenameAndConvertBorderTransportNationalityField(countriesService: CountriesService) extends MigrationDefinition with Logging {

  private val batchSize = 100

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-5606 Rename transportCrossingTheBorderNationality sub-field to countryCode. Convert values to country codes.",
      order = 25,
      author = "Tom Robinson"
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val declarationCollection = db.getCollection("declarations")
    val newFieldName = "countryCode"
    val oldFieldName = "countryName"
    val transport = "transport"
    val borderTransportNationality = "transportCrossingTheBorderNationality"

    declarationCollection
      .find(exists(s"transport.$borderTransportNationality.$oldFieldName"))
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val decId = document.get("id").toString
        val eori = document.get("eori").toString
        val filter = and(equal("eori", eori), equal("id", decId))

        val updatedDeclaration = updateTransportCrossingTheBorderNationality(document)
        declarationCollection.replaceOne(filter, updatedDeclaration)
      }

    def updateTransportCrossingTheBorderNationality(declaration: Document): Document = {
      val transportDoc = declaration.get(transport, classOf[Document])
      val borderTransportNationalityDoc = transportDoc.get(borderTransportNationality, classOf[Document])
      val borderTransportData = Option(borderTransportNationalityDoc.get(oldFieldName, classOf[String]))

      def renameFieldWithCountryCode(data: String): Document = {
        borderTransportNationalityDoc.put(newFieldName, data)
        borderTransportNationalityDoc.remove(oldFieldName)
        transportDoc.replace(borderTransportNationality, borderTransportNationalityDoc)
        declaration.replace(transport, transportDoc)
        declaration
      }

      val updatedDec = borderTransportData.map { data =>
        countriesService.getCountryCode(data) match {
          case Some(code) => renameFieldWithCountryCode(code)
          case None if countriesService.allCountries.map(_.countryCode).contains(data) =>
            renameFieldWithCountryCode(data) // Data is already a country code
          case _ =>
            logger.warn(s"Unable to find country code for [$data] while migrating.")
            renameFieldWithCountryCode(data)
        }
      }

      updatedDec.getOrElse(declaration)
    }

    logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
  }
}
