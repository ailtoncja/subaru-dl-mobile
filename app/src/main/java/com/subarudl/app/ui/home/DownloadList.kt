package com.subarudl.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subarudl.app.MainViewModel
import com.subarudl.app.data.Kind
import com.subarudl.app.data.Mode
import com.subarudl.app.data.OutputFile
import com.subarudl.app.data.QueueItem
import com.subarudl.app.data.Status
import com.subarudl.app.data.Storage
import com.subarudl.app.ui.components.GalleryMosaic
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.components.InfoItem
import com.subarudl.app.ui.components.LocalVideoThumb
import com.subarudl.app.ui.components.SectionTitle
import com.subarudl.app.ui.components.Thumb
import com.subarudl.app.ui.theme.tabular
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.formatRemaining
import com.subarudl.app.util.formatSpeed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A lista dividida pela situação de cada download, já filtrada pela busca. */
class Groups(items: List<QueueItem>, query: String) {
    private val shown = if (query.isBlank()) items else items.filter { matches(it, query.trim()) }
    val running = shown.filter { it.active }
    val waiting = shown.filter { it.status == Status.WAITING || it.status == Status.PAUSED }
    val failed = shown.filter { it.status == Status.ERROR }.sortedByDescending { it.finishedAt }
    val done = shown.filter { it.status == Status.DONE }.sortedByDescending { it.finishedAt }
    val isEmpty get() = running.isEmpty() && waiting.isEmpty() && failed.isEmpty() && done.isEmpty()
}

/** A busca acha pelo título, canal, site, link, formato (mp3, mp4) e qualidade (1080p, 4k). */
private fun matches(it: QueueItem, q: String): Boolean {
    val format = if (it.isAudio) it.options.audioFormat else it.ext.ifEmpty { it.options.container }
    val fourK = (it.quality.removeSuffix("p").toIntOrNull() ?: 0) >= 2160
    return listOfNotNull(it.title, it.channel, it.site, it.url, format, it.quality, if (fourK) "4k" else null).any { s -> s.contains(q, ignoreCase = true) }
}

/** Monta as partes da lista dentro da coluna da tela principal. */
fun LazyListScope.downloadSections(vm: MainViewModel, g: Groups, actions: Boolean, onDetails: (String) -> Unit, onOpen: (QueueItem) -> Unit) {
    if (g.running.isNotEmpty()) {
        item(key = "h-running") {
            val speed = g.running.sumOf { it.speed }
            SectionTitle(if (speed > 0) "Baixando · ${formatSpeed(speed)}" else "Baixando", Modifier.animateItem()) {
                if (actions) TextButton({ vm.manager.pauseAll() }) { Text("Pausar tudo") }
            }
        }
        // Com um download só, a velocidade fica no título da seção e o cartão mostra só % e tempo (cabe com letra grande)
        items(g.running, key = { it.id }) { DownloadCard(vm, it, onDetails, onOpen, Modifier.animateItem(), speedOnCard = g.running.size > 1) }
    }
    if (g.waiting.isNotEmpty()) {
        item(key = "h-waiting") {
            SectionTitle("Na fila", Modifier.animateItem()) {
                if (!actions) Unit
                else if (g.waiting.any { it.status == Status.PAUSED }) TextButton({ vm.manager.resumeAll() }) { Text("Retomar tudo") }
                else if (g.running.isEmpty()) TextButton({ vm.manager.pauseAll() }) { Text("Pausar tudo") }
            }
        }
        items(g.waiting, key = { it.id }) { DownloadCard(vm, it, onDetails, onOpen, Modifier.animateItem()) }
    }
    if (g.failed.isNotEmpty()) {
        item(key = "h-failed") {
            SectionTitle("Falhas", Modifier.animateItem()) {
                if (actions) TextButton({ vm.clearSection(setOf(Status.ERROR), "Falhas tiradas da lista") }) { Text("Limpar") }
            }
        }
        items(g.failed, key = { it.id }) { DownloadCard(vm, it, onDetails, onOpen, Modifier.animateItem()) }
    }
    if (g.done.isNotEmpty()) {
        item(key = "h-done") {
            SectionTitle("Concluídos", Modifier.animateItem()) {
                if (actions) TextButton({ vm.clearSection(setOf(Status.DONE), "Concluídos tirados da lista; os arquivos continuam no celular") }) { Text("Limpar") }
            }
        }
        items(g.done, key = { it.id }) { DownloadCard(vm, it, onDetails, onOpen, Modifier.animateItem()) }
    }
}

/** Capa do item: miniatura do vídeo, mosaico das fotos ou o ícone do tipo. */
@Composable
fun Cover(item: QueueItem, modifier: Modifier = Modifier) {
    val icon = if (item.isAudio) Icons.Outlined.MusicNote else Icons.Outlined.Movie
    val video = item.outputs.firstOrNull { it.mime.startsWith("video/") }
    when {
        item.kind == Kind.GALLERY -> GalleryMosaic(item, modifier)
        item.thumb == null && video != null -> LocalVideoThumb(video.uri, modifier, icon = icon)
        else -> Thumb(item.thumb ?: item.outputs.firstOrNull { it.mime.startsWith("image/") }?.uri, modifier, icon = icon)
    }
}

/** Formato, qualidade e tamanho de um download pronto: "MP4 · 1080p · 284 MB". */
fun formatLine(it: QueueItem): String = if (it.kind == Kind.GALLERY) {
    listOfNotNull("${it.fileCount} arquivos", if (it.skipped > 0) "${it.skipped} já baixados" else null, if (it.failed > 0) "${it.failed} falharam" else null).joinToString(" · ")
} else listOfNotNull(
    (if (it.isAudio) it.options.audioFormat else it.ext.ifEmpty { it.options.container }).uppercase(),
    it.quality.takeIf { q -> q.isNotEmpty() },
    if (it.sizeBytes > 0) formatBytes(it.sizeBytes) else null,
    if (it.isPlaylist && it.fileCount > 1) "${it.fileCount} arquivos" else null,
).joinToString(" · ")

/** A linha de andamento de um download ativo, em palavras. */
private fun progressLine(it: QueueItem, withSpeed: Boolean = true): String {
    val pct = "${(it.progress * 100).toInt()}%"
    return when {
        it.kind == Kind.GALLERY -> "${it.fileCount} arquivos" + if (it.skipped > 0) " · ${it.skipped} já baixados" else ""
        it.status == Status.PROCESSING -> it.stage.ifEmpty { "Finalizando" }
        it.progress <= 0f && it.speed <= 0.0 -> it.stage.takeIf { s -> s.isNotEmpty() && s != "Iniciando" } ?: "Começando…"
        it.itemCount > 1 -> listOfNotNull("Item ${it.itemIndex.coerceAtLeast(1)} de ${it.itemCount}", pct, if (withSpeed) formatSpeed(it.speed) else null).joinToString(" · ")
        else -> listOfNotNull(pct, formatRemaining(it.eta), if (withSpeed) formatSpeed(it.speed) else null).filter { s -> s.isNotEmpty() }.joinToString(" · ")
    }
}

@Composable
private fun DownloadCard(vm: MainViewModel, it: QueueItem, onDetails: (String) -> Unit, onOpen: (QueueItem) -> Unit, modifier: Modifier = Modifier, speedOnCard: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = { if (it.status == Status.DONE) onOpen(it) else onDetails(it.id) },
        modifier = modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = if (it.status == Status.ERROR) Alignment.Top else Alignment.CenterVertically,
        ) {
            Cover(it, Modifier.width(104.dp).aspectRatio(16f / 9f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(it.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when (it.status) {
                    Status.DOWNLOADING, Status.PROCESSING -> {
                        Bar(it.progress, indeterminate = it.kind == Kind.GALLERY || it.status == Status.PROCESSING || it.progress <= 0f)
                        Meta(progressLine(it, withSpeed = speedOnCard))
                    }
                    Status.PAUSED -> {
                        if (it.progress > 0f) Bar(it.progress, dim = true)
                        Meta(if (it.progress > 0f) "Pausado em ${(it.progress * 100).toInt()}%" else "Pausado")
                    }
                    Status.WAITING -> Meta(it.stage.ifEmpty { "Na fila" })
                    Status.ERROR -> {
                        val f = failureOf(it.hint, it.error, it.url)
                        Text(f.cause, style = MaterialTheme.typography.bodySmall, color = scheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // O botão de texto começa alinhado com o título (tira o recuo interno dele)
                        TextButton({ runRemedy(vm, it, f.remedy) }, Modifier.offset(x = (-12).dp).tightVertical(8.dp)) { Text(f.remedy.label) }
                    }
                    Status.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = scheme.tertiary)
                        Spacer(Modifier.width(6.dp))
                        Meta(formatLine(it))
                    }
                }
            }
            when (it.status) {
                Status.DOWNLOADING, Status.PROCESSING -> IconButton({ vm.manager.pause(it.id) }) { Icon(Icons.Outlined.Pause, "Pausar") }
                Status.PAUSED -> IconButton({ vm.manager.resume(it.id) }) { Icon(Icons.Outlined.PlayArrow, "Retomar") }
                Status.WAITING -> IconButton({ vm.manager.cancel(it.id) }) { Icon(Icons.Outlined.Close, "Cancelar") }
                Status.ERROR, Status.DONE -> IconButton({ onDetails(it.id) }) { Icon(Icons.Outlined.MoreVert, "Mais opções") }
            }
        }
    }
}

/**
 * Tira [amount] de cima e de baixo do espaço que o elemento ocupa, sem diminuir a área de toque: o botão de texto
 * (48 dp para o dedo) fica colado na linha de cima em vez de abrir um vão no cartão.
 */
private fun Modifier.tightVertical(amount: Dp): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    val cut = amount.roundToPx()
    layout(p.width, (p.height - 2 * cut).coerceAtLeast(0)) { p.place(0, -cut) }
}

/** Faz o que o botão de uma falha promete. */
fun runRemedy(vm: MainViewModel, item: QueueItem, remedy: Remedy) {
    when (remedy) {
        Remedy.RETRY -> vm.manager.retry(item.id)
        Remedy.LOGIN -> vm.openSettings("contas")
        Remedy.AS_VIDEO -> vm.retryAsVideo(item)
        Remedy.AS_IMAGES -> vm.retryItemAsImages(item)
        Remedy.TURBO_OFF -> vm.retryWithoutTurbo(item)
    }
}

@Composable
private fun Meta(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall.tabular, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, indeterminate: Boolean = false, dim: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val color = if (dim) scheme.outline else scheme.primary
    val m = modifier.fillMaxWidth().padding(vertical = 2.dp).height(4.dp)
    if (indeterminate) LinearProgressIndicator(m, color = color, trackColor = scheme.surfaceContainerHighest, strokeCap = StrokeCap.Round)
    else LinearProgressIndicator({ progress.coerceIn(0f, 1f) }, m, color = color, trackColor = scheme.surfaceContainerHighest, strokeCap = StrokeCap.Round)
}

// ---------- Detalhes de um item ----------

fun openFile(ctx: Context, vm: MainViewModel, f: OutputFile) {
    if (!Storage.exists(ctx, f.uri)) {
        vm.toast("O arquivo não está mais lá: foi movido ou apagado")
        return
    }
    runCatching { ctx.startActivity(Storage.viewIntent(f)) }.onFailure { vm.toast("Nenhum app do celular abre esse arquivo") }
}

/** Folha com tudo sobre um download e as ações que cabem na situação dele. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSheet(vm: MainViewModel, id: String, onDismiss: () -> Unit) {
    val items by vm.manager.items.collectAsState()
    val it = items.firstOrNull { x -> x.id == id }
    if (it == null) {
        onDismiss()
        return
    }
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf(false) }
    val first = it.outputs.firstOrNull()
    val close = { act: () -> Unit -> act(); onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.Top) {
                Cover(it, Modifier.width(128.dp).aspectRatio(16f / 9f))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(it.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(it.channel, it.site.takeIf { s -> s.isNotEmpty() }).distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(Modifier.padding(horizontal = Gutter, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (it.status) {
                    Status.DOWNLOADING, Status.PROCESSING -> {
                        Bar(it.progress, indeterminate = it.kind == Kind.GALLERY || it.status == Status.PROCESSING || it.progress <= 0f)
                        Text(progressLine(it), style = MaterialTheme.typography.bodyMedium.tabular)
                        if (it.total > 0) Text("${formatBytes(it.downloaded)} de ${formatBytes(it.total)}", style = MaterialTheme.typography.bodySmall.tabular, color = scheme.onSurfaceVariant)
                        Actions {
                            FilledTonalButton({ vm.manager.pause(it.id) }) { Icon(Icons.Outlined.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Pausar") }
                            OutlinedButton({ close { vm.manager.cancel(it.id) } }) { Text("Cancelar download") }
                        }
                    }
                    Status.PAUSED, Status.WAITING -> {
                        Text(
                            if (it.status == Status.PAUSED) "Pausado" + if (it.progress > 0f) " em ${(it.progress * 100).toInt()}%" else ""
                            else it.stage.ifEmpty { "Na fila: começa quando houver vaga" },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Actions {
                            if (it.status == Status.PAUSED) Button({ vm.manager.resume(it.id) }) { Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Retomar") }
                            OutlinedButton({ close { vm.manager.cancel(it.id) } }) { Text("Cancelar download") }
                        }
                    }
                    Status.ERROR -> {
                        val f = failureOf(it.hint, it.error, it.url)
                        // Com o botão "Entrar na conta" ao lado, a frase que manda para os Ajustes sai
                        Text(
                            it.hint?.let { h -> if (f.remedy == Remedy.LOGIN) withoutLoginPath(h) else h } ?: "O download falhou",
                            style = MaterialTheme.typography.bodyMedium, color = scheme.error,
                        )
                        it.error?.let { e -> Text("Detalhe: $e", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis) }
                        Actions {
                            Button({ close { runRemedy(vm, it, f.remedy) } }) { Text(f.remedy.label) }
                            if (f.remedy != Remedy.RETRY) FilledTonalButton({ close { vm.manager.retry(it.id) } }) { Text("Tentar de novo") }
                        }
                    }
                    Status.DONE -> {
                        Text(
                            if (it.finishedAt > 0) "Concluído em " + SimpleDateFormat("d 'de' MMMM 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(it.finishedAt)) else "Concluído",
                            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                        )
                        if (it.error != null) Text(it.error, style = MaterialTheme.typography.bodyMedium, color = scheme.tertiary)
                        if (first != null) Actions {
                            Button({ openFile(ctx, vm, first) }) { Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Abrir") }
                            FilledTonalButton({ runCatching { ctx.startActivity(Storage.shareIntent(first)) } }) {
                                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Compartilhar")
                            }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = Gutter))
            val format = formatLine(it)
            if (it.status == Status.DONE && format.isNotEmpty()) InfoItem("Arquivo", format, Icons.Outlined.Download)
            if (it.status == Status.DONE || it.kind == Kind.VIDEO) {
                val dest = if (it.kind == Kind.GALLERY) Storage.galleryLabel(ctx, it.gallery?.destination ?: vm.settings.value.galleryDest)
                else Storage.destinationLabel(ctx, it.options.destination, it.options.mode == Mode.AUDIO)
                InfoItem("Pasta", dest, Icons.Outlined.FolderOpen)
            }
            Action(Icons.Outlined.ContentCopy, "Copiar o link") {
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("link", it.url))
                vm.toast("Link copiado")
            }
            Action(Icons.AutoMirrored.Outlined.OpenInNew, "Abrir no site") {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            if (it.status == Status.DONE) Action(Icons.Outlined.Refresh, "Baixar de novo") { close { vm.manager.retry(it.id) } }
            if (it.status == Status.DONE || it.status == Status.ERROR) Action(Icons.Outlined.PlaylistRemove, "Tirar da lista") { close { vm.removeWithUndo(it) } }
            if (it.status == Status.DONE && it.outputs.isNotEmpty()) Action(Icons.Outlined.DeleteOutline, "Apagar os arquivos do celular", danger = true) { confirmDelete = true }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        icon = { Icon(Icons.Outlined.DeleteOutline, null) },
        title = { Text("Apagar os arquivos?") },
        text = { Text(if (it.outputs.size == 1) "\"${it.outputs.first().name}\" sai do celular. Não dá para desfazer." else "Os ${it.outputs.size} arquivos deste download saem do celular. Não dá para desfazer.") },
        confirmButton = { TextButton({ confirmDelete = false; vm.manager.remove(it.id, deleteFiles = true); vm.toast("Arquivos apagados"); onDismiss() }) { Text("Apagar") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancelar") } },
    )
}

/** Botões de uma folha: lado a lado e, se não couberem (letra grande), na linha de baixo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Actions(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Action(icon: ImageVector, text: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(text, color = color) },
        leadingContent = { Icon(icon, null, tint = if (danger) color else MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Linha de armazenamento no pé da lista: quanto o app ocupa e quanto ainda cabe. */
@Composable
fun StorageLine(items: List<QueueItem>) {
    val ctx = LocalContext.current
    val space = remember(items.count { it.status == Status.DONE }) { Storage.space(ctx) }
    val used = Storage.appUsage(items)
    val total = space.total.coerceAtLeast(1)
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(6.dp)) {
            LinearProgressIndicator(
                { ((total - space.free).toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(6.dp),
                color = scheme.outline, trackColor = scheme.surfaceContainerHighest, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
            )
            LinearProgressIndicator(
                { (used.toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(6.dp),
                color = scheme.primary, trackColor = Color.Transparent, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
            )
        }
        Text(
            "Subaru DL: ${formatBytes(used)} · livres ${formatBytes(space.free)} de ${formatBytes(space.total)}",
            style = MaterialTheme.typography.bodySmall.tabular, color = scheme.onSurfaceVariant,
        )
    }
}
