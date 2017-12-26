package com.aceyin.dynamic.sql

import net.sf.cglib.beans.BeanMap
import org.slf4j.LoggerFactory


/**
 * Overloaded String unary + function to create a ConditionSelector instance.
 */
operator fun String.unaryPlus(): ConditionSelector {
    return ConditionSelector(this)
}

class ConditionSelector(
        /** Conditional expressions are satisfied when appending statements in SQL */
        private val clause: String) {

    /**
     * Simple If statement, if the bool value within the argument is true, append the sql clause in the statement
     */
    infix fun If(bool: Boolean) = SimpleBooleanCondition(bool, clause)

    infix fun If(body: () -> Boolean) = SimpleBooleanCondition(body(), clause)

    /**
     * Whether the parameter list contains the given parameters, sql check the parameters, if the conditions are appended to the given sql clause
     */
    infix fun If(param: Map<String, Any?>) = ParamContainsCondition(param, clause)

    /**
     * Parameter value check
     */
    infix fun If(paramName: String) = ParamValueCheckCondition(paramName, clause)
}


sealed class Condition(val paramList: Map<String, Any?>, val clause: String)


/**
 * Simple bool expression conditions.
 */
class SimpleBooleanCondition(val bool: Boolean, clause: String) : Condition(emptyMap(), clause) {

    infix fun Else(elseClause: String): String = if (this.bool) " $clause " else " $elseClause "

    override fun toString(): String = if (this.bool) " $clause " else ""
}

/**
 * Check whether the parameter list contains a parameter
 */
class ParamContainsCondition(param: Map<String, Any?>, clause: String) : Condition(param, clause) {
    private val logger = LoggerFactory.getLogger(ParamContainsCondition::class.java)
    /**
     * Check whether the parameter list contains a parameter
     */
    infix fun contains(paramName: String) = SimpleContainsCondition(paramName, paramList, clause)

    infix fun contains(howMany: HowMany) = ComplexContainsConditionStarter(paramList, clause, howMany)

    /**
     * If the statement in SQL is not finished, no clause is appended, and an error log is printed
     */
    override fun toString(): String {
        logger.error("Incomplete SQL statement clause：clause=$clause, Program call stack:")
        val stacktrace = Thread.currentThread().stackTrace
        val msg = StringBuilder()
        stacktrace.forEach {
            msg.appendln("\t\t: ${it.className}:${it.methodName}, Line: ${it.lineNumber}")
        }
        logger.error(msg.toString())
        return ""
    }
}

enum class HowMany {
    one, all
}

/**
 * Parameter Check Expression Conditions
 */
class ParamValueCheckCondition(val paramName: String, clause: String) : Condition(emptyMap(), clause) {
    private val logger = LoggerFactory.getLogger(ParamValueCheckCondition::class.java)

    /**
     *  Check the list of parameters, the value of a parameter meets the expected conditions
     */
    infix fun of(paramList: Map<String, Any?>): ParamCheckerStarter = ParamCheckerStarter(paramName, paramList, clause)

    /**
     * If the statement in SQL is not finished, no clause is appended, and an error log is printed
     */
    override fun toString(): String {
        logger.error("Incomplete SQL statement clause：clause=$clause, Program call stack:")
        val stacktrace = Thread.currentThread().stackTrace
        val msg = StringBuilder()
        stacktrace.forEach {
            msg.appendln("\t\t: ${it.className}:${it.methodName}, Line: ${it.lineNumber}")
        }
        logger.error(msg.toString())
        return ""
    }
}

class SimpleContainsCondition(val paramName: String, paramList: Map<String, Any?>, clause: String) : Condition(paramList, clause) {
    infix fun Else(elseClause: String): String = if (paramList.containsKey(paramName)) " $clause " else " $elseClause "
    /**
     * If the statement in SQL, there is no Else part that the else part does not append any statement
     */
    override fun toString(): String = if (paramList.containsKey(paramName)) " $clause " else ""
}


class ComplexContainsConditionStarter(paramList: Map<String, Any?>, clause: String, val howMany: HowMany) : Condition(paramList, clause) {
    private val logger = LoggerFactory.getLogger(ComplexContainsConditionStarter::class.java)

    infix fun of(paramNames: Array<String>): ComplexContainsConditionEnder {
        val sets = HashSet<String>()
        paramNames.forEach { sets.add(it) }
        return ComplexContainsConditionEnder(paramList, clause, howMany, sets)
    }

    /**
     * If the statement in SQL, there is no Else part that the else part does not append any statement
     */
    override fun toString(): String {
        logger.error("Incomplete SQL statement clause：clause=$clause, Program call stack:")
        val stacktrace = Thread.currentThread().stackTrace
        val msg = StringBuilder()
        stacktrace.forEach {
            msg.appendln("\t\t: ${it.className}:${it.methodName}, Line: ${it.lineNumber}")
        }
        logger.error(msg.toString())
        return ""
    }
}

class ComplexContainsConditionEnder(paramList: Map<String, Any?>, clause: String, val howMany: HowMany, val paramNames: Set<String>) : Condition(paramList, clause) {
    private var elseClause: String = ""
    /**
     * If there is no Else part, the sql clause of the previous part is output directly
     */
    override fun toString(): String {
        return buildClause()
    }

    /**
     * If the parameters are not satisfied, add another specified sql clause
     */
    infix fun Else(elseClause: String): String {
        this.elseClause = elseClause
        return buildClause()
    }

    private fun buildClause(): String {
        var contains = false
        when (howMany) {
            HowMany.one -> {
                paramNames.forEach {
                    if (paramList.containsKey(it)) contains = true
                }
            }
            HowMany.all -> contains = paramList.keys.containsAll(paramNames)
        }
        return if (contains) " $clause " else " $elseClause "
    }
}

/**
 * Parameter checker
 */
class ParamCheckerStarter(private val paramName: String,
                          private val paramList: Map<String, Any?>,
                          private val clause: String) {
    private val logger = LoggerFactory.getLogger(ParamCheckerStarter::class.java)

    /**
     * If the SQL statement is not finished, do not append any clause
     */
    override fun toString(): String {
        logger.error("Incomplete SQL statement clause：clause=$clause, Program call stack:")
        val stacktrace = Thread.currentThread().stackTrace
        val msg = StringBuilder()
        stacktrace.forEach {
            msg.appendln("\t\t: ${it.className}:${it.methodName}, Line: ${it.lineNumber}")
        }
        logger.error(msg.toString())
        return ""
    }

    /**
     * Check the parameters are empty or non-empty, empty array / list / map
     */
    infix fun Is(status: NullOrEmpty) = NullOrEmptyParamChecker(paramName, status, paramList, clause)

    /**
     * Determine whether the parameter value is equal to the given value
     */
    infix fun eq(givenValue: Any?) = ParamValueCompareChecker(paramName, ValueComparator.EQ, givenValue, paramList, clause)

    /**
     * Check whether the parameter value is greater than the given value
     */
    infix fun gt(givenValue: Any?) = ParamValueCompareChecker(paramName, ValueComparator.GT, givenValue, paramList, clause)

    /**
     * Determine whether the parameter value is greater than or equal to the given value
     */
    infix fun ge(givenValue: Any?) = ParamValueCompareChecker(paramName, ValueComparator.GE, givenValue, paramList, clause)

    /**
     * Determine whether the parameter value is less than the given value
     */
    infix fun lt(givenValue: Any?) = ParamValueCompareChecker(paramName, ValueComparator.LT, givenValue, paramList, clause)

    /**
     * Determine whether the parameter value is less than or equal to the given value
     */
    infix fun le(givenValue: Any?) = ParamValueCompareChecker(paramName, ValueComparator.LE, givenValue, paramList, clause)

    /**
     * Determine if the parameter is in the given list
     */
    infix fun inn(givenValues: Collection<out Any>) = ParamValueCompareChecker(paramName, ValueComparator.IN, givenValues, paramList, clause)

    /**
     * Determine if the parameter is in the given Map
     */
    infix fun inn(givenValues: Map<out Any, out Any>) = ParamValueCompareChecker(paramName, ValueComparator.IN, givenValues, paramList, clause)

    /**
     * Determine if the argument is in the given array
     */
    infix fun inn(givenValues: Array<out Any>) = ParamValueCompareChecker(paramName, ValueComparator.IN, givenValues, paramList, clause)


    /**
     * Determine if the parameter is in the given list
     */
    infix fun nin(givenValues: Collection<out Any>) = ParamValueCompareChecker(paramName, ValueComparator.NIN, givenValues, paramList, clause)

    /**
     * Determine if the parameter is in the given Map
     */
    infix fun nin(givenValues: Map<out Any, out Any>) = ParamValueCompareChecker(paramName, ValueComparator.NIN, givenValues, paramList, clause)

    /**
     * Determine if the argument is in the given array
     */
    infix fun nin(givenValues: Array<out Any>) = ParamValueCompareChecker(paramName, ValueComparator.NIN, givenValues, paramList, clause)
}

/**
 * Null or empty (empty array,list or map) value checker.
 */
class NullOrEmptyParamChecker(private val paramName: String,
                              private val status: NullOrEmpty,
                              private val paramList: Map<String, Any?>,
                              private val clause: String) {
    private var elseClause: String = ""
    /**
     * Override toString method so that if there is no 'Else' part, the checker can return an empty ""
     */
    override fun toString(): String {
        return buildClause()
    }

    /**
     * Else part of parameter value comparator.
     */
    infix fun Else(elseClause: String): String {
        this.elseClause = elseClause
        return buildClause()
    }

    private fun buildClause(): String {
        val paramValue = paramList[paramName]
        return when (status) {
            NullOrEmpty.NULL -> {
                if (paramValue == null) " $clause " else {
                    when (paramValue) {
                    // empty string will be treated as null
                        is CharSequence -> if (paramValue.isNullOrBlank()) " $clause " else " $elseClause "
                        else -> " $elseClause "
                    }
                }
            }
            NullOrEmpty.NOT_NULL -> {
                if (paramValue != null) {
                    when (paramValue) {
                    // empty string will be treated as null
                        is CharSequence -> if (paramValue.isNullOrBlank()) " $elseClause " else " $clause "
                        else -> " $clause "
                    }
                } else " $elseClause "
            }
            NullOrEmpty.EMPTY -> {
                when (paramValue) {
                    is Collection<*> -> if (paramValue.size == 0) " $clause " else " $elseClause "
                    is Map<*, *> -> if (paramValue.isEmpty()) " $clause " else " $elseClause "
                    is Array<*> -> if (paramValue.size == 0) " $clause " else " $elseClause "
                    else -> ""
                }
            }
            NullOrEmpty.NOT_EMPTY -> {
                when (paramValue) {
                    is Collection<*> -> if (paramValue.size == 0) " $elseClause " else " $clause "
                    is Map<*, *> -> if (paramValue.isEmpty()) " $elseClause " else " $clause "
                    is Array<*> -> if (paramValue.size == 0) " $elseClause " else " $clause "
                    else -> ""
                }
            }
        }
    }
}

/**
 * Parameter value comparator.
 *
 */
class ParamValueCompareChecker(private val paramName: String,
                               private val comparator: ValueComparator,
                               private val givenValue: Any?,
                               private val paramList: Map<String, Any?>,
                               private val clause: String) {

    /**
     * Override toString method so that if there is no 'Else' part, the checker can return an empty ""
     */
    override fun toString(): String {
        // 如果参数列表里面取出来的值为空，则直接
        val paramValue = paramList[paramName]
        val matches = comparator.compare(paramValue!!, givenValue)
        return if (matches) " $clause " else ""
    }

    /**
     * Else part of parameter value comparator.
     */
    infix fun Else(elseClause: String): String {
        val paramValue = paramList[paramName]
        val matches = comparator.compare(paramValue!!, givenValue)
        return if (matches) " $clause " else " $elseClause "
    }
}

/**
 * Parameter value comparator, for check the parameter value.
 */
enum class ValueComparator(val compare: (paramValue: Any?, givenValue: Any?) -> Boolean) {
    // equals to  given value
    EQ({ paramValue, givenValue -> paramValue == givenValue }),
    // not equals to  given value
    NEQ({ paramValue, givenValue -> paramValue != givenValue }),
    // greater  given value
    GT({ paramValue, givenValue ->
        if (paramValue == null && givenValue == null) false
        else if (paramValue == null) false
        else if (givenValue == null) true
        else {
            if (paramValue::class != givenValue::class) false
            else if (paramValue is Comparable<*> && givenValue is Comparable<*>) {
                (paramValue as kotlin.Comparable<Any>) > (givenValue as kotlin.Comparable<Any>)
            } else false
        }
    }),
    // greater or equals to  given value
    GE({ paramValue, givenValue ->
        if (paramValue == null && givenValue == null) true
        else if (paramValue == null) false
        else if (givenValue == null) true
        else {
            if (paramValue::class != givenValue::class) false
            else if (paramValue is kotlin.Comparable<*> && givenValue is kotlin.Comparable<*>) {
                (paramValue as kotlin.Comparable<kotlin.Any>) >= (givenValue as kotlin.Comparable<kotlin.Any>)
            } else false
        }
    }),
    // less than given value
    LT({ paramValue, givenValue ->
        if (paramValue == null && givenValue == null) false
        else if (paramValue == null) true
        else if (givenValue == null) false
        else {
            if (paramValue::class != givenValue::class) false
            else if (paramValue is kotlin.Comparable<*> && givenValue is kotlin.Comparable<*>) {
                (paramValue as kotlin.Comparable<kotlin.Any>) < (givenValue as kotlin.Comparable<kotlin.Any>)
            } else false
        }
    }),
    // less or equals than given value
    LE({ paramValue, givenValue ->
        if (paramValue == null && givenValue == null) true
        else if (paramValue == null) true
        else if (givenValue == null) false
        else {
            if (paramValue::class != givenValue::class) false
            else if (paramValue is kotlin.Comparable<*> && givenValue is kotlin.Comparable<*>) {
                (paramValue as kotlin.Comparable<kotlin.Any>) <= (givenValue as kotlin.Comparable<kotlin.Any>)
            } else false
        }
    }),
    // parameter value is in list/array/map
    IN({ paramValue, givenValue ->
        if (paramValue == null && givenValue == null) false
        else if (paramValue == null) false
        else if (givenValue == null) false
        else {
            when (givenValue) {
                is Collection<*> -> givenValue.contains(paramValue)
                is Map<*, *> -> givenValue.containsValue(paramValue)
                is Array<*> -> givenValue.contains(paramValue)
                else -> false
            }
        }
    }),
    // parameter value not in list/array/map
    NIN({ paramValue, givenValue ->
        if (paramValue == null && givenValue == null) false
        else if (paramValue == null) false
        else if (givenValue == null) false
        else {
            when (givenValue) {
                is Collection<*> -> !givenValue.contains(paramValue)
                is Map<*, *> -> !givenValue.containsValue(paramValue)
                is Array<*> -> !givenValue.contains(paramValue)
                else -> false
            }
        }
    })
    ;
}

enum class NullOrEmpty {
    /** check if a parameter is null */
    NULL,
    /** check if a parameter is NOT null */
    NOT_NULL,
    /** check if a parameter is an empty list/array/map */
    EMPTY,
    /** check if a parameter is NOT an empty list/array/map */
    NOT_EMPTY
}


class ConditionalSqlBuilder(val body: (param: Map<String, Any?>) -> String) {
    operator fun invoke(param: Any? = null): String {
        return if (param == null) body(emptyMap())
        else {
            when (param) {
                is Map<*, *> -> body(param as Map<String, Any?>)
                else -> {
                    body(Beans.map(param))
                }
            }
        }
    }
}

object Beans {
    /**
     * Convert a bean to Map
     * @param bean Any, the bean to be converted.
     * @return Map<String,Any>, a map which key is the bean's member name and the value is the bean's member value.
     */
    fun map(bean: Any): Map<String, Any?> {
        return BeanMap.create(bean) as Map<String, Any?>
    }
}

fun sql(body: (param: Map<String, Any?>) -> String) = ConditionalSqlBuilder(body)

