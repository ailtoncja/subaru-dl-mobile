package com.subarudl.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.data.CookieReceiver
import com.subarudl.app.data.Cookies
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.theme.tabular
import kotlinx.coroutines.delay

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
    val scheme = MaterialTheme.colorScheme

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        CookieReceiver.start(ctx)
        onDispose {
            view.keepScreenOn = false
            CookieReceiver.stop()
        }
    }
    LaunchedEffect(state) { (state as? CookieReceiver.State.Done)?.let { onDone(it.imported) } }
    // Segundos até o código expirar, para a contagem na tela
    var remaining by remember { mutableLongStateOf(CookieReceiver.LIFETIME_MS / 1000) }
    LaunchedEffect(state) {
        val w = state as? CookieReceiver.State.Waiting ?: return@LaunchedEffect
        while (true) {
            remaining = ((w.expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Gutter).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Receber cookies do PC", style = MaterialTheme.typography.titleLarge)
            when (val s = state) {
                is CookieReceiver.State.Waiting -> {
                    Text(
                        "No computador, abra o Subaru DL, vá em Ajustes, Rede e login, \"Enviar para o celular\", toque em Procurar celular e digite o código abaixo. Os dois precisam estar no mesmo Wi-Fi.",
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh)) {
                        Text("Código", Modifier.padding(start = 16.dp, top = 12.dp), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        // 16 letras em quatro blocos (XXXX-XXXX-XXXX-XXXX): letra fixa para os blocos ficarem alinhados
                        Text(
                            CookieReceiver.pretty(s.code), Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 4.dp, top = 4.dp),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                            color = scheme.primary, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                        )
                        Text(
                            "Vale por mais ${remaining / 60}:${"%02d".format(remaining % 60)}", Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            style = MaterialTheme.typography.bodySmall.tabular, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                    if (s.addresses.isEmpty()) {
                        Text("Este celular não está num Wi-Fi. Conecte-o à mesma rede do computador.", style = MaterialTheme.typography.bodyMedium, color = scheme.error)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Se o PC não achar o celular, digite este endereço lá:", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                            s.addresses.forEach { a -> Text(if (s.port == CookieReceiver.PORT) a else "$a:${s.port}", style = MaterialTheme.typography.bodyLarge.tabular) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Esperando o PC…", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }
                is CookieReceiver.State.Failed -> {
                    Text(s.message, style = MaterialTheme.typography.bodyMedium, color = scheme.error)
                    Button({ CookieReceiver.start(ctx) }, Modifier.fillMaxWidth()) { Text("Gerar outro código") }
                }
                CookieReceiver.State.Expired -> {
                    Text(
                        "O código expirou: o recebimento fica aberto por 5 minutos e foi fechado. Gere outro código quando o PC estiver pronto.",
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                    )
                    Button({ CookieReceiver.start(ctx) }, Modifier.fillMaxWidth()) { Text("Gerar outro código") }
                }
                else -> Unit
            }
            TextButton(onDismiss, Modifier.align(Alignment.End)) { Text("Fechar") }
        }
    }
}
