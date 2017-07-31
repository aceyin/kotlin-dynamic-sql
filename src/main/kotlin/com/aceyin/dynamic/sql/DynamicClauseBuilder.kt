package com.aceyin.dynamic.sql

import java.util.*

/**
 * Created by ace on 2017/7/28.
 */

/**
 * 动态语句封装类，即根据参数值的技术算规则生成的语句。
 */
sealed class DynamicClause {

}

sealed class IfElseClause : DynamicClause() {

    /**
     * 动态SQL语句的条件部分
     */
    var condition = ConditionExpression(ConditionType.DUMMY, "")
    var ifClause: String = ""
    var elseClause: String = ""


    /**
     * 将 DynamicClause 替换成 Spring EL表达式的风格
     */
    override fun toString(): String {
        // 条件语句的 条件类型
        val type = this.condition.type
        // 如果该条件子句没有条件，则直接返回 sql 子句
        if (type == ConditionType.DUMMY) {
            return this.ifClause
        }
        return this.condition.build(ifClause, elseClause)
    }
}

sealed class ChooseClause : DynamicClause() {
    protected lateinit var conditions: LinkedList<WhenCondition>

    open infix fun When(condition: ConditionExpression): WhenClause {
        val clause = WhenClause(LinkedList<WhenCondition>())
        clause.currentCondition = WhenCondition(condition)
        return clause
    }

    override fun toString(): String {
        if (this.conditions.size == 0) return ""


        val sb = StringBuilder()
        val allConditions = mutableListOf<String>()

        var whenCondition = this.conditions.poll()
        do {
            val conditionExpression = whenCondition.condition
            val springEl = conditionExpression.build(
                    trueClause = whenCondition.text,
                    falseClause = "",
                    outerEdge = true)
            sb.appendln(springEl)
            allConditions.add(conditionExpression.conditionText)

            whenCondition = this.conditions.poll()
        } while (whenCondition != null)

        val elseText = when (this) {
            is EndClause -> {
                val x = Array(allConditions.size) {
                    "!(${allConditions[it]})"
                }.joinToString(separator = " and ")
                "#{($x) == true ? '${this.elseClause}':''}"
            }
            else -> ""
        }
        sb.appendln(elseText)

        return sb.toString()
    }
}

class WhenClause(inheritConditions: LinkedList<WhenCondition>) : ChooseClause() {
    internal lateinit var currentCondition: WhenCondition

    init {
        this.conditions = inheritConditions
    }

    infix fun then(clause: () -> String): ThenClause {
        this.currentCondition.text = clause()
        this.conditions.add(this.currentCondition)
        return ThenClause(this.conditions)
    }
}

class ThenClause(inheritConditions: LinkedList<WhenCondition>) : ChooseClause() {
    init {
        this.conditions = inheritConditions
    }

    override infix fun When(condition: ConditionExpression): WhenClause {
        val clause = WhenClause(this.conditions)
        clause.currentCondition = WhenCondition(condition)
        return clause
    }

    infix fun Else(clause: String): EndClause {
        return EndClause(clause, this.conditions)
    }
}

class EndClause(val elseClause: String, inheritConditions: LinkedList<WhenCondition>) : ChooseClause() {
    init {
        this.conditions = inheritConditions
    }
}

class IfClause(ifClause: String) : IfElseClause() {
    init {
        this.ifClause = ifClause
    }

    /**
     * 动态语句的 条件部分的 承接函数，用来接收动态语句中的 条件判断部分。
     */
    infix fun If(condition: ConditionExpression): ElseClause {
        this.condition = condition
        return ElseClause(this)
    }
}

class ElseClause(parent: IfElseClause) : IfElseClause() {
    init {
        this.ifClause = parent.ifClause
        this.elseClause = parent.elseClause
        this.condition = parent.condition
    }

    /**
     * 动态语句的 条件部分的 承接函数，用来接收动态语句中的 条件判断部分。
     */
    infix fun Else(clause: String): IfElseClause {
        this.elseClause = clause
        return this
    }
}


/**
 * 动态SQL语句的条件表达式的封装类。
 */
data class ConditionExpression(
        /**
         * 条件的类型
         */
        val type: ConditionType,
        /**
         * 条件表达式的文本
         */
        val text: Any) {

    internal var conditionText = ""
        get

    companion object CONST {
        /**
         * 动态语句中 参数引用部分的 正则模式
         * 在动态语句(例如 True 判断语句) 中，如果需要引用传入的参数的值，采用 @参数名 的方式即可
         * 在构建动态语句的时候，@参数名 会被替换成 Spring EL所需的格式。
         * 如：
         * @username 最终会被替换成 #param['username']
         */
        val PARAM_PATTERN = "@(\\w+)".toRegex()
    }


    fun build(trueClause: String,
              falseClause: String,
              outerEdge: Boolean = true,
              trueClauseQuoted: Boolean = true,
              falseClauseQuoted: Boolean = true
    ): String {

        val tc = if (trueClauseQuoted) "'$trueClause'" else trueClause
        val fc = if (falseClauseQuoted) "'$falseClause'" else falseClause

        val result = when (type) {
        // 转换 has 类型的条件判断 为 spring el 表达式
            ConditionType.HAS_PARAM -> {
                when (text) {
                    is String -> {
                        this.conditionText = "#p.containsKey('$text')"
                        "#p.containsKey('$text') ? $tc:$fc"
                    }
                    is Array<*> -> {
                        val conds = Array(text.size) {
                            "#p.containsKey('${text[it]}')"
                        }.joinToString(separator = " and ")
                        this.conditionText = conds
                        "$conds ? $tc:$fc"
                    }
                    else -> ""
                }
            }
        // hasno
            ConditionType.HAS_NO_PARAM -> {
                when (text) {
                    is String -> {
                        this.conditionText = "#p.containsKey('$text') == false"
                        "#p.containsKey('$text') == false ? $tc:$fc"
                    }
                    is Array<*> -> {
                        val conds = Array(text.size) {
                            "#p.containsKey('${text[it]}') == false"
                        }.joinToString(separator = " and ")
                        this.conditionText = conds
                        "$conds ? $tc:$fc"
                    }
                    else -> ""
                }
            }
        // is null
            ConditionType.IS_NULL -> {
                when (text) {
                    is String -> {
                        this.conditionText = "#p['$text'] == null"
                        "#p['$text'] == null ? $tc:$fc"
                    }
                    is Array<*> -> {
                        val conds = Array(text.size) {
                            "#p['${text[it]}'] == null"
                        }.joinToString(separator = " and ")
                        this.conditionText = conds
                        "$conds ? $tc:$fc"
                    }
                    else -> ""
                }
            }
        // is not null
            ConditionType.IS_NOT_NULL -> {
                when (text) {
                    is String -> {
                        this.conditionText = "#p['$text'] != null"
                        "#p['$text'] != null ? $tc:$fc"
                    }
                    is Array<*> -> {
                        val conds = Array(text.size) {
                            "#p['${text[it]}'] != null"
                        }.joinToString(separator = " and ")
                        this.conditionText = conds
                        "$conds ? $tc:$fc"
                    }
                    else -> ""
                }
            }
        // 转换 is true 类型的条件判断为 spring el 表达式
            ConditionType.IS_TRUE -> {
                if (text.toString().startsWith("@")) {
                    val con = text.toString().replace(PARAM_PATTERN, "#p['\$1']")
                    this.conditionText = "($con) == true"
                    "($con) == true ? $tc:$fc"
                } else ""
            }
        // 转换 is false 类型的条件判断为 spring el 表达式
            ConditionType.IS_FALSE -> {
                if (text.toString().startsWith("@")) {
                    val con = text.toString().replace(PARAM_PATTERN, "#p['\$1']")
                    this.conditionText = "($con) == false"
                    "($con) == false ? $tc:$fc"
                } else ""
            }
            else -> ""
        }

        return if (result.trim().isNullOrBlank()) "" else {
            if (outerEdge) "#{$result}"
            else result
        }
    }
}

data class WhenCondition(
        val condition: ConditionExpression,
        var text: String = ""
)

/**
 * 条件表达式的类型
 */
enum class ConditionType {
    DUMMY,
    // 判断 指定的参数是否存在且不为空 的表达式
    HAS_PARAM,
    // 判断 参数列表中不包含某些参数
    HAS_NO_PARAM,
    // 判断 指定的表达式结果是否为true 的表达式
    IS_TRUE,
    // 判断 指定的表达式结果是否为 false 的表达式
    IS_FALSE,
    // 判断 指定的表达式结果为 null 的表达式
    IS_NULL,
    // 判断 指定的表达式结果不为 null 的表达式
    IS_NOT_NULL
}