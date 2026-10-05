package com.subarudl.app.util

import java.util.Locale

fun formatBytes(b: Long?): String {
    if (b == null || b <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return if (i == 0) "${v.toLong()} B" else String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

fun formatSpeed(bytesPerSec: Double): String {
    if (bytesPerSec <= 0) return "0.0 KB/s"
    val kb = bytesPerSec / 1024
    return if (kb >= 1024) String.format(Locale.US, "%.1f MB/s", kb / 1024) else String.format(Locale.US, "%.1f KB/s", kb)
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
    val name = locale.getDisplayName(Locale("pt", "BR"))
    return if (name.isBlank() || name.equals(base, true)) base else name.replaceFirstChar { it.uppercase() }
}
