package com.subarudl.app.util

import java.util.Locale

fun formatBytes(b: Long?): String {
    if (b == null || b <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble()
    var i = 0
    // Troca de unidade a partir de 1000 para nunca mostrar "1024 KB" no lugar de "1,0 MB"
    while (v >= 1000 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return if (i == 0) "${v.toLong()} B" else String.format(PtBr, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

/** Números com vírgula, como se escreve no Brasil (3,2 MB/s). */
private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

fun formatSpeed(bytesPerSec: Double): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    val kb = bytesPerSec / 1024
    return if (kb >= 1024) String.format(PtBr, "%.1f MB/s", kb / 1024) else String.format(PtBr, "%.0f KB/s", kb)
}

/** Duração longa em palavras ("106 h 45 min"); abaixo de uma hora, como num relógio ("12:30"). */
fun formatLongDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "$h h" + if (m > 0) " $m min" else "" else formatDuration(sec)
}

/** Tempo que falta em palavras: "faltam 32 s", "faltam 4 min", "faltam 1 h 12 min". Vazio quando não se sabe. */
fun formatRemaining(sec: Long): String {
    if (sec < 0) return ""
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return when {
        h > 0 -> "faltam $h h" + if (m > 0) " $m min" else ""
        m > 0 -> "faltam $m min"
        else -> "faltam $sec s"
    }
}

fun formatDuration(sec: Long?): String {
    if (sec == null || sec < 0) return "—"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatEta(sec: Long): String {
    if (sec < 0) return "--:--"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatCount(n: Long?): String {
    if (n == null) return ""
    return when {
        n >= 1_000_000_000 -> String.format(Locale.US, "%.1f bi", n / 1e9)
        n >= 1_000_000 -> String.format(Locale.US, "%.1f mi", n / 1e6)
        n >= 1_000 -> String.format(Locale.US, "%.1f mil", n / 1e3)
        else -> n.toString()
    }.replace(".", ",")
}

val urlRegex = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)

fun extractLinks(text: String): List<String> = urlRegex.findAll(text).map { it.value.trimEnd('.', ',', ')', ']') }.distinct().toList()

/** "Inglês", "Português (Brasil)"… via o próprio Android, em português. */
fun languageName(code: String): String {
    val base = code.removeSuffix("-orig")
    val locale = Locale.forLanguageTag(base)
    val name = locale.getDisplayName(PtBr)
    return if (name.isBlank() || name.equals(base, true)) base else name.replaceFirstChar { it.uppercase() }
}
