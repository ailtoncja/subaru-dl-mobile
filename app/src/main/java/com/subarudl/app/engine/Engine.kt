package com.subarudl.app.engine

import android.content.Context
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.SettingsStore
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EngineState(
    val ready: Boolean = false,
    val error: String? = null,
    val ytdlp: String? = null,
    val gallerydl: String? = null,
    val updating: Boolean = false,
)

/** Prepara as ferramentas embutidas (Python, yt-dlp, ffmpeg, aria2, gallery-dl) na primeira abertura. */
object Engine {
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    @Volatile
    private var started = false

    @Synchronized
    fun init(ctx: Context) {
        if (started) return
        started = true
        val app = ctx.applicationContext
        try {
            AppLog.i("Preparando as ferramentas…")
            YoutubeDL.init(app)
            FFmpeg.getInstance().init(app)
            Aria2c.getInstance().init(app)
            GalleryDl.ensureInstalled(app)
            _state.value = EngineState(ready = true, ytdlp = YoutubeDL.version(app), gallerydl = GalleryDl.version(app))
            AppLog.i("Ferramentas prontas — yt-dlp ${_state.value.ytdlp}, gallery-dl ${_state.value.gallerydl}")
        } catch (e: Throwable) {
            AppLog.e("Falha ao preparar as ferramentas: ${e.message}")
            _state.value = EngineState(ready = false, error = e.message ?: e.javaClass.simpleName)
            started = false
        }
    }

    private fun channel(): YoutubeDL.UpdateChannel = when (SettingsStore.value.updateChannel) {
        "nightly" -> YoutubeDL.UpdateChannel.NIGHTLY
        "master" -> YoutubeDL.UpdateChannel.MASTER
        else -> YoutubeDL.UpdateChannel.STABLE
    }

    /** Atualiza o yt-dlp. Devolve uma frase para mostrar ao usuário. */
    fun updateYtdlp(ctx: Context): String {
        _state.value = _state.value.copy(updating = true)
        return try {
            val before = YoutubeDL.version(ctx)
            val status = YoutubeDL.updateYoutubeDL(ctx, channel())
            val after = YoutubeDL.version(ctx)
            _state.value = _state.value.copy(updating = false, ytdlp = after)
            AppLog.i("yt-dlp: $status ($before → $after)")
            if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) "O yt-dlp já está na versão mais nova ($after)" else "yt-dlp atualizado para $after"
        } catch (e: Throwable) {
            _state.value = _state.value.copy(updating = false)
            AppLog.e("Falha ao atualizar o yt-dlp: ${e.message}")
            "Não consegui atualizar: ${e.message?.take(120)}"
        }
    }

    fun updateGalleryDl(ctx: Context): String {
        _state.value = _state.value.copy(updating = true)
        return try {
            val before = GalleryDl.version(ctx)
            val after = GalleryDl.update(ctx)
            _state.value = _state.value.copy(updating = false, gallerydl = after)
            AppLog.i("gallery-dl: $before → $after")
            if (before == after) "O gallery-dl já está na versão mais nova ($after)" else "gallery-dl atualizado para $after"
        } catch (e: Throwable) {
            _state.value = _state.value.copy(updating = false)
            AppLog.e("Falha ao atualizar o gallery-dl: ${e.message}")
            "Não consegui atualizar: ${e.message?.take(120)}"
        }
    }
}
