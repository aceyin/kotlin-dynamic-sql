package com.aceyin.dynamic.sql

import org.junit.Test
import kotlin.test.assertEquals


class ConditionalSqlTest {


    @Test
    fun test_simple_boolean_condition() {

        val sql_simple_boolean_condition = sql {
            """
SELECT *
FROM
WHERE 1=1
${+"AND aa=:aa" If (it.containsKey("aa"))}
${+"AND bb=:bb" If (it.containsKey("bb")) Else "AND bb!=:bb"}
"""
        }

        var sql = sql_simple_boolean_condition(mapOf("aa" to "aa"))
        var excepted = """
SELECT *
FROM
WHERE 1=1
 AND aa=:aa
 AND bb!=:bb """
        assertEquals(excepted.replace("\\s+".toRegex(), " "), sql.replace("\\s+".toRegex(), " "))
        ///
        sql = sql_simple_boolean_condition(mapOf("bb" to "bb"))
        excepted = """
SELECT *
FROM
WHERE 1=1

 AND bb=:bb """
        assertEquals(excepted.replace("\\s+".toRegex(), " "), sql.replace("\\s+".toRegex(), " "))

    }


    @Test
    fun test_simple_boolean_condition_with_lumbda() {
        val sql_111 = sql {
            """
SELECT *
FROM
WHERE 1=1
${+"AND aa=:aa" If { it.containsKey("aa") && it["aa"] == "aa" }}
"""
        }

        val excepted = """
SELECT *
FROM
WHERE 1=1
 AND aa=:aa """

        val actual = sql_111(mapOf("aa" to "aa"))
        assertEquals(excepted.replace("\\s+".toRegex(), " "), actual.replace("\\s+".toRegex(), " "))
    }

    @Test
    fun test_param_check_syntax() {
        val sql_param_check_condition = sql {
            """
SELECT *
FROM
WHERE 1=1
${+"AND aa=:aa" If it contains HowMany.one of arrayOf("aa")}
${+"AND bb=:bb" If it contains "bb" Else "AND bb!=:bb"}
${+"AND cc=:cc" If "cc" of it Is NullOrEmpty.NOT_NULL}
${+"AND dd=:dd" If "dd" of it Is NullOrEmpty.NULL Else "AND dd!=:dd"}
"""
        }

        var excepted = """
SELECT *
FROM
WHERE 1=1
 AND aa=:aa
 AND bb=:bb
 AND cc=:cc
 AND dd=:dd """
        var actual = sql_param_check_condition(mapOf("aa" to "aa", "bb" to "bb", "cc" to "cc", "dd" to " "))
        assertEquals(excepted.replace("\\s+".toRegex(), " "), actual.replace("\\s+".toRegex(), " "))

        //
        excepted = """
SELECT *
FROM
WHERE 1=1

 AND bb!=:bb

 AND dd!=:dd """
        actual = sql_param_check_condition(mapOf("cc" to null, "dd" to "dd"))

        assertEquals(excepted.replace("\\s+".toRegex(), " "), actual.replace("\\s+".toRegex(), " "))
    }

    @Test
    fun test_error_condition() {
        val sql_param_error = sql {
            """
SELECT *
FROM
WHERE 1=1
${+"AND aa=:aa" If it}
${+"AND cc=:cc" If "cc" of it}
"""
        }

        var excepted = """
SELECT *
FROM
WHERE 1=1


"""
        var actual = sql_param_error(mapOf("cc" to null, "aa" to "aa"))
        assertEquals(excepted.replace("\\s+".toRegex(), " "), actual.replace("\\s+".toRegex(), " "))
    }

    @Test
    fun test_value_compare() {
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

        var excepted = """
SELECT *
FROM
WHERE 1=1
 AND aa=:aa
 AND bb=:bb
 AND cc=:cc
 AND dd=:dd
 AND ee=:ee
 AND ff!=:ff
 AND gg=:gg
 AND hh=:hh
 AND ii=:ii
 AND jj=:jj
 AND kk!=:kk """
        var actual = sql_param_error(mapOf(
                "aa" to "aa",
                "bb" to 11,
                "cc" to 10,
                "dd" to 9,
                "ee" to 10,
                "ff" to 11,
                "gg" to 20,
                "hh" to "30",
                "ii" to 40,
                "jj" to 50,
                "kk" to 20
        ))
        assertEquals(excepted.replace("\\s+".toRegex(), " "), actual.replace("\\s+".toRegex(), " "))
    }
}