# kotlin-dynamic-sql

## about the project

This project include a simple util class for generate dynamic SQLs for kotlin developers.

For example:

```kotlin
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

```kotlin
val params = mapOf("startTime" to "2017-12-13", "status" to "CLOSED", "searchKey" to "ABC123")
val sql = get_order_list(params)
```

The generated SQL will like this:

```sql
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

More complex parameter check syntax example:

-  Check if parameter value is null 
```kotlin
val sql_param_check_condition = sql {
"""
SELECT *
FROM
WHERE 1=1
${+"AND aa=:aa" If it contains one of arrayOf("aa")}
${+"AND bb=:bb" If it contains "bb" Else "AND bb!=:bb"}
${+"AND cc=:cc" If "cc" of it Is NOT_NULL}
${+"AND dd=:dd" If "dd" of it Is NULL Else "AND dd!=:dd"}
"""
```
-  Compare parameter values
```kotlin
val sql_param_error = sql {
"""
SELECT *
FROM
WHERE 1=1
${+"AND aa=:aa" If "aa" of it eq "aa"}
${+"AND bb=:bb" If "bb" of it gt 10}
${+"AND cc=:cc" If "cc" of it ge 10}
${+"AND dd=:dd" If "dd" of it lt 10}
${+"AND ee=:ee" If "ee" of it le 10}
${+"AND ff=:ff" If "ff" of it le 10 Else "AND ff!=:ff"}
${+"AND gg=:gg" If "gg" of it inn arrayOf(10, 20, 30) Else "AND gg!=:gg"}
${+"AND hh=:hh" If "hh" of it inn listOf("30", "40", "50") Else "AND hh!=:hh"}
${+"AND ii=:ii" If "ii" of it inn mapOf("30" to 30, "40" to 40, "50" to 50) Else "AND ii!=:ii"}
${+"AND jj=:jj" If "jj" of it nin arrayOf(10, 20, 30) Else "AND jj!=:jj"}
${+"AND kk=:kk" If "kk" of it nin arrayOf(10, 20, 30) Else "AND kk!=:kk"}
"""
}
```

## license 
Apache 2.0
