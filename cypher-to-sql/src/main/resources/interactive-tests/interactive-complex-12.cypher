MATCH (:Person {id:19791209300143})-[:KNOWS]-(friend:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:REPLY_OF]->(:Post)-[:HAS_TAG]->(tag:Tag),
  (tag)-[:HAS_TYPE]->(tagClass:TagClass)-[:IS_SUBCLASS_OF*0..]->(baseTagClass:TagClass)
WHERE tagClass.name = 'BasketballPlayer' OR baseTagClass.name = 'BasketballPlayer'
RETURN
  friend.id AS personId,
  friend.firstName AS personFirstName,
  friend.lastName AS personLastName,
  collect(DISTINCT tag.name) AS tagNames,
  count(DISTINCT comment) AS replyCount
ORDER BY replyCount DESC, toInteger(personId) ASC
LIMIT 20
