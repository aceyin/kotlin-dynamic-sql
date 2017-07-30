package kotdata.sql

import com.aceyin.dynamic.sql.SqlScriptLoader
import com.aceyin.dynamic.sql.sql

/**
 * Created by ace on 2017/7/26.
 */
class dsl_test {

    init {
        val file = File("/Users/ace/Documents/workspace/UN32/tech_base/kotun/kotdata/kotdata-jdbc/src/test/kotlin/kotdata/sql/sql.kts")
        SqlScriptLoader.loadKotlinSqlScript(file.reader())
    }

    @Test
    fun test_if_statements() {
        // 测试获取原始sql
        val s = sql["get order by id"]
        var expected = """
        SELECT o.id AS id FROM `order` o where 1=1
        #{#p.containsKey('status') ? 'and o.status = :status':''}
        #{#p.containsKey('age') and #p.containsKey('gender') ? 'and o.age = :age and o.gender = :gender':''}
        #{(#p['urgent'] == 1) == true ? 'and o.urgent = :urgent':''}
        and o.is_test = :isTest
        """
        assertEquals(expected, s)

        val y = sql["find user by condition"]
        expected = """
        select * from user
        where name = :name
        """
        assertEquals(expected, y)


        // 测试获取动态sql，但是传递的参数为空
        val x = sql.build("get order by id", emptyMap())
        expected = """SELECT o.id AS id FROM `order` o where 1=1
        and o.is_test = :isTest"""
        assertEquals(expected, x)

        // 测试 获取动态 sql，且参数完整
        val d = sql.build("get order by id", mapOf("status" to "success", "age" to "18", "gender" to "MALE", "urgent" to 1))
        expected = """SELECT o.id AS id FROM `order` o where 1=1
        and o.status = :status
        and o.age = :age and o.gender = :gender
        and o.urgent = :urgent
        and o.is_test = :isTest"""
        assertEquals(expected, d)


        // 测试 获取动态 sql，且只传部分参数
        val a = sql.build("get order by id", mapOf("status" to "success", "age" to "18", "gender" to "MALE"))
        expected = """SELECT o.id AS id FROM `order` o where 1=1
        and o.status = :status
        and o.age = :age and o.gender = :gender
        and o.is_test = :isTest"""
        assertEquals(expected, a)


        // 测试 获取动态 sql，且部分参数不匹配
        val c = sql.build("get order by id", mapOf("status" to "success", "age" to "18", "gender" to "MALE", "urgent" to 2))
        expected = """SELECT o.id AS id FROM `order` o where 1=1
        and o.status = :status
        and o.age = :age and o.gender = :gender
        and o.is_test = :isTest"""
        assertEquals(expected, c)

    }


    @Test
    fun test_when_statements() {

        val raw = sql["get product by condition"]
        var excepted = """
        select * from product p where 1=1
        #{#p.containsKey('name') ? 'and p.name like :name':''}
#{(#p['onSale']) == false ? 'and p.on_sale = :onSale':''}
#{(!(#p.containsKey('name')) and !((#p['onSale']) == false)) == true ? 'and p.status = 1':''}

        """
        assertEquals(excepted, raw)


        val param = mapOf("name" to "iPhone", "onSale" to false)
        val x = sql.build("get product by condition", param)
        excepted = """select * from product p where 1=1
        and p.name like :name
and p.on_sale = :onSale"""
        assertEquals(excepted, x)

        val p2 = mapOf("onSale" to true)
        val v = sql.build("get product by condition", p2)
        excepted = """select * from product p where 1=1
and p.status = 1"""
        assertEquals(excepted, v)
    }

    @Test
    fun spring_el_test() {
        var text = """
        select * from product p where 1=1
        #{#p.containsKey('name') ? 'and p.name like :name':''}
        #{#p['onSale'] == false ? 'and p.on_sale = :onSale':''}
        #{#p['allConditionNotMatch'] == true ? 'and p.status = 1':''}
        """

        val param = mapOf("name" to "iPhone", "onSale" to false, "allConditionNotMatch" to true)

        val s = evaluate_el(text, param)
        println(s)
    }

    fun evaluate_el(text: String, params: Map<String, Any?>): String {
        val context = StandardEvaluationContext().apply {
            this.setVariable("p", params)
        }
        return SpelExpressionParser().parseExpression(text, TemplateParserContext()).getValue(context, String::class.java)
    }
}