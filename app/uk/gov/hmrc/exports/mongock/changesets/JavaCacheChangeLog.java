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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@ChangeLog
public class JavaCacheChangeLog {

    private String collection = "declarations";
    private Logger logger = Logger.getLogger("uk.gov.hmrc.exports.mongock.changesets.JavaCacheChangeLog");

    @ChangeSet(order = "001", id = "CEDS-2247 (Java) Change destination country structure", author = "Paulo Monteiro")
    public void changeDestinationCountryStructure(MongoDatabase db) {
        logger.info("Applying 'CEDS-2247 Change destination country structure' db migrations... ");

        MongoCollection<Document> collection = db.getCollection(this.collection);
        collection
                .find(and(
                        exists("locations.destinationCountry"),
                        type("locations.destinationCountry", "string"),
                        ne("locations.destinationCountry", "")))
                .forEach((Consumer<Document>) document -> {
                    // Retrieve documentId
                    String documentId = (String) document.get("id");

                    //Retrieve value
                    String destinationCountry = (String) document
                            .get("locations", Map.class)
                            .get("destinationCountry");

                    //Update the document
                    logger.info("Updating [" + destinationCountry + "] for document Id [" + documentId + "]");
                    db.getCollection(this.collection).updateOne(eq("id", documentId),unset("locations.destinationCountry"));
                    db.getCollection(this.collection).updateOne(eq("id", documentId),set("locations.destinationCountry.code", destinationCountry));
                    logger.info("Updated [" + destinationCountry + "] for document Id [" + documentId + "]");
                });
        logger.info("Applying 'CEDS-2247 Change destination country structure' db migrations... Done.");
    }
}
