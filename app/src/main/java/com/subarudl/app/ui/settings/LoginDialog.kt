package com.subarudl.app.ui.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subarudl.app.data.Cookies
import com.subarudl.app.ui.components.Gutter

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

/** Janela cheia com um navegador dentro do app para entrar numa conta; no fim, os cookies vão para o yt-dlp e o gallery-dl. */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Entrar em um site") },
                    navigationIcon = { IconButton({ onDone(0) }) { Icon(Icons.Outlined.Close, "Cancelar") } },
                    actions = {
                        TextButton({
                            CookieManager.getInstance().flush()
                            onDone(Cookies.saveFromWebView(ctx, current))
                        }) { Text("Concluir") }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                OutlinedTextField(
                    address, { address = it }, Modifier.fillMaxWidth().padding(horizontal = Gutter), singleLine = true,
                    label = { Text("Endereço do site") },
                    supportingText = { Text("Entre na sua conta e toque em Concluir. Os cookies ficam só neste celular.") },
                    trailingIcon = { IconButton({ go(address) }) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Abrir o endereço") } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go(address) }),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sites.forEach { (name, url) -> SuggestionChip(onClick = { go(url) }, label = { Text(name) }) }
                }
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
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                    onRelease = { it.destroy() },
                )
            }
        }
    }
}
