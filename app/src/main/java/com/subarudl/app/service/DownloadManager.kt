package com.subarudl.app.service

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.subarudl.app.data.AppJson
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.GalleryOptions
import com.subarudl.app.data.Kind
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.Mode
import com.subarudl.app.data.OutputFile
import com.subarudl.app.data.QueueItem
import com.subarudl.app.data.SettingsStore
import com.subarudl.app.data.Status
import com.subarudl.app.data.Storage
import com.subarudl.app.engine.Args
import com.subarudl.app.engine.Engine
import com.subarudl.app.engine.EngineException
import com.subarudl.app.engine.GalleryDl
import com.subarudl.app.engine.Ytdlp
import com.subarudl.app.engine.errorHint
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class NetState(val online: Boolean = true, val wifi: Boolean = true, val label: String = "Online")

/** A fila de downloads: roda os trabalhos em segundo plano, guarda o histórico e avisa a interface. */
class DownloadManager(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _items = MutableStateFlow<List<QueueItem>>(emptyList())
    val items: StateFlow<List<QueueItem>> = _items.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<Float>>(emptyList())
    val speedHistory: StateFlow<List<Float>> = _speedHistory.asStateFlow()
    private val _peak = MutableStateFlow(0f)
    val peak: StateFlow<Float> = _peak.asStateFlow()

    private val _net = MutableStateFlow(NetState())
    val net: StateFlow<NetState> = _net.asStateFlow()

    private val jobs = ConcurrentHashMap<String, Job>()
    /** Itens interrompidos de propósito (pausa, sem Wi-Fi…): o erro que o processo der ao morrer não conta. */
    private val interrupted = ConcurrentHashMap.newKeySet<String>()
    private val lastEmit = ConcurrentHashMap<String, Long>()
    private val queueFile get() = File(app.filesDir, "queue.json")
    private var saveJob: Job? = null
    @Volatile private var serviceStarted = false

    val busy: Boolean get() = _items.value.any { it.active || (it.status == Status.WAITING && canStart()) }

    // ---------- Início ----------

    fun start() {
        load()
        watchNetwork()
        scope.launch { Engine.init(app); tick(); autoUpdate() }
        scope.launch {
            // Amostras da velocidade total, 1 por segundo, para o gráfico da Fila
            while (true) {
                delay(1000)
                val total = _items.value.filter { it.active }.sumOf { it.speed }.toFloat()
                _speedHistory.update { (it + total).takeLast(60) }
                if (total > _peak.value) _peak.value = total
            }
        }
    }

    private fun autoUpdate() {
        val s = SettingsStore.value
        if (!s.autoUpdateTools || System.currentTimeMillis() - s.lastToolsCheck < 12 * 3600_000L || !_net.value.online) return
        val msg = Engine.updateYtdlp(app, waitSeconds = 0)
        // Se estava ocupado, não conta como verificado: tenta de novo na próxima abertura
        if (!msg.startsWith("Há downloads")) SettingsStore.update { it.copy(lastToolsCheck = System.currentTimeMillis()) }
    }

    private fun load() {
        val loaded = runCatching { AppJson.decodeFromString(ListSerializer(QueueItem.serializer()), queueFile.readText()) }.getOrDefault(emptyList())
        // O que estava baixando quando o app fechou volta para a fila e continua de onde parou
        _items.value = loaded.map {
            if (it.active) it.copy(status = Status.WAITING, speed = 0.0, eta = -1, stage = "") else it
        }
        Storage.cleanWorkDirs(app, _items.value.filter { it.status != Status.DONE && it.status != Status.ERROR }.map { it.id }.toSet())
    }

    private fun saveSoon() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(800)
            runCatching { queueFile.writeText(AppJson.encodeToString(ListSerializer(QueueItem.serializer()), _items.value)) }
        }
    }

    private fun mutate(id: String, persist: Boolean = true, f: (QueueItem) -> QueueItem) {
        _items.update { list -> list.map { if (it.id == id) f(it) else it } }
        if (persist) saveSoon()
    }

    fun item(id: String) = _items.value.firstOrNull { it.id == id }

    // ---------- Rede ----------

    private fun watchNetwork() {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        fun read(n: Network?): NetState {
            val caps = n?.let { cm.getNetworkCapabilities(it) } ?: return NetState(online = false, wifi = false, label = "Offline")
            val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val label = when {
                !online -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cabo"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados móveis"
                else -> "Online"
            }
            return NetState(online, wifi, label)
        }
        _net.value = read(cm.activeNetwork)
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = changed(read(network))
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = changed(read(network))
                override fun onLost(network: Network) = changed(NetState(online = false, wifi = false, label = "Offline"))
            })
        }
    }

    private fun changed(n: NetState) {
        if (n == _net.value) return
        _net.value = n
        if (SettingsStore.value.wifiOnly && !n.wifi) {
            // Saiu do Wi-Fi: o que estava baixando espera (o progresso fica guardado)
            _items.value.filter { it.active }.forEach { interrupt(it.id, "Aguardando Wi-Fi") }
        }
        tick()
    }

    private fun canStart(): Boolean {
        val n = _net.value
        return n.online && (!SettingsStore.value.wifiOnly || n.wifi)
    }

    // ---------- Fila ----------

    fun addVideo(info: MediaInfo, url: String, options: DownloadOptions, start: Boolean = true): QueueItem {
        val item = QueueItem(
            id = UUID.randomUUID().toString(),
            url = url,
            title = info.title,
            channel = info.channel,
            site = info.site,
            thumb = info.thumb,
            duration = info.duration,
            isPlaylist = info.isPlaylist,
            isLive = info.isLive,
            options = options,
            status = if (start) Status.WAITING else Status.PAUSED,
            itemCount = if (info.isPlaylist) info.playlistTotal else 0,
        )
        add(item)
        return item
    }

    fun addGallery(url: String, options: GalleryOptions, title: String? = null, start: Boolean = true): QueueItem {
        val host = runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: url
        val item = QueueItem(
            id = UUID.randomUUID().toString(), kind = Kind.GALLERY, url = url, title = title ?: url.removePrefix("https://").removePrefix("http://").removePrefix("www."),
            site = host, gallery = options, status = if (start) Status.WAITING else Status.PAUSED,
        )
        add(item)
        return item
    }

    private fun add(item: QueueItem) {
        _items.update { it + item }
        saveSoon()
        tick()
    }

    fun pause(id: String) {
        val it = item(id) ?: return
        when (it.status) {
            Status.DOWNLOADING, Status.PROCESSING -> interrupt(id, "Pausado", Status.PAUSED)
            Status.WAITING -> mutate(id) { x -> x.copy(status = Status.PAUSED) }
            else -> Unit
        }
        tick()
    }

    fun pauseAll() = _items.value.filter { it.active || it.status == Status.WAITING }.forEach { pause(it.id) }

    fun resume(id: String) {
        val it = item(id) ?: return
        if (it.status == Status.PAUSED || it.status == Status.ERROR) mutate(id) { x -> x.copy(status = Status.WAITING, error = null, hint = null, stage = "") }
        tick()
    }

    fun resumeAll() = _items.value.filter { it.status == Status.PAUSED }.forEach { resume(it.id) }

    /** Tentar de novo: recomeça a contagem de tentativas; o que já foi baixado é aproveitado. */
    fun retry(id: String) {
        mutate(id) { x ->
            x.copy(
                status = Status.WAITING, error = null, hint = null, attempt = 0, progress = 0f, speed = 0.0, eta = -1, stage = "",
                fileCount = if (x.kind == Kind.GALLERY) 0 else x.fileCount, skipped = 0, failed = 0,
            )
        }
        tick()
    }

    fun cancel(id: String) = remove(id)

    fun remove(id: String, deleteFiles: Boolean = false) {
        val it = item(id) ?: return
        interrupted.add(id)
        killProcess(it)
        if (deleteFiles) it.outputs.forEach { f -> Storage.delete(app, f.uri) }
        File(app.getExternalFilesDir("downloads"), id).deleteRecursively()
        _items.update { l -> l.filterNot { x -> x.id == id } }
        saveSoon()
        tick()
    }

    fun clearFinished(statuses: Set<Status>) {
        _items.update { l -> l.filterNot { it.status in statuses } }
        saveSoon()
    }

    /** Desfazer: põe de volta itens tirados da lista (os arquivos nunca saíram do celular). */
    fun restore(back: List<QueueItem>) {
        _items.update { l -> l + back.filter { b -> l.none { it.id == b.id } } }
        saveSoon()
    }

    /** Tenta de novo com outras opções (ex.: o áudio falhou porque o vídeo não tem som, então baixa como vídeo). */
    fun retryWith(id: String, change: (DownloadOptions) -> DownloadOptions) {
        mutate(id) { x -> x.copy(options = change(x.options)) }
        retry(id)
    }

    fun clearQueue() {
        _items.value.filter { it.status != Status.DONE && it.status != Status.ERROR }.forEach { remove(it.id) }
    }

    private fun killProcess(it: QueueItem) {
        if (it.kind == Kind.GALLERY) GalleryDl.cancel(it.id) else Ytdlp.cancel(it.id)
    }

    private fun interrupt(id: String, why: String, to: Status = Status.WAITING) {
        interrupted.add(id)
        val it = item(id) ?: return
        mutate(id) { x -> x.copy(status = to, speed = 0.0, eta = -1, stage = why) }
        killProcess(it)
    }

    private fun tick() {
        synchronized(this) {
            val limit = SettingsStore.value.concurrent.coerceIn(1, 5)
            var running = _items.value.count { it.active }
            if (!Engine.state.value.ready) return
            if (!canStart()) {
                _items.value.filter { it.status == Status.WAITING && it.stage.isEmpty() }.forEach { w ->
                    mutate(w.id, persist = false) { x -> x.copy(stage = if (!_net.value.online) "Sem internet" else "Aguardando Wi-Fi") }
                }
                return
            }
            for (next in _items.value.filter { it.status == Status.WAITING }) {
                if (running >= limit) break
                if (jobs.containsKey(next.id)) continue
                launchJob(next.id)
                running++
            }
        }
        if (busy) ensureService()
    }

    private fun ensureService() {
        if (serviceStarted) return
        serviceStarted = true
        runCatching { ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java)) }
            .onFailure { serviceStarted = false; AppLog.w("Não consegui iniciar o serviço de downloads: ${it.message}") }
    }

    fun serviceStopped() {
        serviceStarted = false
    }

    // ---------- Trabalhos ----------

    private fun launchJob(id: String) {
        interrupted.remove(id)
        mutate(id) { x -> x.copy(status = Status.DOWNLOADING, error = null, hint = null, stage = "Iniciando", speed = 0.0, eta = -1) }
        jobs[id] = scope.launch {
            try {
                val item = item(id) ?: return@launch
                if (item.kind == Kind.GALLERY) runGallery(item) else runVideo(item)
            } catch (e: Throwable) {
                if (item(id) != null && !interrupted.contains(id)) fail(id, e.message ?: e.javaClass.simpleName, null)
            } finally {
                jobs.remove(id)
                lastEmit.remove(id)
                tick()
            }
        }
    }

    private fun fail(id: String, error: String, hint: String?) {
        val it = item(id) ?: return
        val canRetry = Regex("403").containsMatchIn(error) && Regex("youtu\\.?be", RegexOption.IGNORE_CASE).containsMatchIn(it.url) && it.attempt < 3
        if (canRetry) {
            AppLog.w("403 — tentando de novo com outro cliente (tentativa ${it.attempt + 1})", id)
            mutate(id) { x -> x.copy(status = Status.WAITING, attempt = x.attempt + 1, error = error, stage = "Tentando de novo", speed = 0.0, eta = -1) }
            scope.launch { delay(3000); tick() }
            return
        }
        AppLog.e(error, id)
        mutate(id) { x -> x.copy(status = Status.ERROR, error = error, hint = hint, speed = 0.0, eta = -1, stage = "", finishedAt = System.currentTimeMillis()) }
        item(id)?.let { Notifier.finished(app, it, false) }
    }

    /** Linha de progresso do aria2c (Turbo): `[#a1b2c3 16KiB/422KiB(3%) CN:1 DL:29KiB ETA:13s]` */
    private val aria2Line = Regex("""^\[#\w+ ([\d.]+)([KMGT]?i?B)/([\d.]+)([KMGT]?i?B)(?:\((\d+)%\))? CN:\d+(?: SD:\d+)? DL:([\d.]+)([KMGT]?i?B)(?: ETA:(\S+))?]""")

    private fun aria2Bytes(n: String, unit: String): Double =
        n.toDouble() * when (unit.first()) { 'K' -> 1024.0; 'M' -> 1048576.0; 'G' -> 1073741824.0; 'T' -> 1099511627776.0; else -> 1.0 }

    private fun aria2Eta(text: String): Long =
        if (text.isEmpty()) -1 else Regex("(\\d+)([dhms])").findAll(text).sumOf { m ->
            m.groupValues[1].toLong() * when (m.groupValues[2]) { "d" -> 86400L; "h" -> 3600L; "m" -> 60L; else -> 1L }
        }

    private fun num(v: String?): Double? {
        if (v == null || v == "NA" || v == "None" || v.isEmpty()) return null
        return v.toDoubleOrNull()
    }

    private fun runVideo(item: QueueItem) {
        val id = item.id
        val s = SettingsStore.value
        val work = Storage.workDir(app, id)
        val delivered = mutableListOf<OutputFile>()
        var quality = item.quality
        var ext = item.ext

        // Número novo a cada download: só as linhas com ele são do próprio app (veja Args.Marks)
        val marks = Args.Marks.random()

        fun deliver(file: File) {
            Storage.deliver(app, work, file, item.options.destination)?.let { delivered += it }
        }

        val onLine: (String) -> Unit = { line ->
            when {
                line.startsWith(marks.progress) -> {
                    val p = line.removePrefix(marks.progress).split("|")
                    val status = p.getOrNull(0)
                    val downloaded = num(p.getOrNull(1))
                    val total = num(p.getOrNull(2)) ?: num(p.getOrNull(3))
                    var fraction = if (downloaded != null && total != null && total > 0) downloaded / total else null
                    val frag = num(p.getOrNull(9))
                    val nFrag = num(p.getOrNull(10))
                    if (fraction == null && frag != null && nFrag != null && nFrag > 0) fraction = frag / nFrag
                    if (status == "finished") fraction = 1.0
                    val idx = num(p.getOrNull(6))?.toInt()
                    val n = num(p.getOrNull(7))?.toInt()
                    var percent = (fraction ?: 0.0) * 100
                    if (idx != null && n != null && n > 1) percent = (idx - 1 + (fraction ?: 0.0)) / n * 100
                    val audioOnly = p.getOrNull(8) == "none" || item.options.mode == Mode.AUDIO
                    val now = System.currentTimeMillis()
                    if (status == "finished" || now - (lastEmit[id] ?: 0) > 250) {
                        lastEmit[id] = now
                        if (item(id)?.status != Status.PAUSED && !interrupted.contains(id)) {
                            mutate(id, persist = false) { x ->
                                x.copy(
                                    status = Status.DOWNLOADING,
                                    progress = (percent / 100).toFloat().coerceIn(0f, 1f),
                                    downloaded = downloaded?.toLong() ?: x.downloaded,
                                    total = total?.toLong() ?: x.total,
                                    speed = num(p.getOrNull(4)) ?: 0.0,
                                    eta = num(p.getOrNull(5))?.toLong() ?: -1,
                                    stage = if (audioOnly) "Baixando áudio" else "Baixando vídeo",
                                    itemIndex = idx ?: x.itemIndex,
                                    itemCount = n ?: x.itemCount,
                                )
                            }
                        }
                    }
                }
                line.startsWith(marks.post) -> {
                    val p = line.removePrefix(marks.post).split("|")
                    if (p.getOrNull(0) == "started") {
                        val stage = postStage(p.getOrNull(1) ?: "")
                        val multi = (item(id)?.itemCount ?: 0) > 1 && item.isPlaylist
                        mutate(id, persist = false) { x -> x.copy(status = if (multi) Status.DOWNLOADING else Status.PROCESSING, stage = stage, speed = 0.0, eta = -1) }
                    }
                }
                line.startsWith(marks.file) -> {
                    runCatching {
                        val j = JSONObject(line.removePrefix(marks.file))
                        val path = j.optString("filepath")
                        val height = j.optInt("height").takeIf { it > 0 }
                        val fps = j.optDouble("fps", 0.0)
                        val abr = j.optDouble("abr", 0.0)
                        quality = if (item.options.mode == Mode.AUDIO) (if (abr > 0) "${abr.toInt()}k" else item.options.audioBitrate.toString() + "k")
                        else if (height != null) "${height}p" + (if (fps > 30) fps.toInt().toString() else "") else quality
                        ext = j.optString("ext").ifEmpty { ext }
                        mutate(id, persist = false) { x ->
                            x.copy(
                                title = if (x.title == x.url || x.title.isBlank()) j.optString("title", x.title) else x.title,
                                channel = x.channel ?: j.optString("channel").ifEmpty { j.optString("uploader") }.ifEmpty { null },
                                thumb = x.thumb ?: j.optString("thumbnail").ifEmpty { null },
                                duration = x.duration ?: j.optLong("duration").takeIf { it > 0 },
                                quality = quality, ext = ext,
                            )
                        }
                        // Só arquivos de dentro da pasta deste download (o Storage.deliver confere de novo)
                        if (item.isPlaylist && path.isNotEmpty()) File(path).takeIf { it.isFile && Storage.relativeInside(work, it) != null }?.let(::deliver)
                    }
                }
                line.startsWith("[#") && aria2Line.containsMatchIn(line.trim()) -> {
                    // O aria2c escreve o próprio progresso (o yt-dlp não repassa): vira a barra do app
                    val m = aria2Line.find(line.trim())!!
                    val done = aria2Bytes(m.groupValues[1], m.groupValues[2])
                    val total = aria2Bytes(m.groupValues[3], m.groupValues[4])
                    val speed = aria2Bytes(m.groupValues[6], m.groupValues[7])
                    val fraction = if (total > 0) done / total else 0.0
                    val now = System.currentTimeMillis()
                    if (now - (lastEmit[id] ?: 0) > 250 && item(id)?.status != Status.PAUSED && !interrupted.contains(id)) {
                        lastEmit[id] = now
                        mutate(id, persist = false) { x ->
                            val n = x.itemCount
                            val overall = if (n > 1 && x.itemIndex >= 1) (x.itemIndex - 1 + fraction) / n else fraction
                            x.copy(
                                status = Status.DOWNLOADING,
                                progress = overall.toFloat().coerceIn(0f, 1f),
                                downloaded = done.toLong(), total = total.toLong(), speed = speed,
                                eta = aria2Eta(m.groupValues[8]),
                                stage = if (item.options.mode == Mode.AUDIO) "Baixando áudio" else "Baixando vídeo",
                            )
                        }
                    }
                }
                line.isNotBlank() -> if (line.startsWith("[") || line.startsWith("WARNING") || line.startsWith("ERROR")) AppLog.i(line.trim(), id)
            }
        }

        try {
            Ytdlp.download(app, item, s, work, marks, onLine)
        } catch (e: YoutubeDL.CanceledException) {
            return // pausado, sem Wi-Fi ou removido: a pasta de trabalho fica para continuar depois
        } catch (e: EngineException) {
            if (interrupted.contains(id)) return
            fail(id, e.message ?: "Falha no download", e.hint ?: errorHint(e.message ?: ""))
            return
        }
        if (interrupted.contains(id) || item(id) == null) return

        // Entrega o que sobrou (legendas, capas, o próprio vídeo quando não é lista)
        mutate(id, persist = false) { x -> x.copy(status = Status.PROCESSING, stage = "Salvando", speed = 0.0, eta = -1) }
        work.walkTopDown().filter { it.isFile && !Storage.isTemp(it.name) }.sortedBy { it.name }.forEach(::deliver)
        work.deleteRecursively()
        if (delivered.isEmpty()) {
            fail(id, "Nada foi baixado. O vídeo pode estar indisponível ou filtrado.", null)
            return
        }
        val total = delivered.sumOf { it.size }
        mutate(id) { x ->
            x.copy(
                status = Status.DONE, progress = 1f, speed = 0.0, eta = -1, stage = "", error = null, hint = null,
                finishedAt = System.currentTimeMillis(), outputs = delivered.take(120), fileCount = delivered.size,
                quality = quality, ext = ext, sizeBytes = total, downloaded = total,
            )
        }
        trimHistory()
        AppLog.i("Concluído: ${item(id)?.title}", id)
        item(id)?.let { Notifier.finished(app, it, true) }
    }

    private fun postStage(name: String): String = when {
        Regex("Merger", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Juntando vídeo e áudio"
        Regex("ExtractAudio", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Convertendo o áudio"
        Regex("VideoConvertor", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Convertendo o vídeo"
        Regex("VideoRemuxer", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Ajustando o formato"
        Regex("EmbedSubtitle", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Embutindo as legendas"
        Regex("EmbedThumbnail", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Embutindo a capa"
        Regex("ThumbnailsConvertor", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Convertendo a capa"
        Regex("SubtitlesConvertor", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Convertendo as legendas"
        Regex("Metadata", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Gravando as informações"
        Regex("SplitChapters|ModifyChapters", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "Separando os capítulos"
        else -> "Processando"
    }

    private fun trimHistory() {
        val done = _items.value.filter { it.status == Status.DONE }
        if (done.size > 300) {
            val drop = done.sortedBy { it.finishedAt }.take(done.size - 300).map { it.id }.toSet()
            _items.update { l -> l.filterNot { it.id in drop } }
        }
    }

    private fun runGallery(item: QueueItem) {
        val id = item.id
        val s = SettingsStore.value
        val g = item.gallery ?: GalleryOptions()
        val work = Storage.workDir(app, id)
        val dest = g.destination ?: s.galleryDest
        var files = 0
        var skipped = 0
        var failed = 0
        var lastError = ""
        var hint: String? = null
        val samples = mutableListOf<OutputFile>()
        val args = GalleryDl.downloadArgs(app, item.url, g, s, work)

        val code = try {
            GalleryDl.download(
                app, id, args,
                onLine = { line ->
                    if (line.isNotBlank()) {
                        val skip = line.startsWith("# ")
                        val path = (if (skip) line.drop(2) else line).trim()
                        if (skip) skipped++ else {
                            val f = File(path)
                            if (f.isFile && Storage.relativeInside(work, f) != null) {
                                val out = Storage.deliver(app, work, f, dest)
                                if (out != null) {
                                    files++
                                    if (out.mime.startsWith("image/") && samples.size < 8) samples += out
                                }
                            } else files++
                        }
                        val now = System.currentTimeMillis()
                        if (now - (lastEmit[id] ?: 0) > 300) {
                            lastEmit[id] = now
                            val sampleList = samples.toList()
                            mutate(id, persist = false) { x -> x.copy(fileCount = files, skipped = skipped, failed = failed, stage = "Baixando arquivos", outputs = sampleList) }
                        }
                    }
                },
                onErr = { line ->
                    when {
                        Regex("\\[error]", RegexOption.IGNORE_CASE).containsMatchIn(line) -> {
                            lastError = line.replace(Regex("^\\[[^]]+]\\[error]\\s*", RegexOption.IGNORE_CASE), "")
                            AppLog.e(line, id)
                            if (Regex("Failed to download", RegexOption.IGNORE_CASE).containsMatchIn(line)) failed++
                        }
                        Regex("\\[warning]", RegexOption.IGNORE_CASE).containsMatchIn(line) -> {
                            AppLog.w(line, id)
                            if (hint == null) hint = GalleryDl.galleryHint(line)
                        }
                        line.isNotBlank() -> AppLog.i(line, id)
                    }
                },
            )
        } catch (e: Throwable) {
            if (interrupted.contains(id) || item(id) == null) return
            fail(id, "Não foi possível iniciar o gallery-dl: ${e.message}", null)
            return
        }
        if (interrupted.contains(id) || item(id) == null) return

        // Termina: arquivos que sobraram na pasta de trabalho também são entregues
        work.walkTopDown().filter { it.isFile && !Storage.isTemp(it.name) }.forEach { f ->
            Storage.deliver(app, work, f, dest)?.let { files++ }
        }
        work.deleteRecursively()
        val nothing = files == 0 && skipped == 0
        val finalHint = hint ?: GalleryDl.galleryHint(lastError)
        val now = System.currentTimeMillis()
        val sampleList = samples.toList()
        when {
            code == 0 -> mutate(id) { x ->
                x.copy(
                    status = Status.DONE, progress = 1f, stage = "", fileCount = files, skipped = skipped, failed = failed, finishedAt = now, outputs = sampleList,
                    error = if (nothing) "Nada encontrado para baixar nesse link." else null, hint = finalHint,
                )
            }
            !nothing -> mutate(id) { x ->
                x.copy(
                    status = Status.DONE, progress = 1f, stage = "", fileCount = files, skipped = skipped, failed = failed, finishedAt = now, outputs = sampleList,
                    error = if (failed > 0) "$failed arquivo(s) falharam. Toque em tentar de novo para baixar só os que faltam." else lastError.ifEmpty { null }, hint = finalHint,
                )
            }
            else -> {
                val msg = if (code == 64) "Site não suportado pelo gallery-dl" else lastError.ifEmpty { "O gallery-dl terminou com o código $code" }
                fail(id, msg, finalHint)
                return
            }
        }
        trimHistory()
        item(id)?.let { Notifier.finished(app, it, it.error == null || it.fileCount > 0) }
    }
}
