package com.subarudl.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.subarudl.app.data.QueueItem
import com.subarudl.app.data.Status
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

/** As duas telas do app: a principal (colar e acompanhar) e os Ajustes. */
enum class Screen { HOME, SETTINGS }

/** O que o próximo link vira: vídeo, áudio ou as imagens de um perfil (gallery-dl). */
enum class LinkJob(val label: String) { VIDEO("Vídeo"), AUDIO("Áudio"), IMAGES("Imagens") }

/** Parte da lista para onde uma notificação leva. */
enum class Section { ACTIVE, DONE, FAILED }

/** Aviso rápido (snackbar), com uma ação opcional como "Desfazer". */
class Toast(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)

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

/** Um link de imagens, com os arquivos listados (quando a prévia é aberta). */
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

/** Sites onde um link costuma ser de fotos: quando o vídeo falha neles, o app oferece baixar como imagens. */
private val imageSites = Regex(
    "instagram\\.com|pinterest\\.|pin\\.it|x\\.com|twitter\\.com|tumblr\\.com|deviantart\\.com|pixiv\\.net|flickr\\.com|" +
        "artstation\\.com|imgur\\.com|reddit\\.com|redd\\.it|bsky\\.app|threads\\.net|facebook\\.com|danbooru|kemono",
    RegexOption.IGNORE_CASE,
)

/** Links que só podem ser vídeo ou áudio: vão para o yt-dlp mesmo com "Imagens" escolhido. */
private val videoOnly = Regex(
    "youtube\\.com|youtu\\.be|tiktok\\.com|vimeo\\.com|twitch\\.tv|soundcloud\\.com|dailymotion\\.com|bandcamp\\.com|kwai|" +
        "\\.(mp4|mkv|webm|mov|m4v|avi|mp3|m4a|aac|ogg|opus|flac|wav)(\\?|$)",
    RegexOption.IGNORE_CASE,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<SubaruApp>()
    val manager = getApplication<SubaruApp>().manager
    val settings = SettingsStore.state
    val engine = Engine.state

    var screen by mutableStateOf(Screen.HOME)
    var settingsPage by mutableStateOf<String?>(null)
    var job by mutableStateOf(if (SettingsStore.value.defaultMode == Mode.AUDIO) LinkJob.AUDIO else LinkJob.VIDEO)

    /** Seção pedida por uma notificação; a tela rola até ela e limpa o pedido. */
    var focus by mutableStateOf<Section?>(null)

    /** Sobe a cada download novo: a lista volta ao topo para mostrar o cartão que entrou. */
    var addedTick by mutableIntStateOf(0)
        private set

    private val _toasts = MutableSharedFlow<Toast>(extraBufferCapacity = 8)
    val toasts: SharedFlow<Toast> = _toasts.asSharedFlow()
    fun toast(msg: String) {
        _toasts.tryEmit(Toast(msg))
    }

    /** Tira da lista com "Desfazer" no aviso; os arquivos ficam no celular. */
    fun clearSection(statuses: Set<Status>, message: String) {
        val removed = manager.items.value.filter { it.status in statuses }
        if (removed.isEmpty()) return
        manager.clearFinished(statuses)
        _toasts.tryEmit(Toast(message, "Desfazer") { manager.restore(removed) })
    }

    fun removeWithUndo(item: QueueItem) {
        manager.remove(item.id)
        _toasts.tryEmit(Toast("Tirado da lista", "Desfazer") { manager.restore(listOf(item)) })
    }

    // ---------- Caminhos de uma falha ----------

    /** O áudio falhou porque o vídeo não tem som: baixa o vídeo, com a pasta padrão de vídeos. */
    fun retryAsVideo(item: QueueItem) = manager.retryWith(item.id) { withMode(it, Mode.VIDEO) }

    /** Não havia vídeo no link: tira o item e abre as imagens desse link. */
    fun retryItemAsImages(item: QueueItem) {
        manager.remove(item.id)
        job = LinkJob.IMAGES
        galleryPrepare(item.url)
    }

    fun retryWithoutTurbo(item: QueueItem) {
        update { it.copy(turbo = false) }
        manager.retry(item.id)
        toast("Turbo desligado nos Ajustes")
    }

    fun update(block: (AppSettings) -> AppSettings) = SettingsStore.update(block)
    fun settingsReset() = SettingsStore.reset()

    fun openSettings(page: String? = null) {
        screen = Screen.SETTINGS
        settingsPage = page
    }

    private suspend fun awaitEngine() {
        Engine.state.first { it.ready || it.error != null }
        if (Engine.state.value.error != null) throw EngineException("As ferramentas não puderam ser preparadas: ${Engine.state.value.error}")
    }

    /** Opções de um download novo no modo pedido, com a pasta padrão desse modo. */
    private fun freshOptions(mode: Mode): DownloadOptions {
        val s = SettingsStore.value
        return s.newOptions().copy(mode = mode, destination = if (mode == Mode.AUDIO) s.audioDest else s.videoDest)
    }

    /** Troca vídeo ↔ áudio levando junto a pasta padrão, se a pasta ainda for a padrão do modo anterior. */
    fun withMode(o: DownloadOptions, mode: Mode): DownloadOptions {
        if (o.mode == mode) return o
        val s = SettingsStore.value
        val oldDefault = if (o.mode == Mode.AUDIO) s.audioDest else s.videoDest
        val newDefault = if (mode == Mode.AUDIO) s.audioDest else s.videoDest
        return o.copy(mode = mode, formatId = null, destination = if (o.destination == oldDefault) newDefault else o.destination)
    }

    // ---------- Um link ----------

    var homeUrl by mutableStateOf("")
    var homeLoading by mutableStateOf(false)
    var homeError by mutableStateOf<String?>(null)
    var homeHint by mutableStateOf<String?>(null)
    var homeInfo by mutableStateOf<MediaInfo?>(null)
    var homeInfoUrl by mutableStateOf("")
    var homeOptions by mutableStateOf(DownloadOptions())
    private var homeJob: Job? = null

    /** O que a pessoa colou ou compartilhou: decide entre um link, vários (lote) ou imagens. */
    fun submit(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) {
            homeError = "Isso não parece um link. Copie o endereço que começa com http."
            homeHint = null
            return
        }
        // Com "Imagens", um link que só pode ser vídeo (YouTube, arquivo .mp4…) vai para o vídeo e a escolha acompanha
        if (job == LinkJob.IMAGES && links.all { videoOnly.containsMatchIn(it) }) job = LinkJob.VIDEO
        when {
            job == LinkJob.IMAGES -> galleryPrepare(links.joinToString("\n"))
            links.size > 1 -> analyzeBatch(links.joinToString("\n"))
            else -> analyze(links.first())
        }
    }

    fun analyze(url: String) {
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
                    homeOptions = freshOptions(if (job == LinkJob.AUDIO) Mode.AUDIO else Mode.VIDEO).let { o ->
                        // Playlist/canal: todos os itens marcados e uma pasta com o nome da lista
                        if (info.isPlaylist) o.copy(playlistItems = "", subfolder = if (o.subfolder == "none") "playlist" else o.subfolder) else o
                    }
                }
            } catch (e: EngineException) {
                homeError = e.message
                homeHint = e.hint
            } catch (e: Exception) {
                if (e !is CancellationException) homeError = e.message ?: "Não consegui ler esse link"
            } finally {
                homeLoading = false
            }
        }
    }

    /** O link falhou como vídeo num site de fotos: dá para tentar pelas imagens. */
    val canTryImages get() = homeError != null && imageSites.containsMatchIn(homeUrl)

    fun retryAsImages() {
        val url = homeUrl
        clearHome()
        job = LinkJob.IMAGES
        galleryPrepare(url)
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
        addedTick++
    }

    // ---------- Vários links (lote) ----------

    var batchLoading by mutableStateOf(false)
    var batchSources by mutableStateOf<List<BatchSource>>(emptyList())
    var batchOptions by mutableStateOf(DownloadOptions())
    var batchFolderByPlaylist by mutableStateOf(true)
    var batchNumbering by mutableStateOf(true)
    var batchCovers by mutableStateOf(true)
    private var batchJob: Job? = null

    fun analyzeBatch(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) return
        homeError = null
        batchOptions = freshOptions(if (job == LinkJob.AUDIO) Mode.AUDIO else Mode.VIDEO)
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
                                { e -> s.copy(loading = false, error = (e as? EngineException)?.let { x -> x.hint ?: x.message } ?: e.message ?: "Não consegui ler esse link") },
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
            toast(if (count == 1) "1 link na fila" else "$count links na fila")
            clearBatch()
            addedTick++
        } else toast("Marque pelo menos um item")
    }

    // ---------- Imagens (gallery-dl) ----------

    var galLinks by mutableStateOf<List<GalleryLink>>(emptyList())
    var galOptions by mutableStateOf(GalleryOptions())

    /** Mostra os links; com um só, a prévia já abre. */
    fun galleryPrepare(raw: String) {
        val links = extractLinks(raw)
        if (links.isEmpty()) return
        homeError = null
        val s = SettingsStore.value
        galOptions = GalleryOptions(organize = s.galleryOrganize, customFormat = s.galleryCustom)
        galLinks = links.map { GalleryLink(it) }
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
                            error = if (all.isEmpty()) emptyGalleryMessage(link.url) else null,
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

    /** Nada listado: em sites que escondem as fotos de quem não entrou na conta, diz o caminho. */
    private fun emptyGalleryMessage(url: String): String =
        if (Regex("instagram\\.com|facebook\\.com|x\\.com|twitter\\.com|threads\\.net|pixiv\\.net", RegexOption.IGNORE_CASE).containsMatchIn(url))
            "Não achei fotos. Esse site costuma mostrar as fotos só para quem entrou na conta: entre em Ajustes → Contas e cookies."
        else "Não achei fotos nesse link."

    /** O link não é de fotos: tenta como vídeo. */
    fun retryAsVideo(url: String) {
        galLinks = emptyList()
        job = LinkJob.VIDEO
        analyze(url)
    }

    /** Quantos links vão para a fila: os não abertos baixam tudo; os abertos, só o que estiver marcado. */
    fun galleryCount() = galLinks.count { !it.loaded || it.selected.isNotEmpty() }

    fun galleryDownload() {
        var n = 0
        for (l in galLinks) {
            if (l.loaded && l.selected.isEmpty()) continue
            val range = if (l.loaded && l.selected.size != l.files.size) compactRanges(l.selected) else ""
            manager.addGallery(l.url, galOptions.copy(range = range))
            n++
        }
        if (n > 0) {
            toast(if (n == 1) "1 link na fila" else "$n links na fila")
            galLinks = emptyList()
            addedTick++
        } else toast("Marque pelo menos um arquivo")
    }

    // ---------- Entrada de fora (Compartilhar, área de transferência) ----------

    private var lastIncoming = ""

    /** O mesmo link copiado só é lido sozinho uma vez. */
    private var lastClipboard = ""

    fun handleIncoming(text: String, fromClipboard: Boolean = false) {
        val links = extractLinks(text)
        if (links.isEmpty()) return
        val key = links.joinToString("\n")
        if (fromClipboard && (key == lastIncoming || key == lastClipboard)) return
        // O link copiado nunca atropela um link que já está sendo lido ou uma folha aberta
        if (fromClipboard && (homeLoading || homeInfo != null || batchSources.isNotEmpty() || galLinks.isNotEmpty())) return
        if (fromClipboard) lastClipboard = key
        lastIncoming = key
        screen = Screen.HOME
        settingsPage = null
        submit(key)
    }
}
