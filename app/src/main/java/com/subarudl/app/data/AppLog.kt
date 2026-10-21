package com.subarudl.app.data

import android.util.Log
import com.subarudl.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel { INFO, WARN, ERROR, CMD }

data class LogLine(val time: Long, val level: LogLevel, val text: String, val itemId: String? = null)

/** Registro de tudo que o motor faz (tela Logs). Fica só na memória, com as últimas linhas. */
object AppLog {
    private const val MAX = 1500
    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines.asStateFlow()

    @Synchronized
    fun add(level: LogLevel, text: String, itemId: String? = null) {
        // No logcat só no build de testes: no app publicado o registro fica só na tela Registro
        if (BuildConfig.DEBUG) Log.println(
            when (level) {
                LogLevel.ERROR -> Log.ERROR
                LogLevel.WARN -> Log.WARN
                else -> Log.INFO
            },
            "SUBARU",
            text.take(400),
        )
        val novas = _lines.value + LogLine(System.currentTimeMillis(), level, text, itemId)
        _lines.value = if (novas.size > MAX) novas.takeLast(MAX) else novas
    }

    fun i(text: String, itemId: String? = null) = add(LogLevel.INFO, text, itemId)
    fun w(text: String, itemId: String? = null) = add(LogLevel.WARN, text, itemId)
    fun e(text: String, itemId: String? = null) = add(LogLevel.ERROR, text, itemId)
    fun cmd(text: String, itemId: String? = null) = add(LogLevel.CMD, text, itemId)

    fun clear() {
        _lines.value = emptyList()
    }

    fun asText(): String = _lines.value.joinToString("\n") { "[${it.level}] ${it.text}" }
}
