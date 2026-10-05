package com.subarudl.app.ui.queue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sd
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.MainViewModel
import com.subarudl.app.QueueTab
import com.subarudl.app.data.Kind
import com.subarudl.app.data.Mode
import com.subarudl.app.data.QueueItem
import com.subarudl.app.data.Status
import com.subarudl.app.data.Storage
import com.subarudl.app.ui.components.Badge
import com.subarudl.app.ui.components.BlueBadge
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.ChipSmall
import com.subarudl.app.ui.components.DarkBadge
import com.subarudl.app.ui.components.Empty
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.ProgressTrack
import com.subarudl.app.ui.components.SCard
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.components.Sparkline
import com.subarudl.app.ui.components.TabPill
import com.subarudl.app.ui.components.Thumb
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.formatEta
import com.subarudl.app.util.formatSpeed
import java.text.DateFormat
import java.util.Date

@Composable
fun QueueScreen(vm: MainViewModel) {
    val items by vm.manager.items.collectAsState()
    val speeds by vm.manager.speedHistory.collectAsState()
    val peak by vm.manager.peak.collectAsState()
    val net by vm.manager.net.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    val ctx = LocalContext.current

    fun matches(it: QueueItem): Boolean {
        if (query.isNotBlank() && !(it.title.contains(query, true) || it.url.contains(query, true) || (it.channel ?: "").contains(query, true))) return false
        return when (filter) {
            "MP4" -> it.kind == Kind.VIDEO && it.options.mode == Mode.VIDEO && (it.ext.equals("mp4", true) || (it.ext.isEmpty() && it.options.container == "mp4"))
            "MP3" -> it.kind == Kind.VIDEO && it.options.mode == Mode.AUDIO
            "4K" -> it.quality.removeSuffix("p").toIntOrNull()?.let { h -> h >= 2160 } == true || it.options.quality.toIntOrNull()?.let { h -> h >= 2160 } == true
            else -> true
        }
    }

    val running = items.filter { it.active }
    val pending = items.filter { it.status == Status.WAITING || it.status == Status.PAUSED }
    val done = items.filter { it.status == Status.DONE }.sortedByDescending { it.finishedAt }
    val failed = items.filter { it.status == Status.ERROR }.sortedByDescending { it.finishedAt }
    val live = running + pending
    val tab = vm.queueTab
    val totalSpeed = running.sumOf { it.speed }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ---- Telemetria ----
        item {
            SCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (running.isNotEmpty()) C.Gold else C.Dim))
                    Spacer(Modifier.width(8.dp))
                    SectionLabel("Telemetria DL", Modifier.weight(1f), color = C.Gold)
                    Row(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x0DFFFFFF)).border(1.dp, C.Border, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Wifi, null, Modifier.size(15.dp), tint = C.BluePale)
                        Spacer(Modifier.width(6.dp))
                        Mono(net.label, color = C.Text, style = T.labelMd)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val (value, unit) = formatSpeed(totalSpeed).split(" ").let { it[0] to it.getOrElse(1) { "" } }
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                        Text(value, style = T.headlineLg.copy(fontSize = 38.sp, lineHeight = 42.sp))
                        Spacer(Modifier.width(6.dp))
                        Text(unit, style = T.labelLg, color = C.Muted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SectionLabel("Pico máximo")
                        Mono(formatSpeed(peak.toDouble()), color = C.CyanPale, style = T.labelLg)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Sparkline(speeds)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Stat(Icons.Outlined.CloudDownload, "${running.size} baixando", C.BluePale)
                    Stat(Icons.Outlined.CheckCircle, "${done.size} concluídos", C.Gold)
                    Stat(Icons.Outlined.Timer, if (net.online) "Estável" else "Offline", if (net.online) C.CyanPale else C.ErrorStrong)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val anyPaused = items.any { it.status == Status.PAUSED }
                    val anyRunning = items.any { it.active || it.status == Status.WAITING }
                    if (anyPaused && !anyRunning) {
                        BtnGhost("Retomar Todos", { vm.manager.resumeAll() }, Modifier.weight(1f), icon = Icons.Outlined.PlayArrow, color = C.Gold)
                    } else {
                        BtnGhost("Pausar Todos", { vm.manager.pauseAll() }, Modifier.weight(1f), icon = Icons.Outlined.PauseCircle, color = C.Gold, enabled = anyRunning)
                    }
                    BtnGhost("Limpar Fila", {
                        if (live.isNotEmpty()) vm.manager.clearQueue() else vm.manager.clearFinished(setOf(Status.DONE, Status.ERROR))
                    }, Modifier.weight(1f), icon = Icons.Outlined.DeleteSweep)
                }
            }
        }

        // ---- Abas e filtro ----
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(10.dp)).padding(4.dp)) {
                TabPill("Baixando", live.size, tab == QueueTab.ACTIVE) { vm.queueTab = QueueTab.ACTIVE }
                TabPill("Concluídos", done.size, tab == QueueTab.DONE) { vm.queueTab = QueueTab.DONE }
                TabPill("Falhas", failed.size, tab == QueueTab.FAILED) { vm.queueTab = QueueTab.FAILED }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(8.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = C.Muted)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Filtrar por nome ou link…", style = T.bodySm.copy(fontSize = 14.sp), color = C.Dim)
                        BasicTextField(query, { query = it }, singleLine = true, textStyle = T.bodyMd.copy(fontSize = 14.sp), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
                    }
                }
                listOf("MP4", "MP3", "4K").forEach { f -> ChipSmall(f, filter == f, { filter = if (filter == f) "" else f }) }
            }
        }

        when (tab) {
            QueueTab.ACTIVE -> {
                val shownRunning = running.filter(::matches)
                val shownPending = pending.filter(::matches)
                if (live.isEmpty()) item { Empty("Nada baixando agora. Cole um link na aba Início.", icon = Icons.Outlined.CloudDownload) }
                if (shownRunning.isNotEmpty()) {
                    item { SectionTitle(Icons.Outlined.CloudDownload, "Baixando Agora", "STAGE LIVE") }
                    items(shownRunning, key = { it.id }) { ActiveCard(vm, it, accent = if (running.indexOf(it) % 2 == 0) C.BluePale else C.Gold) }
                }
                if (shownPending.isNotEmpty()) {
                    item { SectionTitle(Icons.Outlined.Timer, "Na Fila", "${shownPending.size}") }
                    items(shownPending, key = { it.id }) { ActiveCard(vm, it, accent = C.Dim) }
                }
            }
            QueueTab.DONE -> {
                val shown = done.filter(::matches)
                item { SectionTitle(Icons.Outlined.CheckCircle, "Concluídos Recentemente", "${shown.size}") }
                if (shown.isEmpty()) item { Empty("Nenhum download concluído ainda.") }
                items(shown, key = { it.id }) { DoneRow(vm, it) }
            }
            QueueTab.FAILED -> {
                val shown = failed.filter(::matches)
                item { SectionTitle(Icons.Outlined.ErrorOutline, "Falhas", "${shown.size}") }
                if (shown.isEmpty()) item { Empty("Nenhuma falha. Tudo certo por aqui.") }
                items(shown, key = { it.id }) { FailedCard(vm, it) }
            }
        }

        // ---- Armazenamento ----
        item { StorageCard(done) }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(5.dp))
        Mono(text, color = C.Muted, style = T.labelMd)
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, right: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = C.Gold)
        Spacer(Modifier.width(8.dp))
        Text(title, style = T.headlineSm, modifier = Modifier.weight(1f))
        Mono(right, color = C.CyanPale, style = T.labelMd)
    }
}

@Composable
private fun ActiveCard(vm: MainViewModel, it: QueueItem, accent: Color) {
    val gallery = it.kind == Kind.GALLERY
    SCard(accent = accent, padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            if (gallery) GalleryMosaic(it, Modifier.size(78.dp))
            else Box {
                Thumb(it.thumb, Modifier.size(width = 84.dp, height = 84.dp))
                if (it.quality.isNotEmpty()) DarkBadge(it.quality, C.Gold, Modifier.align(Alignment.BottomStart).padding(4.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(it.title, style = T.title.copy(fontSize = 16.sp, lineHeight = 21.sp), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    BlueBadge(badgeFor(it))
                }
                Text(listOfNotNull(it.channel ?: it.site.takeIf { s -> s.isNotEmpty() }, it.stage.takeIf { s -> s.isNotEmpty() && it.status != Status.DOWNLOADING }).joinToString(" • "), style = T.bodySm, color = C.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (gallery) {
            Row {
                Mono("${it.fileCount} arquivos" + if (it.skipped > 0) " • ${it.skipped} pulados" else "", color = C.Text, style = T.labelLg, modifier = Modifier.weight(1f))
                if (it.failed > 0) Mono("${it.failed} falhas", color = C.ErrorStrong, style = T.labelMd)
            }
            Spacer(Modifier.height(8.dp))
            ProgressTrack(0f, indeterminate = true)
            Spacer(Modifier.height(4.dp))
            Mono(it.stage.ifEmpty { "Aguardando" }, color = C.Muted, style = T.labelMd)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mono(
                    if (it.total > 0) "${formatBytes(it.downloaded)} / ${formatBytes(it.total)}" else if (it.itemCount > 1) "Vídeo ${it.itemIndex} de ${it.itemCount}" else it.stage.ifEmpty { "Na fila" },
                    color = C.Text, style = T.labelLg, modifier = Modifier.weight(1f),
                )
                if (it.active) Mono(formatSpeed(it.speed), color = C.CyanPale, style = T.labelLg)
            }
            Spacer(Modifier.height(8.dp))
            ProgressTrack(it.progress, indeterminate = !it.active && it.progress <= 0f)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, Modifier.size(15.dp), tint = C.Muted)
                Spacer(Modifier.width(5.dp))
                Mono(
                    when {
                        it.status == Status.PAUSED -> "Pausado"
                        it.status == Status.WAITING -> it.stage.ifEmpty { "Aguardando vaga" }
                        it.eta >= 0 -> formatEta(it.eta) + " restantes"
                        else -> it.stage.ifEmpty { "Calculando…" }
                    },
                    color = C.Muted, style = T.labelMd, modifier = Modifier.weight(1f),
                )
                Mono("${(it.progress * 100).toInt()}%", color = C.Text, style = T.labelLg)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (it.status == Status.PAUSED) BtnGhost("Retomar", { vm.manager.resume(it.id) }, Modifier.weight(1f), icon = Icons.Outlined.PlayArrow, color = C.Gold)
            else BtnGhost("Pausar", { vm.manager.pause(it.id) }, Modifier.weight(1f), icon = Icons.Outlined.Pause)
            BtnGhost("Cancelar", { vm.manager.cancel(it.id) }, Modifier.weight(1f), icon = Icons.Filled.Close, color = C.Error)
        }
    }
}

private fun badgeFor(it: QueueItem): String = when {
    it.kind == Kind.GALLERY -> "GALERIA"
    it.options.mode == Mode.AUDIO -> it.options.audioFormat.uppercase()
    it.isPlaylist -> "LISTA"
    else -> (it.ext.ifEmpty { it.options.container }).uppercase()
}

@Composable
fun GalleryMosaic(it: QueueItem, modifier: Modifier = Modifier) {
    val pics = it.outputs.filter { o -> o.mime.startsWith("image/") }.take(4)
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(C.CardHighest)) {
        if (pics.isEmpty()) Icon(Icons.Outlined.CloudDownload, null, Modifier.align(Alignment.Center).size(26.dp), tint = C.Dim)
        else Column(Modifier.fillMaxSize()) {
            val rows = pics.chunked(2)
            rows.forEach { r ->
                Row(Modifier.weight(1f)) { r.forEach { p -> Thumb(p.uri, Modifier.weight(1f).fillMaxSize(), shape = RoundedCornerShape(0.dp)) } }
            }
        }
    }
}

@Composable
private fun DoneRow(vm: MainViewModel, it: QueueItem) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val gallery = it.kind == Kind.GALLERY
    val audio = it.isAudio
    SCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (gallery) GalleryMosaic(it, Modifier.size(64.dp))
            else Thumb(it.thumb ?: it.outputs.firstOrNull()?.takeIf { o -> o.mime.startsWith("video/") }?.uri, Modifier.size(width = 76.dp, height = 64.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(it.title, style = T.title.copy(fontSize = 15.sp, lineHeight = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                val meta = if (gallery) listOfNotNull("${it.fileCount} arquivos", if (it.skipped > 0) "${it.skipped} pulados" else null, if (it.failed > 0) "${it.failed} falhas" else null)
                else listOfNotNull(
                    if (audio) it.options.audioFormat.uppercase() else (it.ext.ifEmpty { it.options.container }).uppercase(),
                    it.quality.takeIf { q -> q.isNotEmpty() }, if (it.sizeBytes > 0) formatBytes(it.sizeBytes) else null,
                    if (it.isPlaylist && it.fileCount > 1) "${it.fileCount} faixas" else null,
                )
                Mono(meta.joinToString(" • "), color = C.CyanPale, style = T.labelMd)
                if (it.error != null) Text(it.error, style = T.bodySm, color = C.Gold, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            val first = it.outputs.firstOrNull()
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    IconBtn(menu) { menu = true }
                    DropdownMenu(menu, { menu = false }, containerColor = C.CardHigh) {
                        MenuItem("Compartilhar", first != null) { menu = false; first?.let { f -> runCatching { ctx.startActivity(Storage.shareIntent(f)) } } }
                        MenuItem("Copiar o link") {
                            menu = false
                            (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("link", it.url))
                            vm.toast("Link copiado")
                        }
                        MenuItem("Baixar de novo") { menu = false; vm.manager.retry(it.id) }
                        MenuItem("Remover da lista") { menu = false; vm.manager.remove(it.id) }
                        MenuItem("Apagar os arquivos", it.outputs.isNotEmpty(), danger = true) { menu = false; vm.manager.remove(it.id, deleteFiles = true); vm.toast("Arquivos apagados") }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val first = it.outputs.firstOrNull()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Mono(if (it.finishedAt > 0) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it.finishedAt)) else "", color = C.Dim, style = T.labelSm, modifier = Modifier.weight(1f))
            if (first != null) {
                if (audio || first.mime.startsWith("audio/")) BtnGhost("Ouvir", { open(ctx, vm, first) }, icon = Icons.Outlined.Headphones, height = 40.dp)
                else BtnPrimary(if (gallery) "Ver" else "Abrir", { open(ctx, vm, first) }, icon = Icons.Outlined.PlayArrow, height = 40.dp)
            }
        }
    }
}

private fun open(ctx: Context, vm: MainViewModel, f: com.subarudl.app.data.OutputFile) {
    if (!Storage.exists(ctx, f.uri)) {
        vm.toast("O arquivo não está mais lá (foi movido ou apagado)")
        return
    }
    runCatching { ctx.startActivity(Storage.viewIntent(f)) }.onFailure { vm.toast("Nenhum app para abrir esse arquivo") }
}

@Composable
private fun IconBtn(menu: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.MoreVert, "Mais", Modifier.size(22.dp), tint = C.Muted)
    }
}

@Composable
private fun MenuItem(text: String, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, style = T.labelLg, color = if (danger) C.Error else C.Text) }, onClick = onClick, enabled = enabled)
}

@Composable
private fun FailedCard(vm: MainViewModel, it: QueueItem) {
    SCard(accent = C.ErrorStrong, padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            if (it.kind == Kind.GALLERY) GalleryMosaic(it, Modifier.size(64.dp)) else Thumb(it.thumb, Modifier.size(width = 76.dp, height = 64.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(it.title, style = T.title.copy(fontSize = 15.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(it.error ?: "Falhou", style = T.bodySm, color = C.Error, modifier = Modifier.padding(top = 3.dp), maxLines = 4)
                it.hint?.let { h -> Text(h, style = T.bodySm, color = C.Muted, modifier = Modifier.padding(top = 3.dp)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BtnPrimary("Tentar de novo", { vm.manager.retry(it.id) }, Modifier.weight(1f), icon = Icons.Outlined.Refresh, height = 44.dp)
            BtnGhost("Remover", { vm.manager.remove(it.id) }, Modifier.weight(1f), icon = Icons.Filled.Close, color = C.Error, height = 44.dp)
        }
    }
}

@Composable
private fun StorageCard(done: List<QueueItem>) {
    val ctx = LocalContext.current
    val space = remember(done.size) { Storage.space(ctx) }
    val used = Storage.appUsage(done)
    val total = space.total.coerceAtLeast(1)
    val usedByOthers = (total - space.free - used).coerceAtLeast(0)
    SCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Sd, null, Modifier.size(24.dp), tint = C.BluePale)
            Spacer(Modifier.width(10.dp))
            Text("Armazenamento Interno", style = T.title, modifier = Modifier.weight(1f))
            Mono("${(space.free * 100 / total)}% DISPONÍVEL", color = C.CyanPale, style = T.labelMd)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x1FFFFFFF))) {
            if (used > 0) Box(Modifier.weight(used.toFloat().coerceAtLeast(1f)).fillMaxSize().background(C.Blue))
            Box(Modifier.weight(usedByOthers.toFloat().coerceAtLeast(1f)).fillMaxSize().background(Color(0xFF4B5263)))
            Box(Modifier.weight(space.free.toFloat().coerceAtLeast(1f)).fillMaxSize())
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Box(Modifier.size(10.dp).clip(CircleShape).background(C.Blue).align(Alignment.CenterVertically))
            Spacer(Modifier.width(6.dp))
            Mono("Subaru DL: ${formatBytes(used)}", color = C.Text, style = T.labelMd, modifier = Modifier.weight(1f))
            Mono("${formatBytes(space.free)} livres de ${formatBytes(space.total)}", color = C.Muted, style = T.labelMd)
        }
    }
}
