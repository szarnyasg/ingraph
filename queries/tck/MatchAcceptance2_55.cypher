MATCH (a)-[r]->(b)
WITH a, r, b, count(*) AS c
ORDER BY c
MATCH (a)-[r]->(b)
RETURN r AS rel
ORDER BY rel.id
