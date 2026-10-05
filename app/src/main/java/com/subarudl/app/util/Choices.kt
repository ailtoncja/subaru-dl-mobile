package com.subarudl.app.util

import com.subarudl.app.data.FormatInfo
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.SubOptions
import com.subarudl.app.ui.components.Opt

private val tierNames = mapOf(4320 to "8K", 2160 to "4K UHD", 1440 to "2K QHD", 1080 to "FHD", 720 to "HD", 480 to "SD", 360 to "")

fun tierLabel(h: Int): String = "${h}p" + (tierNames[h]?.takeIf { it.isNotEmpty() }?.let { " $it" } ?: "")

private fun sizeOf(f: FormatInfo, duration: Long?): Long? =
    f.size ?: if (f.tbr != null && duration != null) (f.tbr * 1000 / 8 * duration).toLong() else null

/** Melhor formato de vídeo com altura até [h] (como o yt-dlp escolheria). */
private fun pickVideo(info: MediaInfo, h: Int?, fps30: Boolean): FormatInfo? =
    info.formats.filter { it.hasVideo && it.height != null && (h == null || it.height <= h) && (!fps30 || (it.fps ?: 0.0) <= 30.0) }
        .maxWithOrNull(compareBy<FormatInfo> { it.height }.thenBy { it.fps ?: 0.0 }.thenBy { it.tbr ?: 0.0 })

private fun bestAudio(info: MediaInfo): FormatInfo? =
    info.formats.filter { it.hasAudio && !it.hasVideo }.maxByOrNull { it.abr ?: it.tbr ?: 0.0 }

fun estimateSize(info: MediaInfo, h: Int?, fps30: Boolean): Long? {
    val v = pickVideo(info, h, fps30) ?: return null
    val vs = sizeOf(v, info.duration) ?: return null
    if (v.hasAudio) return vs
    val a = bestAudio(info)?.let { sizeOf(it, info.duration) } ?: 0L
    return vs + a
}

/** Para playlists e lotes (o app ainda não sabe os formatos de cada vídeo): se não existir, usa a mais próxima abaixo. */
val genericQualities = listOf(
    Opt("best", "Melhor disponível"), Opt("2160", "2160p 4K UHD"), Opt("1440", "1440p 2K QHD"), Opt("1080", "1080p Full HD"),
    Opt("720", "720p HD"), Opt("480", "480p SD"), Opt("360", "360p"), Opt("240", "240p"),
)

/** Qualidades que existem de verdade, cada uma com a estimativa de tamanho. */
fun videoQualityOptions(info: MediaInfo?, fps30: Boolean): List<Opt<String>> {
    if (info == null || info.isPlaylist) return genericQualities
    val heights = info.formats.filter { it.hasVideo && it.height != null && it.height >= 144 }.map { it.height!! }.distinct().sortedDescending()
    if (heights.isEmpty()) return genericQualities
    fun describe(h: Int?): String {
        val f = pickVideo(info, h, fps30) ?: return ""
        val fps = f.fps?.takeIf { it > 30 }?.let { "${it.toInt()}fps" }
        val size = estimateSize(info, h, fps30)?.let { formatBytes(it) }
        val extra = listOfNotNull(fps, size).joinToString(" • ")
        return if (extra.isEmpty()) "" else " ($extra)"
    }
    val list = mutableListOf(Opt("best", "Melhor disponível • ${tierLabel(heights.first())}" + describe(null)))
    for (h in heights.drop(1)) list += Opt(h.toString(), tierLabel(h) + describe(h))
    return list
}

fun audioBitrateOptions(info: MediaInfo?, lossless: Boolean): List<Opt<Int>> {
    if (lossless) return listOf(Opt(0, "Sem perda (WAV/FLAC)"))
    return listOf(320, 256, 192, 128, 96, 64).map { kbps ->
        val size = info?.duration?.let { it * kbps * 1000L / 8 }
        Opt(kbps, "$kbps kbps" + (size?.let { " • ${formatBytes(it)}" } ?: "") + if (kbps == 320) " (Alta fidelidade)" else "")
    }
}

val videoContainers = listOf(Opt("mp4", "MP4"), Opt("mkv", "MKV"), Opt("webm", "WebM"))
val audioFormats = listOf(Opt("mp3", "MP3"), Opt("m4a", "M4A"), Opt("opus", "OPUS"), Opt("aac", "AAC"), Opt("ogg", "OGG"), Opt("flac", "FLAC"), Opt("wav", "WAV"))
val videoCodecs = listOf(Opt("auto", "Automático"), Opt("h264", "H.264 (compatível)"), Opt("vp9", "VP9"), Opt("av1", "AV1"))

// ---------- Faixas de áudio ----------



fun audioTrackLabel(code: String, original: String?): String {
    if (code == "") return if (original != null) "${languageName(original)} • original (padrão)" else "Padrão do vídeo"
    return "${languageName(code)} • " + if (code == original) "original" else "dublagem"
}

fun audioTrackOptions(langs: List<String>, original: String?): List<Opt<String>> {
    val others = langs.filter { it != original }.sortedBy { languageName(it) }
    return listOf(Opt("", audioTrackLabel("", original))) + others.map { Opt(it, audioTrackLabel(it, original)) }
}

// ---------- Legendas ----------

/** Idiomas para escolher quando o app ainda não sabe quais legendas cada vídeo tem (playlists e lotes). */
val commonSubLangs = listOf("pt-BR", "pt", "en", "es", "fr", "de", "it", "ja", "ko", "ru", "zh-Hans", "ar", "hi", "tr", "pl", "nl")

/** Código escolhido → lista que o yt-dlp entende (português cobre pt e pt-BR). */
fun subLangsFor(code: String): List<String> = if (code.startsWith("pt")) listOf("pt", "pt-BR") else listOf(code)

data class SubChoice(val value: String, val label: String)

private fun subOrder(codes: List<String>): List<String> {
    fun weight(c: String) = if (c.endsWith("-orig")) 0 else if (Regex("^pt(-|$)").containsMatchIn(c)) 1 else if (c == "en") 2 else 3
    return codes.sortedWith(compareBy<String> { weight(it) }.thenBy { languageName(it) })
}

/** (manuais, automáticas), cada uma com valor "m:xx" / "a:xx". */
fun subtitleChoices(info: MediaInfo): Pair<List<SubChoice>, List<SubChoice>> =
    subOrder(info.subs).map { SubChoice("m:$it", languageName(it)) } to
        subOrder(info.autoSubs).map { SubChoice("a:$it", languageName(it) + (if (it.endsWith("-orig")) " (original)" else "") + " • automática") }

fun subtitleValue(s: SubOptions): String {
    if (!s.enabled) return ""
    if (!s.all && s.langs.size == 1) return (if (s.auto) "a:" else "m:") + s.langs[0]
    return "padrao"
}

fun applySubtitle(s: SubOptions, value: String): SubOptions = when (value) {
    "" -> s.copy(enabled = false)
    "padrao" -> s
    else -> s.copy(enabled = true, all = false, auto = value[0] == 'a', langs = listOf(value.substring(2)))
}

fun subtitleLabel(s: SubOptions): String {
    if (s.all) return "todas"
    return s.langs.joinToString(", ") { languageName(it) }
}
