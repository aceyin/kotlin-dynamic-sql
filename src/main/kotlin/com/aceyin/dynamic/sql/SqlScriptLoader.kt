package com.aceyin.dynamic.sql

import org.jetbrains.kotlin.codegen.context.ScriptContext
import org.slf4j.LoggerFactory
import java.io.Reader
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * Created by ace on 2017/7/28.
 */
object SqlScriptLoader {
    private val LOG = LoggerFactory.getLogger(SqlScriptLoader::class.java)
    /**
     * 读取 sql kts 文件
     */
    fun loadKotlinSqlScript(reader: Reader) {
        try {
            val bindings = SimpleBindings()
            val engine = ScriptEngineManager().getEngineByName("kotlin")
            engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
            engine.eval(reader)
        } catch (e: Exception) {
            LOG.error("Error while load SQL definition script", e)
            throw e
        }
    }
}