package com.aceyin.dynamic.sql

import com.aceyin.dynamic.sql.*
import org.slf4j.LoggerFactory
import org.springframework.expression.common.TemplateParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.util.*


/**
 * kotlin script style SQL builder.
 *
 * By using this class, we can define SQL statements in kotlin script file(.kts).
 * Here is the example:
 *
 * <pre>
 * sql {
 *    val common_fields = """ SELECT * from orders o """
 *
 *    "search order by conditions" {
 *        """
 *        $common_fields
 *        WHERE 1=1
 *        ${+"and o.status=:status" If has("status")}
 *        ${+"and o.status=:status" If True("@status='PAYED' ")}
 *        and o.create_time >= :startTime
 *        and o.create_time<= :endTime
 *        """
 *    }
 * }
 * </pre>
 */
object SqlScriptBuilder {
    private val LOG = LoggerFactory.getLogger(SqlScriptBuilder::class.java)

    private val springElParser = SpelExpressionParser()
    private val EMPTY_LINE_REGEX = "^\\s*\$(\n|\r\n)".toRegex(RegexOption.MULTILINE)
    /**
     * the raw sql statement cache.
     * key is the name of sql statement, value is the sql statement.
     */
    private val rawSqlCache = mutableMapOf<String, String>()

    /**
     * get a sql by the specified key.
     * @param key String, the key of the sql statement.
     * @return String?, the raw SQL statement without dynamic sql generation.
     */
    operator fun get(key: String): String? {
        return rawSqlCache[key]
    }

    internal fun put(key: String, value: String) {
        if (rawSqlCache.containsKey(key)) {
            if (LOG.isWarnEnabled) {
                LOG.warn("There already SQL exists with key: $key, overwrite it with new value")
            }
        }
        rawSqlCache[key] = value
    }

    /**
     * Define an invoke operator function so that we can use the brace ({}) syntax to define all the sqls.
     * @param statementsDefinition ()->Unit, the sql statements body
     */
    operator fun invoke(statementsDefinition: () -> Unit) {
        statementsDefinition()
    }

    /**
     * 为动态构建SQL语句添加一个条件，用来检查传入的参数中是否包含指定的值，且这些值不为空
     * @param parameterNames Array<String>, the parameter name
     *                      例如，要完成：
     *                      如果 传入的参数里面 orderName 不为空，则增加 and order_name like %orderName% 的逻辑，
     *                      可以这么写条件语句:
     *                      has("orderName")
     * @return ConditionExpression, a ConditionExpression object
     */
    fun has(vararg parameterNames: String): ConditionExpression {
        return ConditionExpression(ConditionType.HAS_PARAM, parameterNames)
    }

    fun hasno(vararg parameterNames: String): ConditionExpression {
        return ConditionExpression(ConditionType.HAS_NO_PARAM, parameterNames)
    }

    /**
     * 为动态构建SQL语句添加一个条件，当该条件的计算结果为 true 的时候，就往SQL语句中增加指定的语句。
     * @param condition String 判断条件语句。该语句需要符合 Spring EL表达式的语法。
     *                         如果需要引用传入的参数，则采用 @param_name 的方式即可。
     *                         例如，要完成：
     *                         如果传入的参数中，isTestUser 参数的值为 1 则往SQL语句中增加 "and test_user=1"的逻辑，
     *                         可以这么写相应的条件语句部分：
     *                         True("@isTestUser == 1")
     * @return ConditionExpression
     */
    fun True(condition: String): ConditionExpression {
        return ConditionExpression(ConditionType.IS_TRUE, condition)
    }

    fun False(condition: String): ConditionExpression {
        return ConditionExpression(ConditionType.IS_FALSE, condition)
    }

    /**
     * 判断参数是否为空
     */
    fun Null(vararg parameterNames: String): ConditionExpression {
        return ConditionExpression(ConditionType.IS_NULL, parameterNames)
    }

    /**
     * 判断参数是否为空
     */
    fun Notnull(vararg parameterNames: String): ConditionExpression {
        return ConditionExpression(ConditionType.IS_NOT_NULL, parameterNames)
    }


    /**
     * 使用指定的参数构建一条动态生成的SQL语句。
     * 实际进行条件判断业务的过程是使用 Spring EL表达式对动态语句部分进行计算。
     * @param sqlName String, SQL 语句的名称
     * @param params Map<String,Any?>, SQL语句的参数
     * @return String? 如果构建成功则返回最终的SQL语句，否则返回 null
     */
    fun build(sqlName: String, params: Map<String, Any?>): String? {
        val raw = sql[sqlName] ?: return null

        val context = StandardEvaluationContext().apply {
            this.setVariable("p", params)
        }
        val result = springElParser.parseExpression(raw, TemplateParserContext()).getValue(context, StringBuilder::class.java)
        return result?.replace(EMPTY_LINE_REGEX, "")?.trim()
    }
}

/**
 * 全局变量，用来在 kts 文件中定义 sql
 */
val sql = SqlScriptBuilder


/**
 * 重载 invoke 方法，以便在 String 中定义 SQL 语句
 */
operator fun String.invoke(body: () -> String) {
    sql.put(this, body())
}

/**
 * 重载 String 的 一元+ 函数
 */
operator fun String.unaryPlus(): IfClause {
    return IfClause(this)
}

operator fun ChooseClause.unaryPlus(): ChooseClause {
    return this
}

val choose = WhenClause(LinkedList<WhenCondition>())