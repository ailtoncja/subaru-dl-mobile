package com.subarudl.app

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.subarudl.app.data.Cookies
import com.subarudl.app.data.SettingsStore
import com.subarudl.app.service.Notifier
import com.subarudl.app.ui.AppRoot
import com.subarudl.app.ui.theme.SubaruTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** O link copiado é lido uma vez quando o app volta para a frente, não a cada folha que fecha. */
    private var checkClipboard = false

    /** Tela recriada só porque girou ou mudou de tamanho: não é volta para a frente, não lê o link copiado. */
    private var recreated = false

    /** Hora do último link que chegou por Compartilhar ou abrir: logo depois dele, o link copiado não passa na frente. */
    private var lastIncomingAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(this)) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { SubaruTheme { AppRoot(vm) } }
        recreated = savedInstanceState != null
        if (savedInstanceState == null) handle(intent)
    }

    override fun onStart() {
        super.onStart()
        checkClipboard = !recreated
        recreated = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(Notifier.EXTRA_TAB)?.let { t ->
            vm.screen = Screen.HOME
            vm.settingsPage = null
            vm.focus = when (t) {
                "queue-done" -> Section.DONE
                "queue-fail" -> Section.FAILED
                else -> Section.ACTIVE
            }
        }
        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW) lastIncomingAt = SystemClock.elapsedRealtime()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val file = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (file != null) importCookies(file) else intent.getStringExtra(Intent.EXTRA_TEXT)?.let { vm.handleIncoming(it) }
            }
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null && (data.scheme == "content" || data.scheme == "file")) importCookies(data)
                else intent.dataString?.let { vm.handleIncoming(it) }
            }
        }
    }

    /** Um arquivo aberto ou compartilhado com o app: só serve se for um cookies.txt. */
    private fun importCookies(uri: Uri) {
        val r = Cookies.importFile(this, uri)
        vm.toast(if (r.cookies > 0) "${r.cookies} cookies de ${r.sites} sites importados" else "Esse arquivo não parece um cookies.txt")
    }

    /** Link copiado: só dá para ler a área de transferência com o app em primeiro plano. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !checkClipboard) return
        checkClipboard = false
        if (!SettingsStore.value.clipboardCapture || SystemClock.elapsedRealtime() - lastIncomingAt < 5_000) return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.contains("http")) vm.handleIncoming(text, fromClipboard = true)
    }
}
