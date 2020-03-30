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

package uk.gov.hmrc.exports.mongock.changesets;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import uk.gov.hmrc.exports.models.Country;
import uk.gov.hmrc.exports.services.CountriesService;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

@ChangeLog
public class JavaCacheChangeLog {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final String INDEX_ID = "id";
    private final String INDEX_EORI = "eori";

    @ChangeSet(order = "001", id = "Exports DB Baseline", author = "Paulo Monteiro")
    public void dbBaseline(MongoDatabase db) {
    }

    @ChangeSet(order = "002", id = "CEDS-2231 Change country name to country code for location page", author = "Patryk Rudnicki")
    public void updateAllCountriesNameToCodesForLocationPage(MongoDatabase db) {
        LOGGER.info("Applying 'CEDS-2231 Change country name to country code for location page' db migration... ");
        getCollection(db)
                .find(and(
                        exists("locations.goodsLocation.country"),
                        type("locations.goodsLocation.country", "string"),
                        ne("locations.goodsLocation.country", ""),
                        regex("locations.goodsLocation.country","^.{3,}$" )))
                .forEach((Consumer<Document>) document -> {
                    // Retrieve indexes
                    String documentId = (String) document.get(INDEX_ID);
                    String eori = (String) document.get(INDEX_EORI);

                    //Retrieve value
                    String countryName = (String) document
                            .get("locations", Document.class)
                            .get("goodsLocation", Document.class)
                            .get("country");

                    //Update the document
                    LOGGER.info("Updating [" + countryName + "] for document Id [" + documentId + "]");
                    getCollection(db).updateOne(
                            and(eq(INDEX_ID, documentId), eq(INDEX_EORI, eori)),
                            set("locations.goodsLocation.country", getCountryCode(countryName)));
                    LOGGER.info("Updated [" + countryName + "] for document Id [" + documentId + "]");
                });
        LOGGER.info("Applying 'CEDS-2231 Change country name to country code for location page' db migration... Done.");
    }

    @ChangeSet(order = "003", id = "CEDS-2247 Change origination country structure", author = "Patryk Rudnicki")
    public void changeOriginationCountryStructure(MongoDatabase db) {
        LOGGER.info("Applying 'CEDS-2247 Change origination country structure' db migration... ");
        getCollection(db)
                .find(and(
                        exists("locations.originationCountry"),
                        type("locations.originationCountry", "string"),
                        ne("locations.originationCountry", "")))
                .forEach((Consumer<Document>) document -> {
                    // Retrieve indexes
                    String documentId = (String) document.get(INDEX_ID);
                    String eori = (String) document.get(INDEX_EORI);

                    //Retrieve value
                    String originationCountry = (String) document
                            .get("locations", Map.class)
                            .get("originationCountry");

                    //Update the document
                    LOGGER.info("Updating [" + originationCountry + "] for document Id [" + documentId + "]");
                    getCollection(db).updateOne(
                            and(eq(INDEX_ID, documentId), eq(INDEX_EORI, eori)),
                            unset("locations.originationCountry"));
                    getCollection(db).updateOne(
                            and(eq(INDEX_ID, documentId), eq(INDEX_EORI, eori)),
                            set("locations.originationCountry.code", originationCountry));
                    LOGGER.info("Updated [" + originationCountry + "] for document Id [" + documentId + "]");
                });
        LOGGER.info("Applying 'CEDS-2247 Change origination country structure' db migration... Done.");
    }

    @ChangeSet(order = "004", id = "CEDS-2247 Change destination country structure", author = "Patryk Rudnicki")
    public void changeDestinationCountryStructure(MongoDatabase db) {
        LOGGER.info("Applying 'CEDS-2247 Change destination country structure' db migrations... ");
        getCollection(db)
                .find(and(
                        exists("locations.destinationCountry"),
                        type("locations.destinationCountry", "string"),
                        ne("locations.destinationCountry", "")))
                .forEach((Consumer<Document>) document -> {
                    // Retrieve indexes
                    String documentId = (String) document.get(INDEX_ID);
                    String eori = (String) document.get(INDEX_EORI);

                    //Retrieve value
                    String destinationCountry = (String) document
                            .get("locations", Map.class)
                            .get("destinationCountry");

                    //Update the document
                    LOGGER.info("Updating [" + destinationCountry + "] for document Id [" + documentId + "]");
                    getCollection(db).updateOne(
                            and(eq(INDEX_ID, documentId), eq(INDEX_EORI, eori)),
                            unset("locations.destinationCountry"));
                    getCollection(db).updateOne(
                            and(eq(INDEX_ID, documentId), eq(INDEX_EORI, eori)),
                            set("locations.destinationCountry.code", destinationCountry));
                    LOGGER.info("Updated [" + destinationCountry + "] for document Id [" + documentId + "]");
                });
        LOGGER.info("Applying 'CEDS-2247 Change destination country structure' db migration... Done.");
    }

    @ChangeSet(order = "005", id = "CEDS-2247 Change routing countries structure", author = "Patryk Rudnicki")
    public void changeRoutingCountriesStructure(MongoDatabase db) {
        LOGGER.info("Applying 'CEDS-2247 Change routing countries structure... ");
        getCollection(db)
                .find(and(
                        not(elemMatch("locations.routingCountries", exists("code", true))),
                        exists("locations.routingCountries"),
                        type("locations.routingCountries", "array")))
                .forEach((Consumer<Document>) document -> {
                    // Retrieve indexes
                    String documentId = (String) document.get(INDEX_ID);
                    String eori = (String) document.get(INDEX_EORI);

                    //Retrieve value
                    List<String> routingCountries = (List) document
                            .get("locations", Map.class)
                            .get("routingCountries");

                    List<ImmutableMap<String, String>> codesList = routingCountries.stream()
                            .map(code -> ImmutableMap.of("code", code))
                            .collect(Collectors.toList());

                    //Update the document
                    LOGGER.info("Updating document Id [" + documentId + "]");
                    getCollection(db).updateOne(
                            and(eq(INDEX_ID, documentId), eq(INDEX_EORI, eori)),
                            set("locations.routingCountries", codesList));
                    LOGGER.info("Updated for document Id [" + documentId + "]");
                });
        LOGGER.info("Applying 'CEDS-2247 Change destination country structure' db migration... Done.");
    }

    private MongoCollection<Document> getCollection(MongoDatabase db) {
        String collectionName = "declarations";
        return db.getCollection(collectionName);
    }

    private String getCountryCode(String countryName) {
        CountriesService service = new CountriesService();
        return service.allCountriesAsJava().stream()
                .filter(country -> country.countryName().equals(countryName))
                .findFirst().map(Country::countryCode)
                .orElse(countryName);
    }
}
