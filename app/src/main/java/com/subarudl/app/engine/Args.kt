package com.subarudl.app.engine

import android.content.Context
import com.subarudl.app.data.AppSettings
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.Mode
import com.subarudl.app.data.QueueItem
import java.io.File

/** Monta os argumentos do yt-dlp a partir das opções da interface (espelha o app do PC). */
object Args {
    /** Marcadores que o motor procura na saída do yt-dlp. */
    const val MARK_PROGRESS = "SUBARU_P "
    const val MARK_POST = "SUBARU_PP "
    const val MARK_FILE = "SUBARU_ARQ "

    fun cookiesFile(ctx: Context) = File(ctx.filesDir, "cookies.txt")

    /** Divide uma linha de argumentos respeitando aspas: `-x "a b"` → ["-x", "a b"] */
    fun split(text: String): List<String> =
        Regex("\"([^\"]*)\"|'([^']*)'|(\\S+)").findAll(text).map { it.groupValues[1].ifEmpty { it.groupValues[2].ifEmpty { it.groupValues[3] } } }.toList()

    private fun lines(text: String) = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

    // ---------- Rede, cookies e login (usado para ler informações e para baixar) ----------

    fun network(ctx: Context, s: AppSettings, extraExtractor: List<String> = emptyList()): List<String> {
        val a = mutableListOf<String>()
        val cookies = cookiesFile(ctx)
        if (s.cookiesEnabled && cookies.exists() && cookies.length() > 0) a += listOf("--cookies", cookies.absolutePath)
        if (s.proxy.isNotBlank()) a += listOf("--proxy", s.proxy.trim())
        if (s.rateLimit.isNotBlank()) a += listOf("-r", s.rateLimit.trim())
        a += listOf("--retries", s.retries.toString(), "--fragment-retries", s.retries.toString(), "--socket-timeout", "30")

        // Argumentos de extrator agrupados por site + player.js real
        val byExtractor = linkedMapOf<String, MutableList<String>>()
        val all = lines(s.extractorArgs) + extraExtractor + if (s.playerJsActual) listOf("youtube:player_js_version=actual") else emptyList()
        for (line in all) {
            val i = line.indexOf(':')
            if (i <= 0) continue
            byExtractor.getOrPut(line.substring(0, i).trim().lowercase()) { mutableListOf() }.add(line.substring(i + 1).trim())
        }
        for ((k, v) in byExtractor) a += listOf("--extractor-args", "$k:${v.joinToString(";")}")
        return a
    }

    // ---------- Seleção de formato ----------

    private val sortCodec = mapOf("h264" to "vcodec:h264", "vp9" to "vcodec:vp9", "av1" to "vcodec:av01")

    fun format(o: DownloadOptions): List<String> {
        val a = mutableListOf<String>()
        val audioLang = if (o.audioLang != null) "ba[language^=${o.audioLang}]/ba" else "ba"

        if (o.mode == Mode.AUDIO) {
            a += listOf("-f", o.formatId ?: "$audioLang/b")
            if (!o.keepOriginal) {
                a += listOf("-x", "--audio-format", o.audioFormat)
                if (o.audioFormat != "wav" && o.audioFormat != "flac") {
                    a += listOf("--audio-quality", if (o.audioBitrate > 0) "${o.audioBitrate}K" else "0")
                }
            }
            // "lang" na frente: a faixa original (ou a escolhida) vem antes de qualquer outra preferência
            if (o.formatId == null) a += listOf("-S", "lang")
            return a
        }

        // WebM só aceita VP9/AV1 + Opus: ajusta o codec para o arquivo sair válido
        val webm = o.container == "webm" && !o.keepOriginal
        val codecVideo = if (webm && (o.codecVideo == "h264" || o.codecVideo == "auto")) "vp9" else o.codecVideo
        // MP4 toca em qualquer celular com H.264 + AAC; AV1/Opus dentro de MP4 trava em muitos aparelhos
        val sortAudio = if (webm) "acodec:opus" else if (o.container == "mp4" && !o.keepOriginal) "acodec:aac" else null

        if (o.formatId != null) {
            a += listOf("-f", if (o.formatVideoOnly) "${o.formatId}+$audioLang/${o.formatId}" else o.formatId)
        } else {
            val filter = (if (o.quality == "best") "" else "[height<=${o.quality}]") + (if (o.fps30) "[fps<=30]" else "")
            // Se não existir na qualidade pedida, pega a mais próxima abaixo; em último caso, a pior disponível
            a += listOf("-f", "bv*$filter+$audioLang/b$filter/wv*+ba/w")
            // "res" antes do codec: a qualidade pedida manda; o codec só desempata (H.264 a 1080p ganha do VP9 a 1080p)
            a += listOf("-S", listOfNotNull("lang", "res", sortCodec[codecVideo], sortAudio).joinToString(","))
        }
        if (!o.keepOriginal) a += listOf("--merge-output-format", o.container, "--remux-video", o.container)
        return a
    }

    // ---------- Pastas e nomes ----------

    fun outputTemplate(s: AppSettings, o: DownloadOptions, isPlaylist: Boolean): String {
        val audio = o.mode == Mode.AUDIO
        val folder = when (o.subfolder) {
            "playlist" -> if (isPlaylist) "%(playlist_title,playlist)s/" else ""
            "channel" -> "%(channel,uploader|Desconhecido)s/"
            "channel-playlist" -> "%(channel,uploader|Desconhecido)s/" + if (isPlaylist) "%(playlist_title,playlist)s/" else ""
            "year-month" -> "%(upload_date>%Y|Sem data)s/%(upload_date>%m|)s/"
            "artist-album" -> "%(artist,creator,uploader|Desconhecido)s/%(album|Sem álbum)s/"
            "custom" -> o.subfolderCustom.trim().trimEnd('/', '\\') + "/"
            else -> ""
        }
        val quality = if (audio) (if (o.audioBitrate > 0) "(${o.audioBitrate}k)" else "(%(abr)dk)") else "(%(height)sp%(fps)s)"
        val index = "%(playlist_index)03d"
        var name = when (s.nameMode) {
            "title-quality" -> "%(title)s-$quality"
            "playlist-title-quality" -> (if (isPlaylist) "$index-" else "") + "%(title)s-$quality"
            "custom" -> s.nameCustom.trim().ifEmpty { "%(title)s" }
            else -> "%(title)s"
        }
        if (isPlaylist && o.numbering && !name.contains("playlist_index")) name = "$index - $name"
        return "$folder$name.%(ext)s"
    }

    // ---------- Download completo ----------

    fun download(ctx: Context, item: QueueItem, s: AppSettings, workDir: File): List<String> {
        val o = item.options
        val a = mutableListOf(
            "--color", "never",
            "--newline", "--progress", "--no-quiet", "--no-mtime",
            "--progress-template",
            "download:$MARK_PROGRESS%(progress.status)s|%(progress.downloaded_bytes)s|%(progress.total_bytes)s|%(progress.total_bytes_estimate)s|%(progress.speed)s|%(progress.eta)s|%(info.playlist_index)s|%(info.n_entries)s|%(info.vcodec)s|%(progress.fragment_index)s|%(progress.fragment_count)s",
            "--progress-template",
            "postprocess:$MARK_POST%(progress.status)s|%(progress.postprocessor)s|%(info.playlist_index)s",
            "--print",
            "after_move:$MARK_FILE%(.{id,title,channel,uploader,extractor_key,webpage_url,duration,height,fps,abr,ext,filepath,thumbnail,upload_date,playlist_title,view_count,like_count})j",
            "-P", "home:${workDir.absolutePath}",
            "-o", outputTemplate(s, o, item.isPlaylist),
        )
        a += format(o)
        if (s.safeNames) a += "--restrict-filenames"

        // Trechos e capítulos
        if (o.sectionStart.isNotBlank() || o.sectionEnd.isNotBlank()) {
            a += listOf("--download-sections", "*${o.sectionStart.ifBlank { "0" }}-${o.sectionEnd.ifBlank { "inf" }}")
            a += "--force-keyframes-at-cuts"
        }
        if (o.splitChapters) a += listOf("--split-chapters", "-o", "chapter:%(title)s/%(section_number)02d - %(section_title)s.%(ext)s")
        if (o.liveFromStart && item.isLive) a += "--live-from-start"

        // Legendas (só em vídeo). Com --embed-subs o yt-dlp apaga o arquivo depois de embutir
        if (o.subs.enabled && o.mode == Mode.VIDEO) {
            if (!o.subs.embed) a += "--write-subs"
            if (o.subs.auto) a += "--write-auto-subs"
            val langs = if (o.subs.all) "all,-live_chat" else o.subs.langs.joinToString(",")
            if (langs.isNotEmpty()) a += listOf("--sub-langs", langs)
            a += listOf("--sub-format", "${o.subs.format}/best")
            if (o.subs.format in listOf("srt", "vtt", "ass")) a += listOf("--convert-subs", o.subs.format)
            if (o.subs.embed) a += "--embed-subs"
        }

        // Pós-processamento
        if (o.embedMetadata) a += "--embed-metadata"
        val acceptsCover = if (o.mode == Mode.AUDIO) o.audioFormat != "wav" else o.container != "webm" || o.keepOriginal
        if (o.embedThumb && acceptsCover) a += "--embed-thumbnail"
        if (o.saveThumb) a += "--write-thumbnail"
        if (o.embedThumb || o.saveThumb) a += listOf("--convert-thumbnails", "jpg")

        // Playlist
        if (item.isPlaylist) {
            if (o.playlistItems.isNotBlank()) a += listOf("-I", o.playlistItems.trim())
            else if (o.playlistLimit > 0) a += listOf("-I", ":${o.playlistLimit}")
            if (o.reverse) a += "--playlist-reverse"
        } else {
            a += "--no-playlist"
        }

        // Velocidade: aria2 (Turbo) ou fragmentos em paralelo
        if (s.turbo) {
            a += listOf("--downloader", "libaria2c.so", "--downloader-args", "aria2c:-x 8 -s 8 -k 1M")
            a += listOf("--concurrent-fragments", "4")
        } else {
            a += listOf("--concurrent-fragments", "3")
        }

        // Nova tentativa depois de um 403 no YouTube: troca o cliente usado para pedir os formatos
        val extras = mutableListOf<String>()
        if (item.attempt > 0 && Regex("403").containsMatchIn(item.error ?: "") && Regex("youtu\\.?be", RegexOption.IGNORE_CASE).containsMatchIn(item.url)) {
            val clients = listOf("tv", "web_safari", "mweb")
            extras += "youtube:player_client=${clients[(item.attempt - 1) % clients.size]}"
        }
        a += network(ctx, s, extras)
        if (s.extraArgs.isNotBlank()) a += split(s.extraArgs)
        a += "--"
        return a
    }

    /** Esconde senhas e tokens antes de mostrar o comando nos logs. */
    fun forLog(args: List<String>): String {
        val sensitive = setOf("--password", "--video-password", "--add-headers", "--proxy", "--username", "--cookies")
        return "yt-dlp " + args.mapIndexed { i, arg ->
            when {
                i > 0 && args[i - 1] in sensitive -> "••••"
                Regex("\\s|\"").containsMatchIn(arg) -> "\"" + arg.replace("\"", "\\\"") + "\""
                else -> arg
            }
        }.joinToString(" ")
    }
}
