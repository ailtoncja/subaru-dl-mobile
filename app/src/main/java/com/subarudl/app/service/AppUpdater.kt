package com.subarudl.app.service

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.subarudl.app.BuildConfig
import com.subarudl.app.R
import com.subarudl.app.SubaruApp
import com.subarudl.app.data.AppJson
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.SettingsStore
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import android.app.DownloadManager as SystemDownloads

/** Uma versão publicada nas Releases do GitHub, com o APK. */
@Serializable
data class AppRelease(
    val version: String,
    val apkUrl: String,
    val size: Long,
    val sha256: String? = null,
    val notes: String = "",
)

enum class UpdatePhase { IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY, INSTALLING, FAILED }

data class UpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val release: AppRelease? = null,
    val done: Long = 0,
    val total: Long = 0,
    /** O download foi pedido só no Wi-Fi e está parado esperando por ele. */
    val waitingWifi: Boolean = false,
    /** Resultado da última procura ou o motivo da falha. */
    val message: String? = null,
)

/**
 * Versão nova do app (igual ao atualizador do PC): procura na Release mais nova do GitHub ao abrir o app (a cada 6 horas),
 * baixa o APK pelo gerenciador de downloads do Android (sozinho só no Wi-Fi), confere se é mesmo uma versão mais nova do
 * Subaru DL, assinada pela mesma chave, e instala por cima quando a pessoa toca em Instalar.
 */
object AppUpdater {
    /** A Release mais nova do GitHub (o build de teste aponta para um servidor local). */
    private const val API = BuildConfig.UPDATE_API
    /** O APK só pode vir das Releases deste repositório. */
    private const val ORIGIN = BuildConfig.UPDATE_ORIGIN
    private const val PREFS = "atualizacao"
    private const val DIR = "atualizacao"
    private const val CHECK_EVERY = 6 * 3600_000L
    const val ACTION_INSTALL_STATUS = "com.subarudl.app.INSTALL_STATUS"
    private const val NOTIFY_ID = 7001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** O app está na frente: o aviso da tela basta, sem notificação de "pronta". */
    @Volatile var foreground = false

    /** Versão que acabou de entrar por aqui: a tela mostra "atualizado" e a notificação some. */
    private val _justUpdated = MutableStateFlow<String?>(null)
    val justUpdated: StateFlow<String?> = _justUpdated.asStateFlow()

    /** Foi pedir a permissão de instalar apps: ao voltar, a instalação continua. */
    @Volatile private var awaitingPermission = false
    private var watchJob: Job? = null
    private val finishing = Mutex()
    private lateinit var app: Context

    private fun prefs(): SharedPreferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun downloads() = app.getSystemService(SystemDownloads::class.java)

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val p = prefs()
        val release = p.getString("release", null)?.let { runCatching { AppJson.decodeFromString(AppRelease.serializer(), it) }.getOrNull() }
        // Já está nessa versão (atualizou por aqui ou instalou o APK à mão): o arquivo baixado não serve mais
        if (release == null || !isNewer(release.version)) {
            forget()
            return
        }
        _state.value = when {
            p.getString("ready", null) == release.version && readyFile() != null -> UpdateState(UpdatePhase.READY, release)
            p.getLong("downloadId", -1) >= 0 -> UpdateState(UpdatePhase.DOWNLOADING, release, total = release.size).also { watch() }
            else -> UpdateState(UpdatePhase.AVAILABLE, release)
        }
    }

    // ---------- Procurar ----------

    /** Ao abrir o app: procura se o ajuste estiver ligado e a última procura tiver mais de 6 horas. */
    fun checkIfDue() {
        val s = SettingsStore.value
        if (!s.autoUpdateApp || System.currentTimeMillis() - s.lastAppCheck < CHECK_EVERY) return
        if (!network().online) return
        check(manual = false)
    }

    /** "Verificar" nos Ajustes, ou a procura automática. Achando versão nova no Wi-Fi, já começa a baixar. */
    fun check(manual: Boolean) {
        val before = _state.value
        if (before.phase == UpdatePhase.CHECKING || before.phase == UpdatePhase.DOWNLOADING ||
            before.phase == UpdatePhase.READY || before.phase == UpdatePhase.INSTALLING
        ) return
        _state.update { it.copy(phase = UpdatePhase.CHECKING, message = null) }
        scope.launch {
            val result = runCatching { fetchLatest() }
            result.onSuccess { found ->
                SettingsStore.update { it.copy(lastAppCheck = System.currentTimeMillis()) }
                if (found == null || !isNewer(found.version)) {
                    forget()
                    _state.value = UpdateState(message = if (manual) "Você já tem a versão mais nova." else null)
                    return@launch
                }
                if (before.release?.version != found.version) forget()
                prefs().edit().putString("release", AppJson.encodeToString(AppRelease.serializer(), found)).apply()
                _state.value = UpdateState(UpdatePhase.AVAILABLE, found)
                AppLog.i("Versão nova do app: ${found.version}")
                if (network().unmetered && (manual || SettingsStore.value.autoUpdateApp)) download(allowMetered = false)
            }
            result.onFailure { e ->
                AppLog.w("Procurar versão nova do app: ${e.message}")
                _state.value = before.copy(
                    phase = if (before.release != null) UpdatePhase.AVAILABLE else UpdatePhase.IDLE,
                    message = if (manual) "Não consegui procurar: sem internet ou o GitHub não respondeu." else before.message,
                )
            }
        }
    }

    private fun fetchLatest(): AppRelease? {
        val c = URL(API).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "SubaruDL-Android/${BuildConfig.VERSION_NAME}")
        try {
            if (c.responseCode == 404) return null // nenhuma Release publicada ainda
            if (c.responseCode != 200) error("o GitHub respondeu ${c.responseCode}")
            val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val assets = j.optJSONArray("assets") ?: return null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) } ?: return null
            val url = apk.optString("browser_download_url")
            if (!url.startsWith(ORIGIN)) return null
            return AppRelease(
                version = j.optString("tag_name").removePrefix("v").trim(),
                apkUrl = url,
                size = apk.optLong("size"),
                sha256 = if (apk.isNull("digest")) null else apk.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
                // O que vem depois de uma linha "---" é para quem baixa pelo site (como instalar): o app não mostra
                notes = if (j.isNull("body")) "" else j.optString("body").replace("\r\n", "\n").substringBefore("\n---").trim(),
            )
        } finally {
            c.disconnect()
        }
    }

    // ---------- Baixar ----------

    /** Baixa o APK. Sozinho, só no Wi-Fi; pedido pela pessoa, por qualquer rede. */
    fun download(allowMetered: Boolean) {
        val rel = _state.value.release ?: return
        val dm = downloads()
        prefs().getLong("downloadId", -1).takeIf { it >= 0 }?.let { dm.remove(it) }
        apkDir().deleteRecursively()
        val request = SystemDownloads.Request(Uri.parse(rel.apkUrl))
            .setTitle("Subaru DL ${rel.version}")
            .setDescription("Versão nova do app")
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalFilesDir(app, DIR, "SubaruDL-${rel.version}.apk")
            .setAllowedOverMetered(allowMetered)
            .setAllowedOverRoaming(allowMetered)
            // Sem a notificação do Android (ela chega a ficar parada em 95%): o andamento aparece no app e o aviso vem quando estiver pronta
            .setNotificationVisibility(SystemDownloads.Request.VISIBILITY_HIDDEN)
        val id = runCatching { dm.enqueue(request) }.getOrElse { e ->
            _state.value = UpdateState(UpdatePhase.FAILED, rel, message = "O Android não deixou baixar a versão nova (${e.message}).")
            return
        }
        prefs().edit().putLong("downloadId", id).remove("ready").apply()
        _state.value = UpdateState(UpdatePhase.DOWNLOADING, rel, total = rel.size)
        watch()
    }

    fun cancelDownload() {
        prefs().getLong("downloadId", -1).takeIf { it >= 0 }?.let { downloads().remove(it) }
        prefs().edit().remove("downloadId").apply()
        _state.update { it.copy(phase = UpdatePhase.AVAILABLE, done = 0, waitingWifi = false) }
    }

    private class Row(val status: Int, val reason: Int, val done: Long, val total: Long)

    private fun query(id: Long): Row? = runCatching {
        downloads().query(SystemDownloads.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) null
            else Row(
                c.getInt(c.getColumnIndexOrThrow(SystemDownloads.COLUMN_STATUS)),
                c.getInt(c.getColumnIndexOrThrow(SystemDownloads.COLUMN_REASON)),
                c.getLong(c.getColumnIndexOrThrow(SystemDownloads.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                c.getLong(c.getColumnIndexOrThrow(SystemDownloads.COLUMN_TOTAL_SIZE_BYTES)),
            )
        }
    }.getOrNull()

    /** Acompanha o download enquanto o app está vivo; com o app fechado, quem avisa é o [UpdateDownloadReceiver]. */
    private fun watch() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            while (true) {
                val id = prefs().getLong("downloadId", -1)
                if (id < 0 || _state.value.phase != UpdatePhase.DOWNLOADING) break
                val row = query(id)
                when (row?.status) {
                    // Cancelado pela notificação do Android
                    null -> {
                        prefs().edit().remove("downloadId").apply()
                        _state.update { it.copy(phase = UpdatePhase.AVAILABLE, done = 0, waitingWifi = false) }
                        break
                    }
                    SystemDownloads.STATUS_SUCCESSFUL -> {
                        downloaded()
                        break
                    }
                    SystemDownloads.STATUS_FAILED -> {
                        downloads().remove(id)
                        prefs().edit().remove("downloadId").apply()
                        AppLog.w("Download da versão nova do app falhou (motivo ${row.reason})")
                        _state.update { it.copy(phase = UpdatePhase.FAILED, waitingWifi = false, message = "O download parou no meio (erro ${row.reason}).") }
                        break
                    }
                    else -> _state.update {
                        it.copy(
                            done = row.done, total = if (row.total > 0) row.total else it.total,
                            waitingWifi = row.status == SystemDownloads.STATUS_PAUSED && row.reason == SystemDownloads.PAUSED_QUEUED_FOR_WIFI,
                        )
                    }
                }
                delay(700)
            }
        }
    }

    /** O APK chegou: confere antes de oferecer a instalação. */
    suspend fun downloaded() = finishing.withLock {
        val p = prefs()
        val id = p.getLong("downloadId", -1)
        val rel = _state.value.release ?: return@withLock
        if (id < 0 || p.getString("ready", null) == rel.version || query(id)?.status != SystemDownloads.STATUS_SUCCESSFUL) return@withLock
        val problem = verify(apkFile(rel), rel)
        if (problem != null) {
            downloads().remove(id)
            p.edit().remove("downloadId").apply()
            AppLog.w("Versão nova do app recusada: $problem")
            _state.value = UpdateState(UpdatePhase.FAILED, rel, message = problem)
            return@withLock
        }
        p.edit().putString("ready", rel.version).apply()
        _state.value = UpdateState(UpdatePhase.READY, rel)
        AppLog.i("Versão nova do app baixada: ${rel.version}")
        if (!foreground) notifyReady(rel)
    }

    /** Devolve o motivo para recusar o arquivo, ou null se ele pode ser instalado. */
    private fun verify(file: File, rel: AppRelease): String? {
        if (!file.exists()) return "O arquivo baixado sumiu."
        if (rel.size > 0 && file.length() != rel.size) return "O arquivo chegou incompleto."
        rel.sha256?.let { want -> if (!sha256(file).equals(want, ignoreCase = true)) return "O arquivo chegou diferente do publicado." }
        val pm = app.packageManager
        val info = pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES) ?: return "O arquivo baixado não é um APK válido."
        if (info.packageName != app.packageName) return "O APK baixado é de outro app."
        if (info.longVersionCode <= BuildConfig.VERSION_CODE) return "O APK baixado não é mais novo que este."
        // Instalar por cima só funciona com a mesma chave: melhor dizer agora do que o Android recusar depois
        val theirs = signers(info)
        val mine = signers(pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES))
        if (theirs.isNotEmpty() && mine.isNotEmpty() && theirs != mine) return "O APK baixado foi assinado por outra chave e não instala por cima."
        return null
    }

    private fun signers(p: PackageInfo): Set<String> = p.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    // ---------- Instalar ----------

    fun canInstall(): Boolean = app.packageManager.canRequestPackageInstalls()

    /** Abre a tela do Android "Instalar apps desconhecidos" do Subaru DL; ao voltar, a instalação continua. */
    fun askPermission(activity: Activity) {
        awaitingPermission = true
        runCatching { activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))) }
            .onFailure { awaitingPermission = false }
    }

    /** Voltou para o app: se foi dar a permissão de instalar, segue a instalação. */
    fun onResume(activity: Activity) {
        if (!awaitingPermission) return
        awaitingPermission = false
        if (canInstall()) install(activity)
        else _state.update { it.copy(message = "Sem a permissão de instalar apps, o Android não deixa o Subaru DL se atualizar.") }
    }

    fun install(activity: Activity) {
        val rel = _state.value.release ?: return
        if (_state.value.phase != UpdatePhase.READY) return
        if (!canInstall()) return askPermission(activity)
        val file = readyFile() ?: run {
            forget()
            _state.value = UpdateState(UpdatePhase.AVAILABLE, rel, message = "O arquivo baixado sumiu; baixe de novo.")
            prefs().edit().putString("release", AppJson.encodeToString(AppRelease.serializer(), rel)).apply()
            return
        }
        app.getSystemService(NotificationManager::class.java).cancel(NOTIFY_ID)
        _state.update { it.copy(phase = UpdatePhase.INSTALLING, message = null) }
        scope.launch {
            runCatching {
                val installer = app.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(app.packageName)
                    setSize(file.length())
                    // O app atualizando a si mesmo (com UPDATE_PACKAGES_WITHOUT_USER_ACTION): o Android 12+ não pergunta
                    if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                val sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    file.inputStream().use { input ->
                        session.openWrite("base.apk", 0, file.length()).use { out ->
                            input.copyTo(out, 1 shl 16)
                            session.fsync(out)
                        }
                    }
                    prefs().edit().putString("installing", rel.version).commit()
                    val status = PendingIntent.getBroadcast(
                        app, sessionId, Intent(app, UpdateReceiver::class.java).setAction(ACTION_INSTALL_STATUS),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                    session.commit(status.intentSender)
                }
            }.onFailure { e ->
                AppLog.e("Instalar a versão nova do app: ${e.message}")
                prefs().edit().remove("installing").apply()
                _state.update { it.copy(phase = UpdatePhase.READY, message = "Não consegui começar a instalação (${e.message}).") }
            }
        }
    }

    /** Resposta do instalador do Android. */
    fun onInstallStatus(ctx: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                val shown = confirm != null && runCatching { ctx.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
                if (!shown) failInstall("O Android não mostrou a confirmação; toque em Instalar de novo.")
            }
            // O próprio Android encerra o app ao trocar a versão; quem avisa depois é o UpdateReceiver (MY_PACKAGE_REPLACED)
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                AppLog.w("Instalar a versão nova do app: status $status ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()}")
                failInstall(
                    when (status) {
                        PackageInstaller.STATUS_FAILURE_ABORTED -> "Instalação cancelada."
                        PackageInstaller.STATUS_FAILURE_STORAGE -> "Falta espaço no celular para instalar."
                        PackageInstaller.STATUS_FAILURE_BLOCKED -> "O Android bloqueou a instalação."
                        PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "O Android recusou a versão nova."
                        else -> "A instalação falhou."
                    },
                )
            }
        }
    }

    private fun failInstall(message: String) {
        prefs().edit().remove("installing").apply()
        _state.update { it.copy(phase = UpdatePhase.READY, message = message) }
    }

    /** O app acabou de ser trocado por uma versão nova (roda já na versão nova). */
    fun onPackageReplaced(ctx: Context) {
        val p = prefs()
        val installed = p.getString("installing", null) ?: return
        p.edit().remove("installing").apply()
        if (installed != BuildConfig.VERSION_NAME) return
        _justUpdated.value = installed
        // O Android costuma abrir o app de novo sozinho; se não abrir, a notificação avisa
        if (!foreground) notify(ctx, "Subaru DL atualizado para a versão $installed", "Toque para abrir")
    }

    fun consumeJustUpdated() {
        _justUpdated.value = null
        runCatching { app.getSystemService(NotificationManager::class.java).cancel(NOTIFY_ID) }
    }

    // ---------- Arquivos, rede e avisos ----------

    private fun apkDir() = File(app.getExternalFilesDir(null), DIR)
    private fun apkFile(rel: AppRelease) = File(apkDir(), "SubaruDL-${rel.version}.apk")
    private fun readyFile(): File? = _state.value.release?.let(::apkFile)?.takeIf { it.exists() }
        ?: prefs().getString("release", null)?.let { runCatching { AppJson.decodeFromString(AppRelease.serializer(), it) }.getOrNull() }?.let(::apkFile)?.takeIf { it.exists() }

    /** Esquece a versão guardada e apaga o APK baixado. */
    private fun forget() {
        val p = prefs()
        p.getLong("downloadId", -1).takeIf { it >= 0 }?.let { id -> runCatching { downloads().remove(id) } }
        p.edit().remove("release").remove("downloadId").remove("ready").apply()
        runCatching { apkDir().deleteRecursively() }
    }

    private class Net(val online: Boolean, val unmetered: Boolean)

    private fun network(): Net {
        val n = runCatching { (app as SubaruApp).manager.net.value }.getOrNull() ?: return Net(online = true, unmetered = false)
        return Net(n.online, n.online && n.wifi)
    }

    private fun notifyReady(rel: AppRelease) = notify(app, "Versão ${rel.version} do Subaru DL pronta", "Toque para instalar")

    private fun notify(ctx: Context, title: String, text: String) {
        if (!Notifier.canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, Notifier.CH_UPDATE)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(Notifier.openApp(ctx, "update"))
            .setAutoCancel(true)
            .build()
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(NOTIFY_ID, n) }
    }

    private fun parts(v: String) = v.removePrefix("v").split('.', '-').map { p -> p.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    /** A versão é mais nova que a instalada? (1.10.0 > 1.9.0) */
    fun isNewer(version: String): Boolean {
        val a = parts(version)
        val b = parts(BuildConfig.VERSION_NAME)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/** Resposta do instalador e o aviso de "atualizado" depois que a versão nova entra. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppUpdater.ACTION_INSTALL_STATUS -> AppUpdater.onInstallStatus(context, intent)
            Intent.ACTION_MY_PACKAGE_REPLACED -> AppUpdater.onPackageReplaced(context)
        }
    }
}

/** O gerenciador de downloads do Android avisa quando o APK termina, mesmo com o app fechado. */
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SystemDownloads.ACTION_DOWNLOAD_COMPLETE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppUpdater.downloaded()
            } finally {
                pending.finish()
            }
        }
    }
}
