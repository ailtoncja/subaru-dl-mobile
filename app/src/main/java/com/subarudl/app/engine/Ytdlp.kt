package com.subarudl.app.engine

import android.content.Context
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.AppSettings
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.QueueItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File
import java.util.UUID

class EngineException(message: String, val hint: String? = null) : Exception(message)

/** Tradução do erro do yt-dlp para uma explicação em português. */
fun errorHint(raw: String): String? = when {
    Regex("HTTP Error 403|403: Forbidden", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O site recusou o pedido (403). O app tenta de novo com outro cliente do YouTube."
    Regex("HTTP Error 429|Too Many Requests", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O site limitou as requisições. Espere alguns minutos ou use cookies/proxy."
    Regex("Sign in to confirm|confirm you.re not a bot|age.restricted|login required", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O site pediu login. Entre na sua conta em Ajustes → Contas e cookies."
    Regex("Private video|members.only|Join this channel", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Vídeo privado ou só para membros. Entre na conta que tem acesso em Ajustes → Contas e cookies."
    Regex("Video unavailable|not available|has been removed|removed by", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O vídeo não está disponível (removido, bloqueado na região ou privado)."
    Regex("HTTP Error 404|404: Not Found", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O link não existe mais ou está incompleto (erro 404). Confira o endereço."
    Regex("No video formats found|There is no video in this|No media found", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Não achei vídeo nesse link. Se for um post de fotos, escolha Imagens e cole de novo."
    Regex("unable to obtain file audio codec|does not contain any stream|no audio stream", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Esse vídeo não tem som, então não dá para tirar o áudio. Baixe como Vídeo."
    Regex("aria2c exited with code", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O Turbo (aria2) falhou. Desligue o Turbo em Ajustes e tente de novo."
    Regex("No space left|ENOSPC", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Acabou o espaço no aparelho."
    Regex("Unsupported URL", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Esse link não é suportado."
    Regex("Requested format is not available", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "A qualidade pedida não existe nesse vídeo. Escolha outra qualidade."
    Regex("Unable to resolve|Name or service not known|Network is unreachable|timed out|Connection reset", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Sem conexão com a internet ou o site não respondeu."
    else -> null
}

fun cleanError(raw: String): String {
    val line = raw.lines().map { it.trim() }.lastOrNull { it.startsWith("ERROR") }
        ?: raw.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() && !it.startsWith("[debug]") && !it.startsWith("WARNING") }
        ?: "Falha desconhecida"
    return line.removePrefix("ERROR:").trim().replace(Regex("^\\[[^]]+]\\s*[\\w-]+:\\s*"), "")
        // As dicas do yt-dlp em inglês ("See https://github.com/…") não ajudam quem usa o app
        .substringBefore(" See  https://").substringBefore(" See https://").substringBefore(" Also see https://").trim().take(400)
}

object Ytdlp {
    /** Lê as informações do link (vídeo único ou lista de itens, sem baixar nada). */
    fun fetchInfo(ctx: Context, url: String, s: AppSettings, trackId: String? = null): MediaInfo {
        val args = listOf("--color", "never", "-J", "--flat-playlist", "--no-warnings") + Args.network(ctx, s) + "--"
        AppLog.cmd(Args.forLog("yt-dlp", args + url), trackId)
        val req = YoutubeDLRequest(url).addCommands(args)
        val response = try {
            Engine.shared { YoutubeDL.execute(req, "info-" + UUID.randomUUID()) }
        } catch (e: YoutubeDLException) {
            val raw = e.message ?: ""
            AppLog.e(cleanError(raw), trackId)
            throw EngineException(cleanError(raw), errorHint(raw))
        } catch (e: YoutubeDL.CanceledException) {
            throw EngineException("Cancelado")
        }
        val out = response.out.trim()
        if (out.isEmpty()) throw EngineException("O yt-dlp não devolveu nada para esse link")
        return try {
            parseInfo(JSONObject(out.substring(out.indexOf('{'))))
        } catch (e: Exception) {
            AppLog.e("Não consegui ler a resposta: ${e.message}", trackId)
            throw EngineException("Não consegui entender a resposta do yt-dlp")
        }
    }

    /** Roda o download. Cada linha de saída vai para [onLine]. Lança [EngineException] em caso de erro. */
    fun download(ctx: Context, item: QueueItem, s: AppSettings, workDir: File, marks: Args.Marks, onLine: (String) -> Unit) {
        try {
            runDownload(ctx, item, s, workDir, marks, onLine)
        } catch (e: EngineException) {
            // O Turbo (aria2) é um extra: se ele falhar, o download continua do jeito normal em vez de dar erro
            if (!s.turbo || e.message?.contains("aria2c", ignoreCase = true) != true) throw e
            AppLog.w("O Turbo (aria2) falhou (${e.message}). Baixando sem ele.", item.id)
            runDownload(ctx, item, s.copy(turbo = false), workDir, marks, onLine)
        }
    }

    private fun runDownload(ctx: Context, item: QueueItem, s: AppSettings, workDir: File, marks: Args.Marks, onLine: (String) -> Unit) {
        val args = Args.download(ctx, item, s, workDir, marks)
        AppLog.cmd(Args.forLog("yt-dlp", args + item.url), item.id)
        val req = YoutubeDLRequest(item.url).addCommands(args)
        val response = try {
            Engine.shared { YoutubeDL.execute(req, item.id) { _, _, line -> onLine(line) } }
        } catch (e: YoutubeDLException) {
            val raw = e.message ?: ""
            raw.lines().filter { it.startsWith("WARNING") }.forEach { AppLog.w(it, item.id) }
            throw EngineException(cleanError(raw), errorHint(raw))
        }
        response.err.lines().filter { it.startsWith("WARNING") }.forEach { AppLog.w(it, item.id) }
    }

    fun cancel(id: String) = YoutubeDL.destroyProcessById(id)
}
