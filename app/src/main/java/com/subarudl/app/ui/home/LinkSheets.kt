package com.subarudl.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.subarudl.app.BatchSource
import com.subarudl.app.GalleryLink
import com.subarudl.app.MainViewModel
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.GalleryFile
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.Mode
import com.subarudl.app.data.PlaylistEntry
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SectionTitle
import com.subarudl.app.ui.components.SwitchItem
import com.subarudl.app.ui.components.Thumb
import com.subarudl.app.ui.theme.tabular
import com.subarudl.app.util.audioFormats
import com.subarudl.app.util.bitrateChoices
import com.subarudl.app.util.effectiveQuality
import com.subarudl.app.util.expandSelection
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.formatCount
import com.subarudl.app.util.formatDuration
import com.subarudl.app.util.formatLongDuration
import com.subarudl.app.util.qualityChoices
import com.subarudl.app.util.selectionSpec
import com.subarudl.app.util.videoContainers

// ---------- Peças das folhas ----------

/** Folha do Material 3 com o conteúdo rolando e a barra de ações fixa embaixo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkSheet(onDismiss: () -> Unit, actions: @Composable ColumnScope.() -> Unit, content: LazyListScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column {
            LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(bottom = 8.dp), content = content)
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp), content = actions)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(start = Gutter, end = Gutter, top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ModeSwitch(mode: Mode, onSelect: (Mode) -> Unit) {
    val options = listOf(Triple(Mode.VIDEO, "Vídeo", Icons.Outlined.Videocam), Triple(Mode.AUDIO, "Áudio", Icons.Outlined.Headphones))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Gutter)) {
        options.forEachIndexed { i, (value, label, icon) ->
            SegmentedButton(
                selected = mode == value, onClick = { onSelect(value) }, shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = { SegmentedButtonDefaults.Icon(mode == value) { Icon(icon, null, Modifier.size(SegmentedButtonDefaults.IconSize)) } },
            ) { Text(label) }
        }
    }
}

/** Qualidade, formato, faixa de áudio e legenda: o que muda conforme vídeo ou áudio. */
private fun LazyListScope.formatOptions(info: MediaInfo?, o: DownloadOptions, set: (DownloadOptions) -> Unit, playlistLike: Boolean) {
    item(key = "quality") {
        Column {
            if (o.mode == Mode.VIDEO) {
                val choices = qualityChoices(if (playlistLike) null else info, o.fps30)
                if (choices.size == 1) {
                    val only = choices.first()
                    Label("Qualidade")
                    Text(
                        if (only.value == "best" && only.label == "Melhor") "A única que o arquivo tem" else listOfNotNull(only.label, only.sub).joinToString(" · "),
                        Modifier.padding(horizontal = Gutter), style = MaterialTheme.typography.bodyLarge,
                    )
                    if (info?.site.equals("YouTube", ignoreCase = true)) {
                        Hint("O YouTube liberou só esta qualidade agora. Com a conta conectada em Ajustes, ele costuma liberar as outras.")
                    }
                } else {
                    Label("Qualidade")
                    ChoiceChips(choices, effectiveQuality(o.quality, choices), { set(o.copy(quality = it, formatId = null)) })
                    if (playlistLike) Hint("Em cada vídeo vale a escolhida ou a maior abaixo dela.")
                }
            } else {
                val lossless = o.audioFormat == "wav" || o.audioFormat == "flac"
                Label("Qualidade do áudio")
                ChoiceChips(bitrateChoices(if (playlistLike) null else info), o.audioBitrate, { set(o.copy(audioBitrate = it)) }, enabled = !lossless)
                if (lossless) Hint("${o.audioFormat.uppercase()} é sem perda: a taxa não se aplica.")
            }
        }
    }
    item(key = "fields") {
        Column(Modifier.padding(horizontal = Gutter).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (o.mode == Mode.VIDEO) DropField("Formato", o.container, videoContainers, { set(o.copy(container = it)) }, Modifier.fillMaxWidth())
            else DropField("Formato", o.audioFormat, audioFormats, { set(o.copy(audioFormat = it)) }, Modifier.fillMaxWidth())
            if (info != null && !playlistLike) AudioTrackField(info, o, set)
            if (o.mode == Mode.VIDEO) {
                if (info != null && !playlistLike) SubtitleFields(info, o, set) else GenericSubtitleFields(o, set)
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(horizontal = Gutter, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Uma linha da lista de uma playlist: marcar, capa, título e duração. */
@Composable
private fun EntryRow(e: PlaylistEntry, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() }).padding(horizontal = Gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = on, onCheckedChange = null)
        Spacer(Modifier.width(12.dp))
        Thumb(e.thumb, Modifier.width(80.dp).aspectRatio(16f / 9f), icon = Icons.Outlined.Movie)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(e.index.toString(), e.channel, e.duration?.let { formatDuration(it) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.tabular, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DownloadButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier.height(48.dp), enabled = enabled) {
        Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------- Um link (vídeo, música ou playlist) ----------

@Composable
fun VideoSheet(vm: MainViewModel, info: MediaInfo) {
    val ctx = LocalContext.current
    val o = vm.homeOptions
    val set = { n: DownloadOptions -> vm.homeOptions = n }
    val total = info.entries.size
    val selected = if (info.isPlaylist) expandSelection(o.playlistItems, total) else emptySet()

    LinkSheet(
        onDismiss = { vm.clearHome() },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ vm.downloadHome(false) }, Modifier.weight(1f).height(48.dp)) { Text("Pôr na fila", maxLines = 1) }
                DownloadButton(if (info.isPlaylist) "Baixar ${selected.size}" else "Baixar", enabled = !info.isPlaylist || selected.isNotEmpty(), Modifier.weight(1f)) { vm.downloadHome(true) }
            }
        },
    ) {
        item(key = "head") {
            Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.width(136.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                        .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.pageUrl.ifEmpty { vm.homeInfoUrl })).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                ) {
                    val cover = info.thumb ?: info.entries.firstOrNull()?.thumb
                    Thumb(cover, Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), icon = Icons.Outlined.Movie)
                    // Tocar abre a página no site (não toca o vídeo): o sinal é o de abrir fora, e só sobre uma capa de verdade
                    if (cover != null) {
                        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Abrir no site", Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                    if (info.duration != null && !info.isPlaylist) {
                        Text(
                            formatDuration(info.duration), Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xB3000000)).padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.labelSmall.tabular, color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(info.playlistTitle ?: info.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    info.channel?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Text(
                        listOfNotNull(
                            info.site.takeIf { it.isNotEmpty() },
                            if (info.isPlaylist) "playlist com $total" else info.views?.let { formatCount(it) + " visualizações" },
                            if (info.isLive) "ao vivo" else null,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    )
                }
            }
        }
        item(key = "mode") {
            Column(Modifier.padding(top = 20.dp)) { ModeSwitch(o.mode) { set(vm.withMode(o, it)) } }
        }
        formatOptions(info, o, set, playlistLike = info.isPlaylist)
        if (info.isPlaylist) {
            item(key = "entries-head") {
                Column(Modifier.padding(top = 12.dp)) {
                    SectionTitle("${selected.size} de $total marcados") {
                        if (selected.size == total) TextButton({ set(o.copy(playlistItems = "0")) }) { Text("Desmarcar todos") }
                        else TextButton({ set(o.copy(playlistItems = "")) }) { Text("Marcar todos") }
                    }
                }
            }
            items(info.entries, key = { "e-${it.index}" }) { e ->
                val on = e.index in selected
                EntryRow(e, on) { set(o.copy(playlistItems = selectionSpec(if (on) selected - e.index else selected + e.index, total))) }
            }
        }
        item(key = "where") {
            Column(Modifier.padding(top = 12.dp)) {
                HorizontalDivider(Modifier.padding(horizontal = Gutter))
                DestinationItem(o.destination, o.mode == Mode.AUDIO, { set(o.copy(destination = it)) })
                OrganizeField(o.subfolder, o.subfolderCustom, { v, c -> set(o.copy(subfolder = v, subfolderCustom = c)) }, Modifier.padding(horizontal = Gutter))
                Spacer(Modifier.height(8.dp))
                AdvancedSection(info, o, info.isPlaylist, set)
            }
        }
    }
}

// ---------- Vários links ----------

@Composable
fun BatchSheet(vm: MainViewModel) {
    val sources = vm.batchSources
    val o = vm.batchOptions
    val set = { n: DownloadOptions -> vm.batchOptions = n }
    val count = vm.batchSelectedCount()
    val seconds = sources.sumOf { s ->
        val info = s.info ?: return@sumOf 0L
        if (info.isPlaylist) info.entries.filter { it.index in s.selected }.sumOf { it.duration ?: 0 } else if (s.selected.isNotEmpty()) info.duration ?: 0 else 0
    }
    val estimate = if (o.mode == Mode.AUDIO && o.audioBitrate > 0 && o.audioFormat != "wav" && o.audioFormat != "flac") seconds * o.audioBitrate * 1000L / 8 else null

    LinkSheet(
        onDismiss = { vm.clearBatch() },
        actions = {
            DownloadButton(
                when (count) { 0 -> "Marque o que baixar"; 1 -> "Baixar 1 item"; else -> "Baixar $count itens" },
                enabled = count > 0 && !vm.batchLoading, Modifier.fillMaxWidth(),
            ) { vm.downloadBatch() }
        },
    ) {
        item(key = "head") {
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${sources.size} links", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (vm.batchLoading) "Lendo os links…" else listOfNotNull(
                        "$count ${if (count == 1) "item marcado" else "itens marcados"}",
                        if (seconds > 0) formatLongDuration(seconds) else null,
                        estimate?.let { "cerca de ${formatBytes(it)}" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium.tabular, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (vm.batchLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        item(key = "mode") { Column(Modifier.padding(top = 20.dp)) { ModeSwitch(o.mode) { set(vm.withMode(o, it)) } } }
        formatOptions(null, o, set, playlistLike = true)
        item(key = "sources-head") {
            val allSelected = sources.all { s -> s.info == null || s.selected.size >= s.total }
            Column(Modifier.padding(top = 12.dp)) {
                SectionTitle("Links") {
                    TextButton({
                        sources.forEachIndexed { i, s ->
                            val info = s.info ?: return@forEachIndexed
                            vm.updateSource(i) { it.copy(selected = if (allSelected) emptySet() else if (info.isPlaylist) info.entries.map { e -> e.index }.toSet() else setOf(1)) }
                        }
                    }) { Text(if (allSelected) "Desmarcar todos" else "Marcar todos") }
                }
            }
        }
        sources.forEachIndexed { index, s ->
            item(key = "src-$index") { SourceRow(vm, index, s) }
            val info = s.info
            if (info != null && info.isPlaylist && s.expanded) {
                items(info.entries, key = { "src-$index-${it.index}" }) { e ->
                    val on = e.index in s.selected
                    EntryRow(e, on) { vm.updateSource(index) { cur -> cur.copy(selected = if (on) cur.selected - e.index else cur.selected + e.index) } }
                }
            }
        }
        item(key = "rules") {
            Column(Modifier.padding(top = 12.dp)) {
                HorizontalDivider(Modifier.padding(horizontal = Gutter))
                SwitchItem("Uma pasta por playlist", "Cada lista fica na própria pasta", vm.batchFolderByPlaylist, { vm.batchFolderByPlaylist = it })
                SwitchItem("Numerar os arquivos", "01, 02, 03… no começo do nome", vm.batchNumbering, { vm.batchNumbering = it })
                SwitchItem("Embutir as capas", "A miniatura vira a capa de cada arquivo", vm.batchCovers, { vm.batchCovers = it })
                DestinationItem(o.destination, o.mode == Mode.AUDIO, { set(o.copy(destination = it)) })
            }
        }
    }
}

@Composable
private fun SourceRow(vm: MainViewModel, index: Int, s: BatchSource) {
    val info = s.info
    val playlist = info?.isPlaylist == true
    ListItem(
        headlineContent = { Text(info?.playlistTitle ?: info?.title ?: s.url, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            when {
                s.loading -> Text("Lendo…")
                s.error != null -> Text(if (needsLogin(s.error)) withoutLoginPath(s.error) else s.error, color = MaterialTheme.colorScheme.error, maxLines = 3)
                playlist -> Text("${s.selected.size} de ${s.total} marcados")
                info != null -> Text(listOfNotNull(info.channel, info.duration?.let { formatDuration(it) }).joinToString(" · "))
            }
        },
        leadingContent = {
            when {
                s.loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                info != null && !playlist -> Checkbox(checked = 1 in s.selected, onCheckedChange = { on -> vm.updateSource(index) { it.copy(selected = if (on) setOf(1) else emptySet()) } })
                info != null -> Thumb(info.thumb ?: info.entries.firstOrNull()?.thumb, Modifier.width(56.dp).aspectRatio(16f / 9f), icon = Icons.Outlined.Movie)
                else -> Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            }
        },
        trailingContent = when {
            playlist -> ({ Icon(if (s.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (s.expanded) "Recolher" else "Ver os itens") })
            needsLogin(s.error) -> ({ TextButton({ vm.openSettings("contas") }) { Text("Entrar") } })
            else -> null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (playlist) Modifier.clickable { vm.updateSource(index) { it.copy(expanded = !it.expanded) } } else Modifier,
    )
}

// ---------- Imagens (gallery-dl) ----------

private val organizeOptions = listOf(
    Opt("flat", "Sem subpastas"),
    Opt("site", "Uma pasta por site"),
    Opt("profile", "Uma pasta por perfil"),
    Opt("default", "Site e depois perfil"),
    Opt("custom", "Personalizado…"),
)

@Composable
fun GallerySheet(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val links = vm.galLinks
    val o = vm.galOptions
    val allLoaded = links.all { it.loaded }
    val files = links.sumOf { it.selected.size }
    val count = vm.galleryCount()

    LinkSheet(
        onDismiss = { vm.galLinks = emptyList() },
        actions = {
            DownloadButton(
                when {
                    count == 0 -> "Marque o que baixar"
                    allLoaded -> if (files == 1) "Baixar 1 arquivo" else "Baixar $files arquivos"
                    else -> if (count == 1) "Baixar tudo do link" else "Baixar $count links"
                },
                enabled = count > 0, Modifier.fillMaxWidth(),
            ) { vm.galleryDownload() }
        },
    ) {
        item(key = "head") {
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (links.size == 1) "Imagens do link" else "Imagens de ${links.size} links", style = MaterialTheme.typography.titleLarge)
                if (links.any { it.files.isNotEmpty() }) {
                    Text("Toque nas fotos para marcar ou desmarcar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        links.forEachIndexed { index, link ->
            item(key = "gl-$index") { GalleryLinkRow(vm, index, link, removable = links.size > 1) }
            if (link.expanded && link.loaded && link.files.isNotEmpty()) {
                item(key = "gl-$index-tools") {
                    Row(Modifier.padding(horizontal = 8.dp)) {
                        TextButton({ vm.updateGal(index) { it.copy(selected = it.files.map { f -> f.index }.toSet()) } }) { Text("Marcar todas") }
                        TextButton({ vm.updateGal(index) { it.copy(selected = emptySet()) } }) { Text("Desmarcar") }
                    }
                }
                items(link.files.chunked(3), key = { "gl-$index-${it.first().index}" }) { row ->
                    Row(Modifier.padding(horizontal = Gutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { f ->
                            val on = f.index in link.selected
                            FileTile(f, link.url, on, Modifier.weight(1f)) {
                                vm.updateGal(index) { cur -> cur.copy(selected = if (on) cur.selected - f.index else cur.selected + f.index) }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (!link.end) item(key = "gl-$index-more") {
                    OutlinedButton({ vm.galleryLoad(index, true) }, Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp), enabled = !link.loading) {
                        Text(if (link.loading) "Carregando…" else "Carregar mais fotos")
                    }
                }
            }
        }
        item(key = "gal-options") {
            Column(Modifier.padding(top = 12.dp)) {
                HorizontalDivider(Modifier.padding(horizontal = Gutter))
                DestinationItem(o.destination ?: settings.galleryDest, false, { vm.galOptions = o.copy(destination = it) }, gallery = true)
                Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropField("Organizar em pastas", o.organize.takeIf { x -> organizeOptions.any { it.value == x } } ?: "default", organizeOptions, { vm.galOptions = o.copy(organize = it) }, Modifier.fillMaxWidth())
                    if (o.organize == "custom") {
                        OutlinedTextField(
                            o.customFormat, { vm.galOptions = o.copy(customFormat = it) }, Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("Pastas separadas por /") }, supportingText = { Text("Ex.: {category}/{user}") },
                        )
                    }
                }
                SwitchItem("Pular o que já baixei", "Lembra cada arquivo baixado e não baixa de novo", settings.gallerySkipDone, { v -> vm.update { it.copy(gallerySkipDone = v) } })
            }
        }
    }
}

@Composable
private fun GalleryLinkRow(vm: MainViewModel, index: Int, link: GalleryLink, removable: Boolean) {
    ListItem(
        headlineContent = { Text(link.url.removePrefix("https://").removePrefix("http://").removePrefix("www."), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            when {
                link.loading && !link.loaded -> Text("Procurando as fotos…")
                link.error != null -> Text(if (needsLogin(link.error)) withoutLoginPath(link.error) else link.error, color = MaterialTheme.colorScheme.error, maxLines = 4)
                link.loaded -> Text("${link.selected.size} de ${link.files.size}${if (link.end) "" else "+"} marcadas")
                else -> Text("Baixa tudo, ou abra para escolher as fotos")
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (link.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else if (!link.loaded || link.files.isNotEmpty()) {
                    val open = link.expanded && link.loaded
                    IconButton({ if (!link.loaded) vm.galleryLoad(index, false) else vm.updateGal(index) { it.copy(expanded = !it.expanded) } }) {
                        Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (open) "Recolher as fotos" else "Ver as fotos")
                    }
                }
                if (needsLogin(link.error)) TextButton({ vm.openSettings("contas") }) { Text("Entrar") }
                if (link.error?.contains("não é suportado") == true) TextButton({ vm.retryAsVideo(link.url) }) { Text("Como vídeo") }
                if (removable) IconButton({ vm.galLinks = vm.galLinks.filterIndexed { i, _ -> i != index } }) { Icon(Icons.Outlined.Close, "Tirar este link") }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun FileTile(f: GalleryFile, page: String, on: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val referer = remember(page) { runCatching { java.net.URI(page).let { "${it.scheme}://${it.host}/" } }.getOrNull() }
    val inset by animateDpAsState(if (on) 10.dp else 0.dp, label = "foto")
    Box(modifier.aspectRatio(1f).toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() })) {
        Box(Modifier.fillMaxSize().padding(inset).clip(RoundedCornerShape(if (on) 12.dp else 4.dp)).background(scheme.surfaceContainerHighest)) {
            if (f.video) Icon(Icons.Filled.PlayArrow, "Vídeo", Modifier.align(Alignment.Center).size(32.dp), tint = scheme.onSurfaceVariant)
            else {
                val request = remember(f.url, referer) {
                    ImageRequest.Builder(ctx).data(f.url).apply {
                        if (referer != null) httpHeaders(NetworkHeaders.Builder().add("Referer", referer).add("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36").build())
                    }.build()
                }
                AsyncImage(request, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            // Sombra no canto para o círculo aparecer até em foto clara
            Box(Modifier.align(Alignment.TopStart).size(56.dp).background(Brush.radialGradient(listOf(Color(0x8C000000), Color.Transparent), center = Offset.Zero, radius = 150f)))
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp).clip(CircleShape)
                    .then(if (on) Modifier.background(scheme.primary) else Modifier.background(Color(0x40000000)).border(2.dp, Color.White, CircleShape)),
                contentAlignment = Alignment.Center,
            ) { if (on) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = scheme.onPrimary) }
        }
        Text(
            f.index.toString(), Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xB3000000)).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall.tabular, color = Color.White,
        )
    }
}
