package com.subarudl.app.data

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import com.subarudl.app.engine.Args
import java.net.URI

/**
 * Cookies do yt-dlp e do gallery-dl num arquivo só (cookies.txt, formato Netscape).
 * Eles chegam por duas vias: o login numa janela dentro do app, ou um cookies.txt importado.
 */
object Cookies {
    /** Sites que usam a mesma conta: ao entrar num, pegamos os cookies dos outros. */
    private val related = mapOf(
        "youtube.com" to listOf("youtube.com", "google.com", "accounts.google.com", "googlevideo.com"),
        "google.com" to listOf("youtube.com", "google.com", "accounts.google.com"),
        "instagram.com" to listOf("instagram.com"),
        "x.com" to listOf("x.com", "twitter.com"),
        "twitter.com" to listOf("x.com", "twitter.com"),
        "tiktok.com" to listOf("tiktok.com"),
        "twitch.tv" to listOf("twitch.tv"),
        "reddit.com" to listOf("reddit.com"),
        "pixiv.net" to listOf("pixiv.net"),
        "facebook.com" to listOf("facebook.com"),
    )

    data class Status(val exists: Boolean, val lines: Int, val domains: List<String>)

    fun status(ctx: Context): Status {
        val f = Args.cookiesFile(ctx)
        if (!f.exists()) return Status(false, 0, emptyList())
        val entries = f.readLines().filter { it.isNotBlank() && !it.startsWith("# ") }.mapNotNull { it.removePrefix("#HttpOnly_").split("\t").takeIf { p -> p.size >= 7 } }
        return Status(entries.isNotEmpty(), entries.size, entries.map { it[0].removePrefix(".") }.distinct().sorted())
    }

    fun clear(ctx: Context) {
        Args.cookiesFile(ctx).delete()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun registrable(host: String): String {
        val parts = host.lowercase().split('.')
        return if (parts.size <= 2) host.lowercase() else if (parts.takeLast(2).joinToString(".") in setOf("co.uk", "com.br", "com.au")) parts.takeLast(3).joinToString(".") else parts.takeLast(2).joinToString(".")
    }

    /** Lê os cookies que a janela de login guardou e grava no cookies.txt (troca os antigos dos mesmos sites). */
    fun saveFromWebView(ctx: Context, pageUrl: String): Int {
        val host = runCatching { URI(pageUrl).host }.getOrNull() ?: return 0
        val base = registrable(host)
        val hosts = (related[base] ?: listOf(base)).toMutableList().also { if (base !in it) it += base }
        val cm = CookieManager.getInstance()
        val newLines = mutableListOf<String>()
        val expiry = System.currentTimeMillis() / 1000 + 365L * 24 * 3600
        val seen = mutableSetOf<String>()
        for (h in hosts) {
            for (u in listOf("https://$h/", "https://www.$h/")) {
                val raw = cm.getCookie(u) ?: continue
                for (pair in raw.split(";")) {
                    val kv = pair.trim().split("=", limit = 2)
                    if (kv.size != 2 || kv[0].isEmpty()) continue
                    val domain = "." + h
                    if (!seen.add("$domain|${kv[0]}")) continue
                    newLines += listOf(domain, "TRUE", "/", "TRUE", expiry.toString(), kv[0], kv[1]).joinToString("\t")
                }
            }
        }
        if (newLines.isEmpty()) return 0
        val file = Args.cookiesFile(ctx)
        val replaced = hosts.map { "." + it }.toSet()
        val keep = if (file.exists()) file.readLines().filter { line ->
            val dom = line.removePrefix("#HttpOnly_").split("\t").firstOrNull() ?: ""
            line.startsWith("# ") || line.isBlank() || replaced.none { r -> dom == r || dom == r.removePrefix(".") }
        } else emptyList()
        val header = if (keep.any { it.startsWith("# Netscape") }) emptyList() else listOf("# Netscape HTTP Cookie File", "# Gerado pelo Subaru DL", "")
        file.writeText((header + keep + newLines).joinToString("\n") + "\n")
        return newLines.size
    }

    /** Importa um cookies.txt (exportado por uma extensão do navegador). Devolve quantos cookies entraram. */
    fun importFile(ctx: Context, uri: Uri): Int {
        val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return 0
        val lines = text.lines()
        val valid = lines.count { it.isNotBlank() && !it.startsWith("# ") && it.removePrefix("#HttpOnly_").split("\t").size >= 7 }
        if (valid == 0) return 0
        val out = if (lines.any { it.startsWith("# Netscape") }) text else "# Netscape HTTP Cookie File\n$text"
        Args.cookiesFile(ctx).writeText(out)
        return valid
    }
}
