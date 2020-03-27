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

package uk.gov.hmrc.exports.mongock.changesets

import java.util.Map

import com.github.cloudyrock.mongock.{ChangeLog, ChangeSet}
import com.google.common.collect.ImmutableMap
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoDatabase
import org.bson.Document
import play.api.Logger

import scala.collection.JavaConversions._
import uk.gov.hmrc.exports.services.CountriesService

class CacheChangeLog {

  private val collection = "declarations"

  private val logger = Logger(this.getClass)

  @ChangeSet(order = "001", id = "Exports DB Baseline", author = "Paulo Monteiro")
  def dbBaseline(db: MongoDatabase): Unit = {}

  @ChangeSet(order = "002", id = "CEDS-2231 Change country name to country code for location page", author = "Patryk Rudnicki")
  def updateAllCountriesNameToCodesForLocationPage(db: MongoDatabase): Unit = {
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.goodsLocation.country", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = db.getCollection(collection).find(new BasicDBObject(query)).toSeq

    documents.foreach { document =>
      val goodsLocation = document
        .get("locations", classOf[Map[String, Map[String, String]]])
        .get("goodsLocation")
        .asInstanceOf[Map[String, String]]

      val countryName: String = goodsLocation.get("country")

      goodsLocation.put("country", findCountryCode(countryName))

      // update document on MongoDB
      val queryIndexes: Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      db.getCollection(collection).replaceOne(objectToBeUpdated, document)
    }
  }

  private def findCountryCode(countryName: String): String = {
    val service: CountriesService = new CountriesService

    service.allCountries.find(_.countryName == countryName).map(_.countryCode).getOrElse(countryName)
  }

  @ChangeSet(order = "003", id = "CEDS-2247 Change origination country structure", author = "Patryk Rudnicki")
  def changeOriginationCountryStructure(db: MongoDatabase): Unit = {
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.originationCountry", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = db.getCollection(collection).find(new BasicDBObject(query)).toSeq

    documents.foreach { document =>
      val locations = document.get("locations", classOf[Map[String, String]])
      val originationCountry: String = locations.get("originationCountry")

      val queryIndexes: Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      val codeElement = ImmutableMap.of("code", originationCountry)
      val setUpdate = ImmutableMap.of("$set", ImmutableMap.of("locations.originationCountry", codeElement))
      val setUpdateObj = new BasicDBObject(setUpdate)

      db.getCollection(collection).findOneAndUpdate(objectToBeUpdated, setUpdateObj)
    }
  }

  @ChangeSet(order = "004", id = "CEDS-2247 Change destination country structure", author = "Patryk Rudnicki")
  def changeDestinationCountryStructure(db: MongoDatabase): Unit = {
    logger.info("Applying 'CEDS-2247 Change destination country structure' db migrations... ")

    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.destinationCountry", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = db.getCollection(collection).find(new BasicDBObject(query)).toSeq
    logger.info(s"[${documents.size}] documents found")

    documents.foreach { document =>
      val locations = document.get("locations", classOf[Map[String, String]])
      val destinationCountry: String = locations.get("destinationCountry")

      val queryIndexes: Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      val codeElement = ImmutableMap.of("code", destinationCountry)
      val setUpdate = ImmutableMap.of("$set", ImmutableMap.of("locations.destinationCountry", codeElement))
      val setUpdateObj = new BasicDBObject(setUpdate)
      logger.info(s"Applying [4] changes to ${document.get("id")}")
      db.getCollection(collection).findOneAndUpdate(objectToBeUpdated, setUpdateObj)
      logger.info(s"Applied [4] changes to ${document.get("id")}")
    }
    logger.info("Applying 'CEDS-2247 Change destination country structure' db migrations... Done.")
  }

  @ChangeSet(order = "005", id = "CEDS-2247 Change routing countries structure", author = "Patryk Rudnicki")
  def changeRoutingCountriesStructure(db: MongoDatabase): Unit = {
    logger.info("Applying 'CEDS-2247 Change routing countries structure... ")
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.routingCountries", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = db.getCollection(collection).find(new BasicDBObject(query)).toSeq
    logger.info(s"[${documents.size}] documents found")

    documents.foreach { document =>
      val locations = document.get("locations", classOf[Map[String, java.util.List[String]]])
      val routingCountries: Seq[String] = locations.get("routingCountries")

      val queryIndexes: Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      val updatedRoutingCountries = routingCountries.map(ImmutableMap.of("code", _)).toArray

      val setUpdate = ImmutableMap.of("$set", ImmutableMap.of("locations.routingCountries", updatedRoutingCountries))
      val setUpdateObj = new BasicDBObject(setUpdate)

      logger.info(s"Applying [5] changes to ${document.get("id")}")
      db.getCollection(collection).findOneAndUpdate(objectToBeUpdated, setUpdateObj)
      logger.info(s"Applied [5] changes to ${document.get("id")}")
    }
    logger.info("Applying 'CEDS-2247 Change routing countries structure... Done.")
  }
}
