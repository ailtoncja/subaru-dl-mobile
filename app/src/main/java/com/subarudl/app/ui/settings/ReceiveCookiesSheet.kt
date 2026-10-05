package com.subarudl.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.data.CookieReceiver
import com.subarudl.app.data.Cookies
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.SCard
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T

/**
 * Receber o cookies.txt do Subaru DL do PC pelo Wi-Fi: o celular escuta enquanto esta folha está aberta e mostra o código
 * que o PC precisa digitar. Ao chegar, os cookies entram sozinhos e a folha fecha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveCookiesSheet(onDone: (Cookies.Imported) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val state by CookieReceiver.state.collectAsState()

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        CookieReceiver.start(ctx)
        onDispose {
            view.keepScreenOn = false
            CookieReceiver.stop()
        }
    }
    LaunchedEffect(state) { (state as? CookieReceiver.State.Done)?.let { onDone(it.imported) } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = C.Card, contentColor = C.Text) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Receber cookies do PC", style = T.headlineSm)
            when (val s = state) {
                is CookieReceiver.State.Waiting -> {
                    Text(
                        "No computador, abra o Subaru DL → Configurações → Rede e login → \"Enviar para o celular\", toque em Procurar celular e digite o código abaixo. Os dois precisam estar no mesmo Wi-Fi.",
                        style = T.bodySm.copy(fontSize = 14.sp), color = C.Muted,
                    )
                    SCard {
                        SectionLabel("Código")
                        Text(
                            CookieReceiver.pretty(s.code), Modifier.fillMaxWidth().padding(top = 6.dp),
                            style = T.number.copy(fontSize = 34.sp, letterSpacing = 3.sp), color = C.Gold, textAlign = TextAlign.Center,
                        )
                    }
                    if (s.addresses.isEmpty()) {
                        Text("Este celular não está num Wi-Fi. Conecte-o à mesma rede do computador.", style = T.bodySm.copy(fontSize = 14.sp), color = C.Error)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Se o PC não achar o celular, digite este endereço lá:", style = T.bodySm, color = C.Muted)
                            s.addresses.forEach { a -> Mono(if (s.port == CookieReceiver.PORT) a else "$a:${s.port}", color = C.Text, style = T.labelLg) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = C.BluePale, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Esperando o PC…", style = T.labelLg, color = C.Muted)
                    }
                }
                is CookieReceiver.State.Failed -> {
                    Text(s.message, style = T.bodyMd, color = C.Error)
                    BtnPrimary("Gerar outro código", { CookieReceiver.start(ctx) }, Modifier.fillMaxWidth())
                }
                else -> Unit
            }
            BtnGhost("Fechar", onDismiss, Modifier.fillMaxWidth())
        }
    }
}
