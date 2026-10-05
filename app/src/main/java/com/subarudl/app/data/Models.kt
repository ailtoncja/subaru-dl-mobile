package com.subarudl.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class Mode { VIDEO, AUDIO }

@Serializable
enum class Kind { VIDEO, GALLERY }

@Serializable
enum class Status { WAITING, DOWNLOADING, PROCESSING, PAUSED, DONE, ERROR }

/** Legendas de um download (espelha o PC). */
@Serializable
data class SubOptions(
    val enabled: Boolean = false,
    val auto: Boolean = false,
    val langs: List<String> = listOf("pt", "pt-BR"),
    val all: Boolean = false,
    val format: String = "srt",
    val embed: Boolean = true,
)

/** Opções de um download de vídeo/áudio. Cada item da fila guarda uma cópia. */
@Serializable
data class DownloadOptions(
    val mode: Mode = Mode.VIDEO,
    /** "best" ou a altura máxima (ex.: "1080") */
    val quality: String = "best",
    val container: String = "mp4",
    val audioFormat: String = "mp3",
    val audioBitrate: Int = 320,
    /** auto, h264, vp9, av1 */
    val codecVideo: String = "h264",
    val fps30: Boolean = false,
    /** Faixa de áudio escolhida (null = a original) */
    val audioLang: String? = null,
    /** Formato exato escolhido na lista real do vídeo */
    val formatId: String? = null,
    val formatVideoOnly: Boolean = false,
    /** Não converte: o arquivo sai como veio do site */
    val keepOriginal: Boolean = false,
    val sectionStart: String = "",
    val sectionEnd: String = "",
    val splitChapters: Boolean = false,
    val liveFromStart: Boolean = false,
    val subs: SubOptions = SubOptions(),
    val embedMetadata: Boolean = true,
    val embedThumb: Boolean = true,
    val saveThumb: Boolean = false,
    /** "" = todos, "0" = nenhum, "1,3-5" = escolhidos */
    val playlistItems: String = "",
    val playlistLimit: Int = 0,
    val reverse: Boolean = false,
    /** none, playlist, channel, channel-playlist, year-month, artist-album, custom */
    val subfolder: String = "none",
    val subfolderCustom: String = "%(uploader)s",
    val numbering: Boolean = false,
    /** Pasta (árvore do seletor do Android). null = pasta padrão do app (Filmes/Música). */
    val destination: String? = null,
)

/** Opções de um download da galeria (gallery-dl). */
@Serializable
data class GalleryOptions(
    /** flat, site, profile, default, custom */
    val organize: String = "default",
    val customFormat: String = "{category}/{user}",
    val range: String = "",
    val destination: String? = null,
)

@Serializable
data class OutputFile(
    val name: String,
    val uri: String,
    val mime: String,
    val size: Long = 0,
    val rel: String = "",
)

@Serializable
data class QueueItem(
    val id: String,
    val kind: Kind = Kind.VIDEO,
    val url: String,
    val title: String,
    val channel: String? = null,
    val site: String = "",
    val thumb: String? = null,
    val duration: Long? = null,
    val isPlaylist: Boolean = false,
    val isLive: Boolean = false,
    val options: DownloadOptions = DownloadOptions(),
    val gallery: GalleryOptions? = null,
    val status: Status = Status.WAITING,
    val progress: Float = 0f,
    val downloaded: Long = 0,
    val total: Long = 0,
    val speed: Double = 0.0,
    val eta: Long = -1,
    val stage: String = "",
    val itemIndex: Int = 0,
    val itemCount: Int = 0,
    val error: String? = null,
    val hint: String? = null,
    val attempt: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = 0,
    val outputs: List<OutputFile> = emptyList(),
    val fileCount: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    /** Resolução/qualidade que saiu de fato (ex.: "1080p"), para mostrar no cartão */
    val quality: String = "",
    val ext: String = "",
    val sizeBytes: Long = 0,
) {
    val active get() = status == Status.DOWNLOADING || status == Status.PROCESSING
    val isAudio get() = kind == Kind.VIDEO && options.mode == Mode.AUDIO
}

// ---------- Informações de um link (não são guardadas) ----------

data class FormatInfo(
    val id: String,
    val ext: String,
    val height: Int?,
    val fps: Double?,
    val vcodec: String?,
    val acodec: String?,
    val size: Long?,
    val approx: Boolean,
    val tbr: Double?,
    val abr: Double?,
    val language: String?,
    val note: String?,
    val protocol: String?,
) {
    val hasVideo get() = vcodec != null && vcodec != "none"
    val hasAudio get() = acodec != null && acodec != "none"
}

data class PlaylistEntry(
    val index: Int,
    val id: String,
    val title: String,
    val channel: String?,
    val duration: Long?,
    val url: String,
    val thumb: String?,
)

data class MediaInfo(
    val id: String,
    val title: String,
    val channel: String?,
    val site: String,
    val pageUrl: String,
    val thumb: String?,
    val duration: Long?,
    val uploadDate: String?,
    val views: Long?,
    val likes: Long?,
    val isLive: Boolean,
    val formats: List<FormatInfo>,
    val subs: List<String>,
    val autoSubs: List<String>,
    val audioLangs: List<String>,
    val originalLang: String?,
    val chapters: List<Chapter>,
    val playlistTitle: String?,
    val playlistTotal: Int,
    val entries: List<PlaylistEntry>,
) {
    val isPlaylist get() = playlistTitle != null
    val maxHeight get() = formats.maxOfOrNull { it.height ?: 0 } ?: 0
}

data class Chapter(val title: String, val start: Double, val end: Double)

/** Arquivo listado pela prévia da galeria. */
data class GalleryFile(
    val index: Int,
    val url: String,
    val name: String,
    val ext: String,
    val width: Int?,
    val height: Int?,
    val video: Boolean,
)
