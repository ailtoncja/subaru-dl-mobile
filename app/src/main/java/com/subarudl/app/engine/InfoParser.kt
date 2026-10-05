package com.subarudl.app.engine

import com.subarudl.app.data.Chapter
import com.subarudl.app.data.FormatInfo
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.PlaylistEntry
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.str(key: String): String? = if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotEmpty() && it != "null" }
private fun JSONObject.lng(key: String): Long? = if (isNull(key) || !has(key)) null else runCatching { getDouble(key).toLong() }.getOrNull()
private fun JSONObject.dbl(key: String): Double? = if (isNull(key) || !has(key)) null else runCatching { getDouble(key) }.getOrNull()
private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

/** "YoutubeTab" → "YouTube", "TwitchVod" → "Twitch"… */
fun siteName(key: String): String {
    val base = key.replace(Regex("(Tab|Playlist|Vod|Clips?|Live|User|Channel|Post|Video|Search(URL)?)$", RegexOption.IGNORE_CASE), "").ifEmpty { key }
    val names = mapOf(
        "youtube" to "YouTube", "tiktok" to "TikTok", "twitch" to "Twitch", "instagram" to "Instagram", "twitter" to "X/Twitter",
        "facebook" to "Facebook", "vimeo" to "Vimeo", "soundcloud" to "SoundCloud", "reddit" to "Reddit", "kick" to "Kick",
        "bilibili" to "Bilibili", "dailymotion" to "Dailymotion",
    )
    return names[base.lowercase()] ?: base
}

private fun parseFormat(f: JSONObject): FormatInfo = FormatInfo(
    id = f.optString("format_id"),
    ext = f.optString("ext"),
    height = f.lng("height")?.toInt(),
    fps = f.dbl("fps"),
    vcodec = f.str("vcodec"),
    acodec = f.str("acodec"),
    size = f.lng("filesize") ?: f.lng("filesize_approx"),
    approx = f.lng("filesize") == null,
    tbr = f.dbl("tbr"),
    abr = f.dbl("abr"),
    language = f.str("language"),
    note = f.str("format_note"),
    protocol = f.str("protocol"),
)

/** Capa de um item da lista rápida: a primeira com ~300 px de largura ou mais. */
private fun entryThumb(e: JSONObject): String? {
    val list = e.optJSONArray("thumbnails").objects().filter { it.str("url") != null }
    val good = list.firstOrNull { (it.lng("width") ?: 0) >= 300 } ?: list.lastOrNull()
    good?.str("url")?.let { return it }
    e.str("thumbnail")?.let { return it }
    val id = e.str("id") ?: return null
    val isYoutube = Regex("youtu", RegexOption.IGNORE_CASE).containsMatchIn((e.str("url") ?: "") + (e.str("ie_key") ?: ""))
    return if (isYoutube && Regex("^[\\w-]{11}$").matches(id)) "https://i.ytimg.com/vi/$id/mqdefault.jpg" else null
}

fun parseInfo(d: JSONObject): MediaInfo {
    val entriesJson = d.optJSONArray("entries")
    val isPlaylist = d.optString("_type") == "playlist" || entriesJson != null
    val rawFormats = d.optJSONArray("formats").objects()
    val formats = rawFormats
        .filter { it.has("format_id") && !Regex("storyboard|mhtml", RegexOption.IGNORE_CASE).containsMatchIn(it.optString("format_note")) && it.optString("ext") != "mhtml" }
        .map(::parseFormat)
    val audioLangs = formats.filter { it.vcodec == "none" && it.language != null }.map { it.language!! }.distinct()
    // O YouTube marca a faixa original com language_preference positivo e "original" na descrição; as dubladas têm -1
    val original = rawFormats.firstOrNull {
        it.optString("vcodec") == "none" && it.str("language") != null &&
            ((it.dbl("language_preference") ?: 0.0) > 0 || Regex("original", RegexOption.IGNORE_CASE).containsMatchIn(it.optString("format_note")))
    }?.str("language")
    val chapters = d.optJSONArray("chapters").objects().map { Chapter(it.optString("title"), it.optDouble("start_time", 0.0), it.optDouble("end_time", 0.0)) }

    val entries = if (isPlaylist) {
        entriesJson.objects().mapIndexed { i, e ->
            PlaylistEntry(
                index = e.lng("playlist_index")?.toInt() ?: (i + 1),
                id = e.str("id") ?: i.toString(),
                title = e.str("title") ?: e.str("id") ?: "Item ${i + 1}",
                channel = e.str("channel") ?: e.str("uploader"),
                duration = e.lng("duration"),
                url = e.str("url") ?: e.str("webpage_url") ?: "",
                thumb = entryThumb(e),
            )
        }
    } else emptyList()

    val firstEntry = entriesJson.objects().firstOrNull()
    val thumb = if (isPlaylist) {
        firstEntry?.let(::entryThumb) ?: d.str("thumbnail")
    } else {
        d.str("thumbnail") ?: entryThumb(d)
    }
    val site = siteName(d.str("extractor_key") ?: d.str("extractor") ?: "")
    return MediaInfo(
        id = d.optString("id"),
        title = d.str("title") ?: d.str("id") ?: "Sem título",
        channel = d.str("channel") ?: d.str("uploader"),
        site = site,
        pageUrl = d.str("webpage_url") ?: d.str("original_url") ?: "",
        thumb = thumb,
        duration = d.lng("duration") ?: if (isPlaylist) entries.sumOf { it.duration ?: 0 }.takeIf { it > 0 } else null,
        uploadDate = d.str("upload_date"),
        views = d.lng("view_count"),
        likes = d.lng("like_count"),
        isLive = d.optBoolean("is_live") || d.optString("live_status") == "is_live",
        formats = formats,
        subs = d.optJSONObject("subtitles")?.keys()?.asSequence()?.filter { it != "live_chat" }?.toList().orEmpty(),
        // Automáticas "de verdade" (pt-BR, zh-Hans…), sem as traduzidas "en-ar", "pt-PT-bg"…
        autoSubs = d.optJSONObject("automatic_captions")?.keys()?.asSequence()
            ?.filter { it.endsWith("-orig") || !Regex("-[a-z]{2,3}$").containsMatchIn(it) }?.toList().orEmpty(),
        audioLangs = audioLangs,
        originalLang = original,
        chapters = chapters,
        playlistTitle = if (isPlaylist) (d.str("title") ?: "Playlist") else null,
        playlistTotal = if (isPlaylist) (d.lng("playlist_count")?.toInt() ?: entries.size) else 0,
        entries = entries,
    )
}
