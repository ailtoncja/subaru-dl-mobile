package com.subarudl.app.engine

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Atualiza o yt-dlp com conferência (no lugar do atualizador da biblioteca, que troca o arquivo sem conferir nada).
 *
 * Baixa da Release do GitHub do canal escolhido (yt-dlp, yt-dlp-nightly-builds ou yt-dlp-master-builds):
 *  1. SHA2-256SUMS e SHA2-256SUMS.sig, e confere a assinatura com a chave oficial do yt-dlp ([OpenPgp]);
 *  2. o arquivo "yt-dlp", e confere o SHA-256 dele com o da lista assinada;
 *  3. só então troca o arquivo, de uma vez (renomear na mesma pasta), e grava a versão onde a biblioteca lê.
 *
 * A biblioteca (youtubedl-android 0.18.1) roda sempre noBackupFilesDir/youtubedl-android/yt-dlp/yt-dlp com o Python
 * embutido e guarda a versão nas preferências "youtubedl-android" (chaves dlpVersion e dlpVersionName).
 */
object YtdlpUpdater {
    private val repos = mapOf(
        "stable" to "yt-dlp/yt-dlp",
        "nightly" to "yt-dlp/yt-dlp-nightly-builds",
        "master" to "yt-dlp/yt-dlp-master-builds",
    )
    private const val PREFS = "youtubedl-android"
    private const val KEY_VERSION = "dlpVersion"
    private const val KEY_VERSION_NAME = "dlpVersionName"

    class Result(val updated: Boolean, val version: String)

    private fun binary(ctx: Context) = File(ctx.noBackupFilesDir, "youtubedl-android/yt-dlp/yt-dlp")

    fun installedVersion(ctx: Context): String? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_VERSION, null)

    /** Procura e instala a versão mais nova do [channel] ("stable", "nightly" ou "master"). Lança exceção se algo não conferir. */
    fun update(ctx: Context, channel: String): Result {
        val repo = repos[channel] ?: repos.getValue("stable")
        val release = JSONObject(String(get("https://api.github.com/repos/$repo/releases/latest", 2 * 1024 * 1024, api = true), Charsets.UTF_8))
        val tag = release.getString("tag_name")
        // A versão vira parte do endereço: só números e pontos (2026.08.19, 2026.10.10.233141)
        if (!Regex("[0-9]{4}\\.[0-9]{1,2}\\.[0-9]{1,2}(\\.[0-9]{1,8})?").matches(tag)) throw EngineException("O GitHub informou uma versão estranha do yt-dlp ($tag)")
        if (tag == installedVersion(ctx) && binary(ctx).isFile) return Result(false, tag)

        val base = "https://github.com/$repo/releases/download/$tag/"
        val sums = get(base + "SHA2-256SUMS", 256 * 1024)
        val sig = get(base + "SHA2-256SUMS.sig", 64 * 1024)
        if (!OpenPgp.verify(sums, sig)) throw EngineException("A lista de resumos do yt-dlp $tag não tem a assinatura oficial; nada foi trocado")
        val want = String(sums, Charsets.UTF_8).lines().firstNotNullOfOrNull { line ->
            Regex("^([0-9a-fA-F]{64})\\s+\\*?yt-dlp$").find(line.trim())?.groupValues?.get(1)?.lowercase()
        } ?: throw EngineException("A lista assinada não traz o yt-dlp; nada foi trocado")

        val target = binary(ctx)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "yt-dlp.novo")
        try {
            val md = MessageDigest.getInstance("SHA-256")
            open(base + "yt-dlp").use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > 64L * 1024 * 1024) throw EngineException("O yt-dlp veio grande demais; nada foi trocado")
                        md.update(buf, 0, n)
                        out.write(buf, 0, n)
                    }
                }
            }
            val got = md.digest().joinToString("") { "%02x".format(it) }
            if (got != want) throw EngineException("O yt-dlp baixado não bate com o resumo assinado; nada foi trocado")
            if (!tmp.renameTo(target)) throw EngineException("Não consegui trocar o arquivo do yt-dlp")
        } finally {
            tmp.delete()
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_VERSION, tag)
            .putString(KEY_VERSION_NAME, release.optString("name", tag))
            .commit()
        return Result(true, tag)
    }

    /** Abre um endereço https (os redirecionamentos do GitHub também precisam ser https). */
    private fun open(url: String, api: Boolean = false): InputStream {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 60_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "SubaruDL")
        if (api) c.setRequestProperty("Accept", "application/vnd.github+json")
        val code = c.responseCode
        if (c.url.protocol != "https") {
            c.disconnect()
            throw EngineException("O GitHub redirecionou para um endereço sem https")
        }
        if (code != 200) {
            c.disconnect()
            throw EngineException("O GitHub respondeu $code")
        }
        return c.inputStream
    }

    private fun get(url: String, limit: Int, api: Boolean = false): ByteArray = open(url, api).use { input ->
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > limit) throw EngineException("Resposta grande demais do GitHub")
        }
        out.toByteArray()
    }
}
