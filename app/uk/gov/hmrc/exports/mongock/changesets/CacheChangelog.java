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
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import uk.gov.hmrc.exports.services.CountriesService;

import java.util.Map;

@ChangeLog
public class CacheChangelog {
    private String collection = "declarations";

    @ChangeSet(order = "001", id = "Exports DB Baseline", author = "Paulo Monteiro")
    public void dbBaseline(MongoDatabase db) {
    }

    @ChangeSet(order = "002", id = "CEDS-2231 Change country name to country code for location page", author = "Patryk Rudnicki")
    public void updateAllCountriesNameToCodesForLocationPage(MongoDatabase db) {
        Document query = new Document();
        CountriesService service = new CountriesService();

        FindIterable<Document> documents = db.getCollection(collection).find(new BasicDBObject(query));

        for (Document document : documents) {
            if (document.get("locations") != null &&
                ((Map) document.get("locations")).get("goodsLocation") != null &&
                ((Map) ((Map) document.get("locations")).get("goodsLocation")).get("country") != null) {

                String countryName = (String) ((Map) ((Map) document.get("locations")).get("goodsLocation")).get("country");

                ((Map) ((Map) document.get("locations")).get("goodsLocation")).put("country", service.findCountryCodeOrReturnCountryName(countryName));

                Map<String, String> queryIndexes = ImmutableMap.of("id", (String) document.get("id"), "eori", (String) document.get("eori"));
                BasicDBObject objectToBeUpdated = new BasicDBObject(queryIndexes);

                db.getCollection(collection).replaceOne(objectToBeUpdated, document);
            }
        }
    }
}
