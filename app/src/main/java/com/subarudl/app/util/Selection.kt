package com.subarudl.app.util

/** "1,3-5,8-" → {1,3,4,5,8..total}. Vazio = todos; "0" = nenhum. Números negativos contam do fim. */
fun expandSelection(spec: String, total: Int): Set<Int> {
    val t = spec.trim()
    if (t.isEmpty()) return (1..total).toSet()
    if (t == "0") return emptySet()
    val out = mutableSetOf<Int>()
    fun at(n: String, fallback: Int): Int {
        if (n.isEmpty()) return fallback
        val v = n.toIntOrNull() ?: return fallback
        return if (v < 0) total + v + 1 else v
    }
    val dashRange = Regex("^(\\d*)-(\\d*)$")
    val colonRange = Regex("^(-?\\d*):(-?\\d*)$")
    for (part in t.split(",")) {
        val p = part.trim()
        val dash = dashRange.matchEntire(p)
        val colon = colonRange.matchEntire(p)
        when {
            dash != null -> {
                val a = at(dash.groupValues[1], 1)
                val b = at(dash.groupValues[2], total)
                for (n in maxOf(1, a)..minOf(total, b)) out += n
            }
            colon != null -> {
                val a = at(colon.groupValues[1], 1)
                val b = at(colon.groupValues[2], total)
                for (n in maxOf(1, a)..minOf(total, b)) out += n
            }
            p.toIntOrNull() != null -> out += at(p, 1)
        }
    }
    return out.filter { it in 1..total }.toSet()
}

/** Conjunto de itens → texto para o yt-dlp (-I). Todos = vazio; nenhum = "0". */
fun selectionSpec(set: Set<Int>, total: Int): String = when {
    set.isEmpty() -> "0"
    set.size >= total -> ""
    else -> compactRanges(set)
}

/** {1,2,3,5,7,8} → "1-3,5,7-8" */
fun compactRanges(set: Set<Int>): String {
    val s = set.sorted()
    if (s.isEmpty()) return "0"
    val out = mutableListOf<String>()
    var start = s.first()
    var prev = start
    for (n in s.drop(1)) {
        if (n == prev + 1) { prev = n; continue }
        out += if (start == prev) "$start" else "$start-$prev"
        start = n
        prev = n
    }
    out += if (start == prev) "$start" else "$start-$prev"
    return out.joinToString(",")
}
