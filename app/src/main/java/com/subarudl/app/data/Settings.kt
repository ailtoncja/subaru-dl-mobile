package com.subarudl.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

@Serializable
data class AppSettings(
    // Downloads e qualidade
    val defaultMode: Mode = Mode.VIDEO,
    val quality: String = "best",
    val container: String = "mp4",
    val audioFormat: String = "mp3",
    val audioBitrate: Int = 320,
    val codecVideo: String = "auto",
    val fps30: Boolean = false,
    val clipboardCapture: Boolean = true,
    // Legendas e pós-processamento
    val subs: SubOptions = SubOptions(),
    val embedMetadata: Boolean = true,
    val embedThumb: Boolean = true,
    val saveThumb: Boolean = false,
    // Pastas e nomes
    val subfolder: String = "none",
    val subfolderCustom: String = "%(uploader)s",
    val numbering: Boolean = false,
    /** title, title-quality, playlist-title-quality, custom */
    val nameMode: String = "title",
    val nameCustom: String = "%(title)s",
    val safeNames: Boolean = false,
    val videoDest: String? = null,
    val audioDest: String? = null,
    val galleryDest: String? = null,
    // Rede
    val wifiOnly: Boolean = false,
    val concurrent: Int = 2,
    val turbo: Boolean = false,
    val rateLimit: String = "",
    val retries: Int = 3,
    val proxy: String = "",
    val cookiesEnabled: Boolean = true,
    val extractorArgs: String = "",
    val playerJsActual: Boolean = false,
    val extraArgs: String = "",
    // Avisos
    val notifyDone: Boolean = true,
    val soundDone: Boolean = false,
    // Galeria
    val galleryOrganize: String = "default",
    val galleryCustom: String = "{category}/{user}",
    val gallerySkipDone: Boolean = true,
    val gallerySleep: Double = 0.0,
    // Ferramentas
    val updateChannel: String = "stable",
    val autoUpdateTools: Boolean = true,
    val lastToolsCheck: Long = 0,
) {
    /** Opções padrão para um download novo, vindas das configurações. */
    fun newOptions(): DownloadOptions = DownloadOptions(
        mode = defaultMode,
        quality = quality,
        container = container,
        audioFormat = audioFormat,
        audioBitrate = audioBitrate,
        codecVideo = codecVideo,
        fps30 = fps30,
        subs = subs,
        embedMetadata = embedMetadata,
        embedThumb = embedThumb,
        saveThumb = saveThumb,
        subfolder = subfolder,
        subfolderCustom = subfolderCustom,
        numbering = numbering,
        destination = if (defaultMode == Mode.AUDIO) audioDest else videoDest,
    )
}

/** Guarda as configurações num arquivo JSON simples (SharedPreferences) e as expõe como fluxo. */
object SettingsStore {
    private const val PREFS = "settings"
    private const val KEY = "json"
    private lateinit var appContext: Context
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val value get() = _state.value

    fun init(context: Context) {
        appContext = context.applicationContext
        val raw = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw != null) {
            _state.value = runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
        }
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val novo = block(_state.value)
        _state.value = novo
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, AppJson.encodeToString(AppSettings.serializer(), novo)).apply()
    }

    fun reset() = update { AppSettings() }
}
