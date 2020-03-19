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
import uk.gov.hmrc.exports.services.CountriesService

@ChangeLog
class CacheChangeLog {

  private val collection = "declarations"

  @ChangeSet(order = "001", id = "Exports DB Baseline", author = "Paulo Monteiro")
  def dbBaseline(db: MongoDatabase): Unit = {}

  @ChangeSet(order = "002", id = "CEDS-2231 Change country name to country code for location page", author = "Patryk Rudnicki")
  def updateAllCountriesNameToCodesForLocationPage(db: MongoDatabase): Unit = {
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.goodsLocation.country", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    import scala.collection.JavaConversions._

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
}
