package com.subarudl.app.ui.batch

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.BatchSource
import com.subarudl.app.MainViewModel
import com.subarudl.app.data.Mode
import com.subarudl.app.ui.components.Badge
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnGold
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.CheckSubaru
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.IconTile
import com.subarudl.app.ui.components.LinkField
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.ScreenTitle
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SCard
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.components.Seg
import com.subarudl.app.ui.components.Segmented
import com.subarudl.app.ui.components.SwitchSubaru
import com.subarudl.app.ui.components.Thumb
import com.subarudl.app.ui.home.DestinationRow
import com.subarudl.app.ui.home.GenericSubtitleSelect
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.Manrope
import com.subarudl.app.ui.theme.T
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.audioBitrateOptions
import com.subarudl.app.util.audioFormats
import com.subarudl.app.util.formatDuration
import com.subarudl.app.util.videoContainers
import com.subarudl.app.util.videoQualityOptions

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    val sources = vm.batchSources
    val o = vm.batchOptions
    val selectedCount = vm.batchSelectedCount()
    val totalDuration = sources.sumOf { s ->
        val info = s.info ?: return@sumOf 0L
        if (info.isPlaylist) info.entries.filter { it.index in s.selected }.sumOf { it.duration ?: 0 } else if (s.selected.isNotEmpty()) info.duration ?: 0 else 0
    }
    val estimate = if (o.mode == Mode.AUDIO && o.audioBitrate > 0) totalDuration * o.audioBitrate * 1000L / 8 else null

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenTitle("Download em lote", "Baixe álbuns, cursos e playlists completas com um toque.") }

        item {
            SCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.PlaylistAdd, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Links do lote", style = T.title)
                        Text("Uma playlist ou vários links, um por linha", style = T.labelMd)
                    }
                    if (sources.isNotEmpty()) BtnGhost("Limpar", { vm.clearBatch() }, icon = Icons.Filled.Close, height = 40.dp)
                }
                Spacer(Modifier.height(14.dp))
                LinkField(vm.batchText, { vm.batchText = it }, "Cole os links aqui", maxLines = 5)
                Spacer(Modifier.height(12.dp))
                if (vm.batchLoading) {
                    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = C.Gold, strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Analisando o lote…", style = T.labelLg, color = C.Muted)
                    }
                } else {
                    BtnPrimary("Analisar lote", { vm.analyzeBatch(vm.batchText) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Search, enabled = vm.batchText.isNotBlank())
                }
            }
        }

        if (sources.isNotEmpty()) {
            // ---- Resumo ----
            item {
                val first = sources.firstOrNull { it.info != null }?.info
                SCard(padding = 16.dp) {
                    Row {
                        Box(Modifier.size(104.dp).clip(RoundedCornerShape(10.dp))) {
                            Thumb(first?.thumb, Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp), icon = Icons.Outlined.Album)
                            Row(Modifier.align(Alignment.BottomStart).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xCC000000)).padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Mono("${sources.size} ${if (sources.size == 1) "LINK" else "LINKS"}", color = C.Gold, style = T.labelSm)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.SmartDisplay, null, Modifier.size(16.dp), tint = C.ErrorStrong)
                                Spacer(Modifier.width(6.dp))
                                Mono(first?.site ?: "…", color = C.Muted, style = T.labelMd)
                            }
                            Text(
                                if (sources.size == 1) first?.title ?: sources.first().url else "${sources.size} fontes no lote",
                                style = T.headlineMd.copy(fontSize = 21.sp, lineHeight = 26.sp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Badge("$selectedCount ${if (o.mode == Mode.AUDIO) "Músicas" else "Vídeos"}")
                                if (estimate != null) Badge("${formatBytes(estimate)} est.", color = C.BluePale, background = Color(0x331D4ED8), border = Color(0x4D3B82F6))
                                else if (totalDuration > 0) Badge(formatDuration(totalDuration), color = C.BluePale, background = Color(0x331D4ED8), border = Color(0x4D3B82F6))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Segmented(
                        listOf(Seg(Mode.AUDIO, "Áudio ${o.audioFormat.uppercase()}", Icons.Outlined.Headphones), Seg(Mode.VIDEO, "Vídeo ${o.container.uppercase()}", Icons.Outlined.Videocam)),
                        o.mode, { vm.batchOptions = o.copy(mode = it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    if (o.mode == Mode.AUDIO) {
                        val lossless = o.audioFormat == "wav" || o.audioFormat == "flac"
                        DropField(if (lossless) 0 else o.audioBitrate, audioBitrateOptions(null, lossless), { vm.batchOptions = o.copy(audioBitrate = it) }, Modifier.fillMaxWidth(), enabled = !lossless)
                        Spacer(Modifier.height(10.dp))
                        DropField(o.audioFormat, audioFormats, { vm.batchOptions = o.copy(audioFormat = it) }, Modifier.fillMaxWidth(), prefix = "Formato: ")
                    } else {
                        DropField(o.quality, videoQualityOptions(null, o.fps30), { vm.batchOptions = o.copy(quality = it) }, Modifier.fillMaxWidth(), prefix = "Qualidade: ")
                        Spacer(Modifier.height(10.dp))
                        DropField(o.container, videoContainers, { vm.batchOptions = o.copy(container = it) }, Modifier.fillMaxWidth(), prefix = "Formato: ")
                        Spacer(Modifier.height(10.dp))
                        GenericSubtitleSelect(o, { vm.batchOptions = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    val allSelected = sources.all { s -> s.info == null || s.selected.size >= s.total }
                    BtnGhost(
                        if (allSelected) "Desmarcar todos" else "Marcar todos",
                        {
                            sources.forEachIndexed { i, s ->
                                val info = s.info ?: return@forEachIndexed
                                vm.updateSource(i) { it.copy(selected = if (allSelected) emptySet() else if (info.isPlaylist) info.entries.map { e -> e.index }.toSet() else setOf(1)) }
                            }
                        },
                        Modifier.fillMaxWidth(), icon = Icons.Outlined.PlaylistAddCheck,
                    )
                    Spacer(Modifier.height(14.dp))
                    BtnGold(
                        "Baixar $selectedCount ${if (selectedCount == 1) "Faixa" else "Faixas"}", { vm.downloadBatch() }, Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.RocketLaunch, tag = estimate?.let { formatBytes(it) }, enabled = selectedCount > 0,
                    )
                }
            }

            // ---- Fontes e faixas ----
            sources.forEachIndexed { index, s ->
                item(key = "src-$index-${s.url}") { SourceHeader(vm, index, s) }
                val info = s.info
                if (info != null && s.expanded) {
                    if (info.isPlaylist) {
                        items(info.entries, key = { "e-$index-${it.index}" }) { e ->
                            val on = e.index in s.selected
                            TrackRow(
                                title = e.title, artist = e.channel, duration = e.duration, thumb = e.thumb, on = on, ready = true,
                                size = if (o.mode == Mode.AUDIO && o.audioBitrate > 0) e.duration?.let { it * o.audioBitrate * 1000L / 8 } else null,
                            ) { vm.updateSource(index) { cur -> cur.copy(selected = if (on) cur.selected - e.index else cur.selected + e.index) } }
                        }
                    } else {
                        item(key = "one-$index") {
                            val on = 1 in s.selected
                            TrackRow(info.title, info.channel, info.duration, info.thumb, on, true, null) {
                                vm.updateSource(index) { cur -> cur.copy(selected = if (on) emptySet() else setOf(1)) }
                            }
                        }
                    }
                }
            }

            // ---- Diretrizes ----
            item {
                SCard(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DataUsage, null, Modifier.size(22.dp), tint = C.Gold)
                        Spacer(Modifier.width(10.dp))
                        Text("Diretrizes de Extração", style = T.headlineSm, modifier = Modifier.weight(1f))
                        Mono("Auto ID3", color = C.Dim, style = T.labelMd)
                    }
                    Spacer(Modifier.height(12.dp))
                    DirectiveRow(Icons.Outlined.Folder, "Criar pasta com o nome da playlist", "Cada lista fica na própria subpasta", vm.batchFolderByPlaylist) { vm.batchFolderByPlaylist = it }
                    Spacer(Modifier.height(8.dp))
                    DirectiveRow(Icons.Outlined.FormatListNumbered, "Adicionar numeração", "Prefixo 01, 02, 03… no nome do arquivo", vm.batchNumbering) { vm.batchNumbering = it }
                    Spacer(Modifier.height(8.dp))
                    DirectiveRow(Icons.Outlined.Image, "Baixar capas dos álbuns", "Embute a capa no arquivo", vm.batchCovers) { vm.batchCovers = it }
                    Spacer(Modifier.height(14.dp))
                    DestinationRow(o.destination, o.mode == Mode.AUDIO, { vm.batchOptions = o.copy(destination = it) })
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(vm: MainViewModel, index: Int, s: BatchSource) {
    val info = s.info
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(10.dp)).clickable(enabled = info != null) { vm.updateSource(index) { it.copy(expanded = !it.expanded) } }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Mono(if (info?.isPlaylist == true) "GRID DE FAIXAS IDENTIFICADAS" else "LINK ${index + 1}", color = C.Muted, style = T.labelMd.copy(letterSpacing = 1.sp))
            Text(
                info?.title ?: s.url, style = T.bodyMd.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (s.error != null) Text(s.error, style = T.bodySm, color = C.Error, maxLines = 3)
        }
        Spacer(Modifier.width(8.dp))
        when {
            s.loading -> CircularProgressIndicator(Modifier.size(20.dp), color = C.Gold, strokeWidth = 2.dp)
            info != null -> {
                Mono("${s.selected.size}/${s.total} no visor", color = C.Text, style = T.labelMd, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(C.CardHighest).padding(horizontal = 8.dp, vertical = 4.dp))
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.ExpandMore, null, Modifier.size(22.dp), tint = C.Muted)
            }
        }
    }
}

@Composable
private fun TrackRow(title: String, artist: String?, duration: Long?, thumb: String?, on: Boolean, ready: Boolean, size: Long?, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Card).border(1.dp, if (on) Color(0x4DFACC15) else C.Border, RoundedCornerShape(10.dp)).clickable(onClick = onToggle).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(46.dp).background(if (on) C.Gold else Color.Transparent))
        Spacer(Modifier.width(8.dp))
        CheckSubaru(on, { onToggle() })
        Spacer(Modifier.width(10.dp))
        Thumb(thumb, Modifier.size(54.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.title.copy(fontSize = 16.sp, lineHeight = 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Mono(artist ?: "", color = C.Muted, style = T.labelMd)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Mono(if (on && ready) "✓ Pronto" else "—", color = if (on) C.Gold else C.Dim, style = T.labelMd)
            Mono(listOfNotNull(duration?.let { formatDuration(it) }, size?.let { formatBytes(it) }).joinToString(" • "), color = C.Muted, style = T.labelSm)
        }
    }
}

@Composable
private fun DirectiveRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x0DFFFFFF)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = C.BluePale)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bodyMd.copy(fontSize = 15.sp), maxLines = 2)
            Mono(sub, color = C.Muted, style = T.labelSm, maxLines = 2)
        }
        Spacer(Modifier.width(10.dp))
        SwitchSubaru(checked, onChange)
    }
}
