

function buildUpdateOneOperation(declaration) {
    var decId = declaration['_id'];
    return { updateOne: {
        "filter": { "_id": decId },
        "update": { $set: { "consignmentReferences.lrn": "NEWLRN1234567" }}
    }};
}

var eori = "GB072071145000";
var cursor = db.getCollection('declarations').find({
    $and: [
        { "eori": { $eq: eori }},
        { "consignmentReferences.lrn": { $eq: "QSLRN6499100" }}
    ]
});

var updateOperations = cursor.map(buildUpdateOneOperation);

db.getCollection('declarations').bulkWrite(updateOperations);


