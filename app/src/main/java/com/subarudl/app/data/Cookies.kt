package com.subarudl.app.data

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import com.subarudl.app.engine.Args
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URI

/**
 * Cookies do yt-dlp e do gallery-dl num arquivo só (cookies.txt, formato Netscape).
 * Eles chegam por três vias: o login numa janela dentro do app, um cookies.txt importado (ou aberto direto
 * pelo "Abrir com"/"Compartilhar") e o envio do computador pelo Wi-Fi ([CookieReceiver]).
 */
object Cookies {
    /** Sobe a cada mudança nos cookies, para as telas atualizarem o resumo. */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision
    private fun changed() { _revision.value++ }

    data class Imported(val cookies: Int, val sites: Int)

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
        changed()
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
        changed()
        return newLines.size
    }

    private fun isCookie(line: String) = line.isNotBlank() && !line.startsWith("# ") && line.removePrefix("#HttpOnly_").split("\t").size >= 7

    private fun domainOf(line: String) = line.removePrefix("#HttpOnly_").split("\t").firstOrNull().orEmpty().removePrefix(".").lowercase()

    /**
     * Junta um cookies.txt (formato Netscape) aos que já existem: os sites que vieram no texto substituem os
     * antigos e os outros ficam como estavam. Devolve quantos cookies e de quantos sites entraram.
     */
    fun importText(ctx: Context, text: String): Imported {
        val incoming = text.lines().map { it.trimEnd('\r') }.filter(::isCookie)
        if (incoming.isEmpty()) return Imported(0, 0)
        val domains = incoming.map(::domainOf).toSet()
        val file = Args.cookiesFile(ctx)
        val keep = if (file.exists()) file.readLines().filter { isCookie(it) && domainOf(it) !in domains } else emptyList()
        val header = listOf("# Netscape HTTP Cookie File", "# Gerado pelo Subaru DL", "")
        file.writeText((header + keep + incoming).joinToString("\n") + "\n")
        changed()
        return Imported(incoming.size, domains.size)
    }

    /** Importa um cookies.txt (exportado por uma extensão do navegador ou gerado no PC). */
    fun importFile(ctx: Context, uri: Uri): Imported {
        val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull() ?: return Imported(0, 0)
        return importText(ctx, text)
    }
}
