# kotlin-dynamic-sql

## about the project

This project include a simple util class for generate dynamic SQLs for kotlin developers.

For example:

```
// this is the shared SQL clause
val shared_order_commons =
"""
SELECT
    o.id                             AS id,
    o.source_id                      AS sourceId,
    o.batch_number                   AS batchNumber,
FROM `order` o
WHERE 1 = 1
"""

// define a dynamic SQL by include the shared SQL clause 
// and dynamic clause generated when the parameters matched the given conditions
val get_order_list = sql {
"""
$shared_order_commons
AND o.buyer_id = :userId
${+"AND o.for_test = :forTest" If it.containsKey("forTest")}
${+"AND o.create_time BETWEEN :startTime AND :endTime" If (it.containsKey("startTime") && it.containsKey("endTime") && !it.containsKey("searchKey"))}
${+"AND o.status = :status" If (it.containsKey("status"))}
${+"AND o.order_number = :orderNumber" If (it.containsKey("searchKey"))}
ORDER BY o.create_time DESC
"""
}
```

Call the dynamic SQL by pass a parameter map to it:

```
val params = mapOf("startTime" to "2017-12-13", "status" to "CLOSED", "searchKey" to "ABC123")
val sql = get_order_list(params)
```

The generated SQL will like this:

```
SELECT
    o.id                             AS id,
    o.source_id                      AS sourceId,
    o.batch_number                   AS batchNumber,
FROM `order` o
WHERE 1 = 1
AND o.buyer_id = :userId
AND o.status = :status
AND o.order_number = :orderNumber
ORDER BY o.create_time DESC
```

## license 
Apache 2.0
