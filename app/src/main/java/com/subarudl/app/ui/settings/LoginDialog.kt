package com.subarudl.app.ui.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subarudl.app.data.Cookies
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.ChipSmall
import com.subarudl.app.ui.components.HRow
import com.subarudl.app.ui.home.fieldColors
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T

private val sites = listOf(
    "YouTube" to "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2F",
    "Instagram" to "https://www.instagram.com/accounts/login/",
    "X" to "https://x.com/i/flow/login",
    "TikTok" to "https://www.tiktok.com/login",
    "Twitch" to "https://www.twitch.tv/login",
    "Reddit" to "https://www.reddit.com/login",
    "Pixiv" to "https://accounts.pixiv.net/login",
    "Facebook" to "https://www.facebook.com/login",
)

/** Janela com um navegador dentro do app para entrar numa conta; no fim, os cookies vão para o yt-dlp e o gallery-dl. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginDialog(onDone: (Int) -> Unit) {
    val ctx = LocalContext.current
    var address by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("https://www.youtube.com/") }
    var web by remember { mutableStateOf<WebView?>(null) }

    fun go(raw: String) {
        val u = raw.trim().let { if (it.isEmpty()) return else if (Regex("^[a-z]+://").containsMatchIn(it)) it else "https://$it" }
        web?.loadUrl(u)
    }

    Dialog(onDismissRequest = { onDone(0) }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(C.Surface).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    address, { address = it }, Modifier.weight(1f), singleLine = true, label = { Text("Endereço do site") }, colors = fieldColors(), textStyle = T.labelLg,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { go(address) }),
                )
                Spacer(Modifier.width(8.dp))
                BtnGhost("Ir", { go(address) }, height = 52.dp)
            }
            Spacer(Modifier.height(8.dp))
            HRow(gap = 6.dp) { sites.forEach { (name, url) -> ChipSmall(name, false, { go(url) }) } }
            Spacer(Modifier.height(8.dp))
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportMultipleWindows(false)
                        // Sem o "; wv" o Google aceita o login numa janela dentro de app
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { url?.let { current = it; address = it } }
                            override fun onPageFinished(view: WebView?, url: String?) { url?.let { current = it } }
                        }
                        loadUrl(sites.first().second)
                        web = this
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onRelease = { it.destroy() },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                BtnGhost("Cancelar", { onDone(0) }, Modifier.weight(1f))
                BtnPrimary("Concluir e guardar", {
                    CookieManager.getInstance().flush()
                    onDone(Cookies.saveFromWebView(ctx, current))
                }, Modifier.weight(1.4f), height = 46.dp)
            }
        }
    }
}

@Composable
private fun Text(text: String) = androidx.compose.material3.Text(text)
