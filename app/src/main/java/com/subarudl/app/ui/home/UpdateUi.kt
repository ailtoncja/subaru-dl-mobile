package com.subarudl.app.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.subarudl.app.BuildConfig
import com.subarudl.app.MainViewModel
import com.subarudl.app.service.AppRelease
import com.subarudl.app.service.AppUpdater
import com.subarudl.app.service.UpdatePhase
import com.subarudl.app.service.UpdateState
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.util.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Aviso da versão nova no alto da tela principal: baixar, acompanhar e instalar. */
@Composable
internal fun UpdateNotice(vm: MainViewModel) {
    val u by vm.update.collectAsState()
    val rel = u.release ?: return
    var notes by rememberSaveable { mutableStateOf(false) }
    val install = rememberInstallUpdate(vm)
    val download = { AppUpdater.download(allowMetered = true) }
    if (notes) {
        val action = when (u.phase) {
            UpdatePhase.AVAILABLE, UpdatePhase.FAILED -> "Baixar (${formatBytes(rel.size)})" to download
            UpdatePhase.READY -> "Instalar" to install
            else -> null
        }
        ReleaseNotesSheet(rel, action) { notes = false }
    }
    if (vm.updateHidden == rel.version && u.phase != UpdatePhase.INSTALLING) return

    val later = "Depois" to { vm.updateHidden = rel.version }
    val news = if (rel.notes.isNotBlank()) listOf("Novidades" to { notes = true }) else emptyList()
    when (u.phase) {
        UpdatePhase.AVAILABLE -> Notice(
            Icons.Outlined.SystemUpdate, "Saiu a versão ${rel.version}",
            u.message ?: "${formatBytes(rel.size)}. Instala por cima, sem perder a lista nem os ajustes.",
            actions = listOf(later) + news + ("Baixar" to download),
        )
        UpdatePhase.DOWNLOADING -> Notice(
            Icons.Outlined.SystemUpdate, "Baixando a versão ${rel.version}", downloadLine(u),
            progress = if (u.total > 0) (u.done.toFloat() / u.total).coerceIn(0f, 1f) else -1f,
            actions = buildList {
                if (u.waitingWifi) add("Usar os dados móveis" to download)
                add("Cancelar" to { AppUpdater.cancelDownload() })
            },
        )
        UpdatePhase.READY -> Notice(
            Icons.Outlined.SystemUpdate, "Versão ${rel.version} pronta para instalar",
            u.message ?: "O app reinicia para instalar; a lista e os ajustes continuam.",
            actions = listOf(later) + news + ("Instalar" to install),
        )
        UpdatePhase.INSTALLING -> Notice(
            Icons.Outlined.SystemUpdate, "Instalando a versão ${rel.version}…",
            "Se o Android pedir, confirme. O app reinicia quando a versão nova entrar.", progress = -1f,
        )
        UpdatePhase.FAILED -> Notice(
            Icons.Outlined.ErrorOutline, "A versão ${rel.version} não pôde ser baixada", u.message, error = true,
            actions = listOf(later, "Tentar de novo" to download),
        )
        else -> Unit
    }
}

private fun downloadLine(u: UpdateState): String {
    val pct = if (u.total > 0) "${(u.done * 100 / u.total).toInt()}%" else null
    return if (u.waitingWifi) listOfNotNull("Esperando o Wi-Fi para continuar", pct).joinToString(" · ")
    else listOfNotNull(pct, if (u.total > 0) "${formatBytes(u.done)} de ${formatBytes(u.total)}" else null).joinToString(" · ").ifEmpty { "Começando…" }
}

/** A linha "Subaru DL x.y.z" dos Ajustes: em que pé está a versão nova e o botão do próximo passo. */
@Composable
internal fun AppVersionItem(vm: MainViewModel) {
    val u by vm.update.collectAsState()
    val s by vm.settings.collectAsState()
    val install = rememberInstallUpdate(vm)
    val v = u.release?.version
    val status = when (u.phase) {
        UpdatePhase.CHECKING -> "Procurando versão nova…"
        UpdatePhase.AVAILABLE -> u.message ?: "Saiu a versão $v (${formatBytes(u.release?.size)})"
        UpdatePhase.DOWNLOADING -> "Baixando a versão $v · ${downloadLine(u)}"
        UpdatePhase.READY -> u.message ?: "Versão $v pronta para instalar"
        UpdatePhase.INSTALLING -> "Instalando a versão $v…"
        UpdatePhase.FAILED -> u.message ?: "A versão $v não pôde ser baixada"
        UpdatePhase.IDLE -> u.message ?: if (s.lastAppCheck > 0) "Versão mais nova · conferida em ${checkedAt(s.lastAppCheck)}" else "As versões novas vêm do GitHub"
    }
    ListItem(
        headlineContent = { Text("Subaru DL ${BuildConfig.VERSION_NAME}") },
        supportingContent = { Text(status) },
        leadingContent = { Icon(Icons.Outlined.Info, null) },
        trailingContent = {
            when (u.phase) {
                UpdatePhase.CHECKING, UpdatePhase.INSTALLING -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                UpdatePhase.DOWNLOADING -> TextButton({ AppUpdater.cancelDownload() }) { Text("Cancelar") }
                UpdatePhase.READY -> TextButton(install) { Text("Instalar") }
                UpdatePhase.AVAILABLE, UpdatePhase.FAILED -> TextButton({ AppUpdater.download(allowMetered = true) }) { Text("Baixar") }
                UpdatePhase.IDLE -> TextButton({ AppUpdater.check(manual = true) }) { Text("Verificar") }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private fun checkedAt(ms: Long): String = SimpleDateFormat("d 'de' MMMM 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(ms))

/**
 * Tocar em Instalar: avisa que os downloads em andamento param, explica a permissão de instalar apps (o Android só pede
 * na primeira vez) e instala.
 */
@Composable
internal fun rememberInstallUpdate(vm: MainViewModel): () -> Unit {
    val activity = LocalActivity.current
    val items by vm.manager.items.collectAsState()
    val running = items.count { it.active }
    var step by remember { mutableStateOf<String?>(null) }

    fun afterDownloads() {
        step = null
        val a = activity ?: return
        if (AppUpdater.canInstall()) AppUpdater.install(a) else step = "permission"
    }

    when (step) {
        "downloads" -> AlertDialog(
            onDismissRequest = { step = null },
            icon = { Icon(Icons.Outlined.SystemUpdate, null) },
            title = { Text("Instalar agora?") },
            text = {
                Text(
                    if (running == 1) "Um download está em andamento. Ele para agora e continua sozinho quando o app voltar."
                    else "$running downloads estão em andamento. Eles param agora e continuam sozinhos quando o app voltar.",
                )
            },
            confirmButton = { TextButton({ afterDownloads() }) { Text("Instalar") } },
            dismissButton = { TextButton({ step = null }) { Text("Agora não") } },
        )
        "permission" -> AlertDialog(
            onDismissRequest = { step = null },
            icon = { Icon(Icons.Outlined.SystemUpdate, null) },
            title = { Text("Deixar o app se atualizar") },
            text = { Text("O Android pede uma permissão, só na primeira vez. Na próxima tela, ligue \"Permitir desta fonte\" e volte: a instalação continua sozinha.") },
            confirmButton = { TextButton({ step = null; activity?.let { AppUpdater.askPermission(it) } }) { Text("Abrir a permissão") } },
            dismissButton = { TextButton({ step = null }) { Text("Agora não") } },
        )
    }
    return { if (running > 0) step = "downloads" else afterDownloads() }
}

/** As notas da Release (o texto do GitHub), com o botão do próximo passo embaixo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseNotesSheet(rel: AppRelease, action: Pair<String, () -> Unit>?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Gutter)) {
                Text("Novidades da versão ${rel.version}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                ReleaseNotes(rel.notes)
                Spacer(Modifier.height(16.dp))
            }
            if (action != null) {
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp)) {
                    Button({ onDismiss(); action.second() }, Modifier.fillMaxWidth()) { Text(action.first) }
                }
            } else Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Markdown simples do GitHub: parágrafos, itens com "-", títulos com "#" e **negrito**. */
@Composable
private fun ReleaseNotes(markdown: String) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        markdown.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val bullet = line.startsWith("- ") || line.startsWith("* ")
            val heading = line.startsWith("#")
            val text = inline(line.removePrefix("- ").removePrefix("* ").trimStart('#', ' '))
            if (bullet) Row {
                Text("•", Modifier.width(20.dp), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
            else Text(text, style = if (heading) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        }
    }
}

private val markdownLink = Regex("\\[([^\\]]+)]\\([^)]*\\)")

private fun inline(s: String) = buildAnnotatedString {
    s.replace("`", "").replace(markdownLink, "$1").split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
    }
}
