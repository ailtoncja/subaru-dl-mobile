package com.subarudl.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.subarudl.app.data.AppSettings
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.GalleryFile
import com.subarudl.app.data.GalleryOptions
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.Mode
import com.subarudl.app.data.SettingsStore
import com.subarudl.app.engine.Engine
import com.subarudl.app.engine.EngineException
import com.subarudl.app.engine.GalleryDl
import com.subarudl.app.engine.Ytdlp
import com.subarudl.app.util.compactRanges
import com.subarudl.app.util.extractLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab(val label: String) { HOME("Início"), QUEUE("Fila"), PLAYLISTS("Playlists"), GALLERY("Galeria"), SETTINGS("Ajustes") }
enum class QueueTab { ACTIVE, DONE, FAILED }

/** Um link do lote: vídeo único ou lista de itens. */
class BatchSource(
    val url: String,
    val info: MediaInfo? = null,
    val error: String? = null,
    val loading: Boolean = false,
    /** Itens marcados (índices de 1 em diante). Vazio = nada marcado. */
    val selected: Set<Int> = emptySet(),
    val expanded: Boolean = false,
) {
    val total get() = info?.let { if (it.isPlaylist) it.entries.size else 1 } ?: 0
    fun copy(
        info: MediaInfo? = this.info, error: String? = this.error, loading: Boolean = this.loading,
        selected: Set<Int> = this.selected, expanded: Boolean = this.expanded,
    ) = BatchSource(url, info, error, loading, selected, expanded)
}

/** Um link da galeria, com os arquivos listados (quando o usuário abre a prévia). */
class GalleryLink(
    val url: String,
    val files: List<GalleryFile> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,
    val end: Boolean = false,
    val expanded: Boolean = false,
) {
    fun copy(
        files: List<GalleryFile> = this.files, selected: Set<Int> = this.selected, loading: Boolean = this.loading,
        error: String? = this.error, loaded: Boolean = this.loaded, end: Boolean = this.end, expanded: Boolean = this.expanded,
    ) = GalleryLink(url, files, selected, loading, error, loaded, end, expanded)
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<SubaruApp>()
    val manager = getApplication<SubaruApp>().manager
    val settings = SettingsStore.state
    val engine = Engine.state

    var tab by mutableStateOf(Tab.HOME)
    var queueTab by mutableStateOf(QueueTab.ACTIVE)

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()
    fun toast(msg: String) {
        _toasts.tryEmit(msg)
    }

    fun update(block: (AppSettings) -> AppSettings) = SettingsStore.update(block)
    fun settingsReset() = SettingsStore.reset()

    private suspend fun awaitEngine() {
        Engine.state.first { it.ready || it.error != null }
        if (Engine.state.value.error != null) throw EngineException("As ferramentas não puderam ser preparadas: ${Engine.state.value.error}")
    }

    // ---------- Início ----------

    var homeUrl by mutableStateOf("")
    var homeLoading by mutableStateOf(false)
    var homeError by mutableStateOf<String?>(null)
    var homeHint by mutableStateOf<String?>(null)
    var homeInfo by mutableStateOf<MediaInfo?>(null)
    var homeInfoUrl by mutableStateOf("")
    var homeOptions by mutableStateOf(DownloadOptions())
    private var homeJob: Job? = null

    /** Lê o link. Vários links de uma vez vão para a aba Playlists (lote). */
    fun analyze(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) {
            homeError = "Cole um link que comece com http"
            return
        }
        if (links.size > 1) {
            tab = Tab.PLAYLISTS
            analyzeBatch(links.joinToString("\n"))
            return
        }
        val url = links.first()
        homeUrl = url
        homeError = null
        homeHint = null
        homeInfo = null
        homeLoading = true
        homeJob?.cancel()
        homeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                awaitEngine()
                val info = Ytdlp.fetchInfo(ctx, url, SettingsStore.value)
                withContext(Dispatchers.Main) {
                    homeInfo = info
                    homeInfoUrl = url
                    homeOptions = SettingsStore.value.newOptions().let { o ->
                        // Playlist/canal: a lista para escolher os vídeos já vem aberta, com tudo marcado
                        if (info.isPlaylist) o.copy(playlistItems = "", subfolder = if (o.subfolder == "none") "playlist" else o.subfolder) else o
                    }
                }
            } catch (e: EngineException) {
                homeError = e.message
                homeHint = e.hint
            } catch (e: Exception) {
                if (e !is CancellationException) homeError = e.message ?: "Falha ao ler o link"
            } finally {
                homeLoading = false
            }
        }
    }

    fun clearHome() {
        homeJob?.cancel()
        homeUrl = ""
        homeInfo = null
        homeError = null
        homeHint = null
        homeLoading = false
    }

    fun downloadHome(start: Boolean) {
        val info = homeInfo ?: return
        if (info.isPlaylist && homeOptions.playlistItems.trim() == "0") {
            toast("Marque pelo menos um vídeo da playlist")
            return
        }
        manager.addVideo(info, homeInfoUrl, homeOptions, start)
        toast(if (start) "Download iniciado" else "Adicionado à fila")
        clearHome()
    }

    // ---------- Playlists (lote) ----------

    var batchText by mutableStateOf("")
    var batchLoading by mutableStateOf(false)
    var batchSources by mutableStateOf<List<BatchSource>>(emptyList())
    var batchOptions by mutableStateOf(DownloadOptions(mode = Mode.AUDIO))
    var batchFolderByPlaylist by mutableStateOf(true)
    var batchNumbering by mutableStateOf(true)
    var batchCovers by mutableStateOf(true)
    private var batchJob: Job? = null

    fun analyzeBatch(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) {
            toast("Cole um ou mais links")
            return
        }
        batchText = links.joinToString("\n")
        val base = SettingsStore.value.newOptions()
        batchOptions = if (batchOptions.mode == Mode.AUDIO) base.copy(mode = Mode.AUDIO, audioFormat = batchOptions.audioFormat, audioBitrate = batchOptions.audioBitrate, destination = batchOptions.destination)
        else base.copy(mode = batchOptions.mode, quality = batchOptions.quality, destination = batchOptions.destination)
        batchSources = links.map { BatchSource(it, loading = true) }
        batchLoading = true
        batchJob?.cancel()
        batchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                awaitEngine()
                links.forEachIndexed { i, url ->
                    val result = runCatching { Ytdlp.fetchInfo(ctx, url, SettingsStore.value) }
                    withContext(Dispatchers.Main) {
                        batchSources = batchSources.mapIndexed { j, s ->
                            if (j != i) s else result.fold(
                                { info -> s.copy(info = info, loading = false, error = null, selected = if (info.isPlaylist) info.entries.map { it.index }.toSet() else setOf(1), expanded = links.size == 1) },
                                { e -> s.copy(loading = false, error = (e as? EngineException)?.message ?: e.message ?: "Falha ao ler o link") },
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        batchSources = batchSources.map { it.copy(loading = false, error = it.error ?: e.message) }
                    }
                }
            } finally {
                batchLoading = false
            }
        }
    }

    fun updateSource(index: Int, f: (BatchSource) -> BatchSource) {
        batchSources = batchSources.mapIndexed { i, s -> if (i == index) f(s) else s }
    }

    fun clearBatch() {
        batchJob?.cancel()
        batchSources = emptyList()
        batchText = ""
        batchLoading = false
    }

    fun batchSelectedCount() = batchSources.sumOf { s -> if (s.info?.isPlaylist == true) s.selected.size else if (s.info != null && s.selected.isNotEmpty()) 1 else 0 }

    fun downloadBatch() {
        val o0 = batchOptions
        var count = 0
        for (s in batchSources) {
            val info = s.info ?: continue
            if (s.selected.isEmpty()) continue
            val picked = if (info.isPlaylist && s.selected.size != info.entries.size) compactRanges(s.selected) else ""
            val o = o0.copy(
                playlistItems = picked,
                subfolder = if (info.isPlaylist && batchFolderByPlaylist) "playlist" else "none",
                numbering = batchNumbering,
                embedThumb = batchCovers,
            )
            manager.addVideo(info, s.url, o, true)
            count++
        }
        if (count > 0) {
            toast("$count ${if (count == 1) "lista" else "listas"} na fila")
            clearBatch()
            tab = Tab.QUEUE
        } else toast("Marque pelo menos um item")
    }

    // ---------- Galeria ----------

    var galText by mutableStateOf("")
    var galLinks by mutableStateOf<List<GalleryLink>>(emptyList())
    var galOptions by mutableStateOf(GalleryOptions())

    /** Mostra os links como lista; com um só, a prévia já abre. */
    fun galleryPrepare(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) {
            toast("Cole um link de perfil, post ou álbum")
            return
        }
        galLinks = links.map { GalleryLink(it) }
        galText = ""
        if (links.size == 1) galleryLoad(0, false)
    }

    fun updateGal(index: Int, f: (GalleryLink) -> GalleryLink) {
        galLinks = galLinks.mapIndexed { i, l -> if (i == index) f(l) else l }
    }

    fun galleryLoad(index: Int, more: Boolean) {
        val link = galLinks.getOrNull(index) ?: return
        if (link.loading) return
        updateGal(index) { it.copy(loading = true, error = null, expanded = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                awaitEngine()
                val start = if (more) link.files.size + 1 else 1
                val listing = GalleryDl.list(ctx, link.url, start, 60, SettingsStore.value)
                val all = if (more) link.files + listing.files else listing.files
                withContext(Dispatchers.Main) {
                    updateGal(index) { l ->
                        val firstTime = !l.loaded
                        l.copy(
                            files = all, loading = false, loaded = true, end = listing.end,
                            selected = if (firstTime || !more) all.map { it.index }.toSet() else l.selected + listing.files.map { it.index },
                            error = if (all.isEmpty()) "Nenhum arquivo encontrado nesse link." else null,
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateGal(index) {
                        it.copy(loading = false, error = (e as? EngineException)?.let { x -> listOfNotNull(x.message, x.hint).joinToString("\n") } ?: e.message)
                    }
                }
            }
        }
    }

    fun galleryDownload() {
        var n = 0
        for (l in galLinks) {
            if (l.loaded && l.selected.isEmpty()) continue
            val range = if (l.loaded && l.selected.size != l.files.size) compactRanges(l.selected) else ""
            manager.addGallery(l.url, galleryOptionsNow().copy(range = range))
            n++
        }
        if (n > 0) {
            toast("$n ${if (n == 1) "link" else "links"} na fila")
            galLinks = emptyList()
            tab = Tab.QUEUE
        } else toast("Marque pelo menos um arquivo")
    }

    fun galleryDownloadText(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) {
            toast("Cole um link de perfil, post ou álbum")
            return
        }
        links.forEach { manager.addGallery(it, galleryOptionsNow()) }
        galText = ""
        toast("${links.size} ${if (links.size == 1) "link" else "links"} na fila")
        tab = Tab.QUEUE
    }

    /** As opções da tela, completadas com o padrão das configurações. */
    private fun galleryOptionsNow(): GalleryOptions = galOptions

    // ---------- Entrada de fora (Compartilhar, área de transferência) ----------

    private var lastIncoming = ""

    fun handleIncoming(text: String, fromClipboard: Boolean = false) {
        val links = extractLinks(text)
        if (links.isEmpty()) return
        val key = links.joinToString("\n")
        if (fromClipboard && key == lastIncoming) return
        lastIncoming = key
        if (links.size > 1) {
            tab = Tab.PLAYLISTS
            analyzeBatch(key)
        } else {
            tab = Tab.HOME
            analyze(links.first())
        }
    }

    init {
        val s = SettingsStore.value
        galOptions = GalleryOptions(organize = s.galleryOrganize, customFormat = s.galleryCustom)
    }
}
