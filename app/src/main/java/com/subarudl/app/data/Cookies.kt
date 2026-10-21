package com.subarudl.app.data

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import com.subarudl.app.engine.Args
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI

/**
 * Cookies do yt-dlp e do gallery-dl num arquivo só (cookies.txt, formato Netscape).
 * Eles chegam por três vias: o login numa janela dentro do app, um cookies.txt importado (ou aberto direto
 * pelo "Abrir com"/"Compartilhar") e o envio do computador pelo Wi-Fi ([CookieReceiver]).
 */
object Cookies {
    /** Um cookies.txt maior que isso não é um cookies.txt. */
    const val MAX_FILE = 2 * 1024 * 1024

    /** Sobe a cada mudança nos cookies, para as telas atualizarem o resumo. */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision
    private fun changed() { _revision.value++ }

    data class Imported(val cookies: Int, val sites: Int)

    /** Cookies lidos de um arquivo e ainda não gravados (a pessoa confirma antes, quando vêm de outro app). */
    class Parsed(val lines: List<String>, val domains: List<String>, val ignored: Int) {
        val count get() = lines.size
    }

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

    /** Traz o cookies.txt da pasta antiga (até a 1.5.0) para a pasta sem backup. Roda uma vez, ao abrir o app. */
    fun migrate(ctx: Context) {
        val old = Args.oldCookiesFile(ctx)
        if (!old.exists()) return
        val new = Args.cookiesFile(ctx)
        runCatching {
            if (!new.exists()) {
                new.parentFile?.mkdirs()
                if (!old.renameTo(new)) old.copyTo(new)
            }
        }.onFailure { AppLog.w("Não consegui mudar o cookies.txt de pasta: ${it.message}") }
        if (new.exists()) old.delete()
    }

    fun status(ctx: Context): Status {
        val f = Args.cookiesFile(ctx)
        if (!f.exists()) return Status(false, 0, emptyList())
        val entries = f.readLines().filter { it.isNotBlank() && !it.startsWith("# ") }.mapNotNull { it.removePrefix("#HttpOnly_").split("\t").takeIf { p -> p.size >= 7 } }
        return Status(entries.isNotEmpty(), entries.size, entries.map { it[0].removePrefix(".") }.distinct().sorted())
    }

    fun clear(ctx: Context) {
        Args.cookiesFile(ctx).delete()
        Args.oldCookiesFile(ctx).delete()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        changed()
    }

    /**
     * O domínio que se pode registrar ("youtube.com", "bbc.co.uk"), pela lista pública de sufixos que vem no OkHttp.
     * Null para sufixos públicos ("com", "com.br", "github.io"), endereços IP e nomes sem ponto.
     */
    fun registrableOrNull(host: String): String? {
        val h = host.trim().trimStart('.').trimEnd('.').lowercase()
        if (h.isEmpty() || h.any { it.isWhitespace() || it == '/' || it == '@' || it == ':' }) return null
        return "https://$h/".toHttpUrlOrNull()?.topPrivateDomain()
    }

    private fun registrable(host: String): String = registrableOrNull(host) ?: host.lowercase()

    /** Lê os cookies que a janela de login guardou e grava no cookies.txt (troca os antigos dos mesmos sites). */
    fun saveFromWebView(ctx: Context, pageUrl: String): Int {
        val pageHost = runCatching { URI(pageUrl).host?.lowercase() }.getOrNull() ?: return 0
        val base = registrable(pageHost)
        val group = related[base]
        val cm = CookieManager.getInstance()
        val newLines = mutableListOf<String>()
        val expiry = System.currentTimeMillis() / 1000 + 365L * 24 * 3600
        val seen = mutableSetOf<String>()
        // Sites conhecidos (YouTube, Instagram…): como antes, valem para o domínio todo e os subdomínios.
        // Os outros ficam presos ao endereço exato de onde vieram (sem subdomínios).
        val sources: List<Pair<String, String>> = if (group != null) {
            group.flatMap { h -> listOf("https://$h/", "https://www.$h/").map { u -> u to ".$h" } }
        } else {
            listOf(pageHost, base, "www.$base").distinct().map { h -> "https://$h/" to h }
        }
        for ((u, domain) in sources) {
            val raw = cm.getCookie(u) ?: continue
            val subdomains = if (domain.startsWith(".")) "TRUE" else "FALSE"
            for (pair in raw.split(";")) {
                val kv = pair.trim().split("=", limit = 2)
                if (kv.size != 2 || kv[0].isEmpty()) continue
                if (!seen.add("$domain|${kv[0]}")) continue
                newLines += listOf(domain, subdomains, "/", "TRUE", expiry.toString(), kv[0], kv[1]).joinToString("\t")
            }
        }
        if (newLines.isEmpty()) return 0
        val file = Args.cookiesFile(ctx)
        val replaced = (group ?: listOf(base, "www.$base", pageHost)).flatMap { listOf(it, ".$it") }.toSet()
        val keep = if (file.exists()) file.readLines().filter { line ->
            val dom = line.removePrefix("#HttpOnly_").split("\t").firstOrNull() ?: ""
            line.startsWith("# ") || line.isBlank() || dom !in replaced
        } else emptyList()
        val header = if (keep.any { it.startsWith("# Netscape") }) emptyList() else listOf("# Netscape HTTP Cookie File", "# Gerado pelo Subaru DL", "")
        file.writeText((header + keep + newLines).joinToString("\n") + "\n")
        changed()
        return newLines.size
    }

    private fun isCookie(line: String) = line.isNotBlank() && !line.startsWith("# ") && line.removePrefix("#HttpOnly_").split("\t").size >= 7

    private fun domainOf(line: String) = line.removePrefix("#HttpOnly_").split("\t").firstOrNull().orEmpty().removePrefix(".").lowercase()

    /**
     * Separa as linhas de cookie de um cookies.txt (formato Netscape). Cookies para um sufixo público inteiro
     * (".com", "com.br", "github.io"), para endereços IP ou nomes sem domínio são ignorados.
     */
    fun parse(text: String): Parsed {
        val candidates = text.lines().map { it.trimEnd('\r') }.filter(::isCookie)
        val ok = candidates.filter { registrableOrNull(domainOf(it)) != null }
        return Parsed(ok, ok.map(::domainOf).distinct().sorted(), candidates.size - ok.size)
    }

    /**
     * Junta os cookies lidos aos que já existem: os sites que vieram substituem os antigos e os outros ficam como
     * estavam. Devolve quantos cookies e de quantos sites entraram.
     */
    fun save(ctx: Context, parsed: Parsed): Imported {
        if (parsed.lines.isEmpty()) return Imported(0, 0)
        val domains = parsed.domains.toSet()
        val file = Args.cookiesFile(ctx)
        val keep = if (file.exists()) file.readLines().filter { isCookie(it) && domainOf(it) !in domains } else emptyList()
        val header = listOf("# Netscape HTTP Cookie File", "# Gerado pelo Subaru DL", "")
        file.parentFile?.mkdirs()
        file.writeText((header + keep + parsed.lines).joinToString("\n") + "\n")
        changed()
        return Imported(parsed.lines.size, domains.size)
    }

    /** Lê e grava de uma vez (cookies que chegaram do PC, com o código digitado). */
    fun importText(ctx: Context, text: String): Imported = save(ctx, parse(text))

    class ReadError(message: String) : Exception(message)

    /**
     * Lê um cookies.txt de outro app (só content://, até [MAX_FILE]). Rode fora da linha principal.
     * Lança [ReadError] com a frase para mostrar.
     */
    fun read(ctx: Context, uri: Uri): String {
        if (uri.scheme != "content") throw ReadError("Por segurança, o app só abre arquivos entregues pelo Android (Arquivos, Drive, WhatsApp…)")
        val bytes = try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val buf = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    buf.write(chunk, 0, n)
                    if (buf.size() > MAX_FILE) throw ReadError("Esse arquivo é grande demais para ser um cookies.txt")
                }
                buf.toByteArray()
            }
        } catch (e: ReadError) {
            throw e
        } catch (e: Exception) {
            null
        } ?: throw ReadError("Não consegui ler esse arquivo")
        return String(bytes, Charsets.UTF_8)
    }
}
