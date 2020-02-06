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

package uk.gov.hmrc.exports.mongobee.changesets;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.Document;

@ChangeLog
public class DeclarationsChangelog {
    private String collection = "declarations";

    @ChangeSet(order = "001", id = "CEDS-2111 Change declaration types to APPLES", author = "Paulo Monteiro")
    public void updateAllDeclarationTypeToApples(DB db) {
        Document query = new Document("type", "STANDARD");
        Document update = new Document("$set", new Document("additionalDeclarationType", "APPLES"));
        db.getCollection(collection).update(new BasicDBObject(query), new BasicDBObject(update), false, true);
    }

    @ChangeSet(order = "002", id = "CEDS-2111 Move LRN to root of document", author = "Paulo Monteiro")
    public void moveLRNToRootOfDocument(DB db) {

        Document query = new Document("consignmentReferences.lrn", new Document("$exists", true));
        DBCursor cursor = db.getCollection(collection).find(new BasicDBObject(query));
        while (cursor.hasNext()) {
            DBObject dbObject = cursor.next();

            BasicDBObject consignmentReferences = (BasicDBObject) dbObject.get("consignmentReferences");
            Document updateSet = new Document("$set", new Document("lrn", consignmentReferences).get("lrn"));
            db.getCollection(collection).update(new BasicDBObject(query), new BasicDBObject(updateSet), false, true);

            Document updateUnset = new Document("$unset", new Document("consignmentReferences.lrn", null));
            db.getCollection(collection).update(new BasicDBObject(query), new BasicDBObject(updateUnset), false, true);
        }
    }
}