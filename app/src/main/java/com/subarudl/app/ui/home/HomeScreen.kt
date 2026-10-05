package com.subarudl.app.ui.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.MainViewModel
import com.subarudl.app.QueueTab
import com.subarudl.app.Tab
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.Mode
import com.subarudl.app.data.Status
import com.subarudl.app.ui.components.Badge
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.CheckSubaru
import com.subarudl.app.ui.components.DarkBadge
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.HRow
import com.subarudl.app.ui.components.LinkField
import com.subarudl.app.ui.components.ScreenTitle
import com.subarudl.app.ui.components.IconTile
import com.subarudl.app.ui.components.textFieldColors
import com.subarudl.app.ui.components.IconBtn
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.ProgressTrack
import com.subarudl.app.ui.components.SCard
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.components.Seg
import com.subarudl.app.ui.components.Segmented
import com.subarudl.app.ui.components.SiteChip
import com.subarudl.app.ui.components.Thumb
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.Manrope
import com.subarudl.app.ui.theme.T
import com.subarudl.app.util.audioBitrateOptions
import com.subarudl.app.util.audioFormats
import com.subarudl.app.util.expandSelection
import com.subarudl.app.util.formatCount
import com.subarudl.app.util.formatDuration
import com.subarudl.app.util.formatEta
import com.subarudl.app.util.formatSpeed
import com.subarudl.app.util.selectionSpec
import com.subarudl.app.util.tierLabel
import com.subarudl.app.util.videoContainers
import com.subarudl.app.util.videoQualityOptions

@Composable
fun HomeScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val items by vm.manager.items.collectAsState()
    val net by vm.manager.net.collectAsState()
    val engine by vm.engine.collectAsState()
    var showAdvanced by remember { mutableStateOf(false) }
    var showList by rememberSaveable { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current

    val info = vm.homeInfo
    val o = vm.homeOptions
    val active = items.filter { it.active }
    val speed = active.sumOf { it.speed }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- Título e estado ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScreenTitle("Central de Downloads", "Cole o link de um vídeo, música ou perfil e baixe em alta qualidade.")
                HRow(gap = 8.dp) {
                    StatusChip(
                        if (net.online) "Online • ${net.label}" else "Offline", if (net.online) C.CyanPale else C.ErrorStrong,
                    )
                    StatusChip("↓ ${formatSpeed(speed)}", C.Text)
                    StatusChip(if (settings.turbo) "Turbo aria2" else "yt-dlp ${engine.ytdlp ?: "…"}", C.Gold)
                }
            }
        }

        // ---- Cartão do link ----
        item {
            SCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Bolt, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Link do vídeo ou áudio", style = T.title)
                        Text("Detecção rápida em mais de 1.800 sites", style = T.labelMd)
                    }
                }
                Spacer(Modifier.height(14.dp))
                LinkField(vm.homeUrl, { vm.homeUrl = it }, "https://youtu.be/…", onClear = { vm.clearHome() }, onGo = { vm.analyze(vm.homeUrl) })
                Spacer(Modifier.height(12.dp))
                if (vm.homeLoading || (!engine.ready && engine.error == null)) {
                    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = C.Gold, strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(if (vm.homeLoading) "Lendo o link…" else "Preparando as ferramentas (só na primeira vez)…", style = T.labelLg, color = C.Muted)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BtnGhost("Colar", {
                            val text = clipboard.getText()?.text.orEmpty()
                            if (text.contains("http")) vm.analyze(text) else vm.toast("Nenhum link na área de transferência")
                        }, Modifier.weight(0.8f), icon = Icons.Outlined.ContentPaste, height = 52.dp)
                        BtnPrimary("Capturar mídia", { vm.analyze(vm.homeUrl) }, Modifier.weight(1.4f), icon = Icons.Outlined.Download, enabled = vm.homeUrl.isNotBlank())
                    }
                }
                if (engine.error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Erro ao preparar as ferramentas: ${engine.error}", style = T.bodySm, color = C.Error)
                }
                vm.homeError?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = T.bodyMd, color = C.Error)
                    vm.homeHint?.let { h -> Text(h, style = T.bodySm, color = C.Muted, modifier = Modifier.padding(top = 4.dp)) }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("Canais compatíveis")
                Spacer(Modifier.height(8.dp))
                HRow(gap = 8.dp) {
                    SiteChip("YouTube", Icons.Outlined.PlayCircleOutline, C.ErrorStrong)
                    SiteChip("TikTok", Icons.Outlined.MusicNote, C.CyanPale)
                    SiteChip("Instagram", Icons.Outlined.CameraAlt, C.Muted)
                    SiteChip("X", Icons.Outlined.Tag)
                    SiteChip("SoundCloud", Icons.Outlined.Headphones)
                    SiteChip("+1.800 sites", Icons.Outlined.Videocam)
                }
            }
        }

        // ---- Mídia identificada ----
        if (info != null) {
            item { MediaCard(vm, info, o) }
            if (info.isPlaylist) {
                val total = info.entries.size
                val selected = expandSelection(o.playlistItems, total)
                item {
                    SCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ViewList, null, Modifier.size(20.dp), tint = C.BluePale)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Vídeos da playlist", style = T.title)
                                Mono("${selected.size} de $total marcados", color = C.Muted, style = T.labelMd)
                            }
                            Text(if (showList) "Esconder" else "Mostrar", Modifier.clickable { showList = !showList }.padding(6.dp), style = T.labelMd, color = C.Gold)
                        }
                        if (showList) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BtnGhost("Marcar todos", { vm.homeOptions = o.copy(playlistItems = "") }, Modifier.weight(1f), height = 40.dp)
                                BtnGhost("Desmarcar", { vm.homeOptions = o.copy(playlistItems = "0") }, Modifier.weight(1f), height = 40.dp)
                            }
                        }
                    }
                }
                if (showList) {
                    items(info.entries, key = { it.index }) { e ->
                        val on = e.index in selected
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Card).border(1.dp, if (on) Color(0x4DFACC15) else C.Border, RoundedCornerShape(10.dp))
                                .clickable {
                                    val next = if (on) selected - e.index else selected + e.index
                                    vm.homeOptions = o.copy(playlistItems = selectionSpec(next, total))
                                }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckSubaru(on, { v ->
                                val next = if (v) selected + e.index else selected - e.index
                                vm.homeOptions = o.copy(playlistItems = selectionSpec(next, total))
                            })
                            Spacer(Modifier.width(10.dp))
                            Thumb(e.thumb, Modifier.width(84.dp).aspectRatio(16f / 9f))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.title, style = T.bodyMd.copy(fontSize = 14.sp, lineHeight = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Mono(listOfNotNull(e.index.toString().padStart(2, '0'), e.channel, e.duration?.let { formatDuration(it) }).joinToString(" • "), color = C.Muted)
                            }
                        }
                    }
                }
            }
            item { ActionCard(vm, o, onAdvanced = { showAdvanced = true }) }
        }

        // ---- Baixando agora ----
        if (active.isNotEmpty()) {
            item {
                val first = active.first()
                SCard(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(18.dp), tint = C.Gold)
                        Spacer(Modifier.width(8.dp))
                        SectionLabel("Baixando agora", Modifier.weight(1f), color = C.Gold)
                        Mono("${(first.progress * 100).toInt()}%", color = C.Text, style = T.labelLg)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Thumb(first.thumb, Modifier.size(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(first.title, style = T.title.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row {
                                Mono(formatSpeed(first.speed), color = C.CyanPale, style = T.labelMd)
                                Spacer(Modifier.width(12.dp))
                                Mono(first.stage.ifEmpty { formatEta(first.eta) + " restantes" }, color = C.Muted, style = T.labelMd)
                            }
                        }
                        IconBtn(Icons.Outlined.Pause, { vm.manager.pause(first.id) }, size = 40.dp)
                        Spacer(Modifier.width(8.dp))
                        IconBtn(Icons.Filled.Close, { vm.manager.cancel(first.id) }, size = 40.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressTrack(first.progress)
                }
            }
        }
        if (items.any { it.status == Status.WAITING || it.active }) {
            item {
                val n = items.count { it.status == Status.WAITING || it.active }
                Row(
                    Modifier.fillMaxWidth().clickable { vm.tab = Tab.QUEUE; vm.queueTab = QueueTab.ACTIVE }.padding(8.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Mono("Ver todos os $n downloads ativos na Fila", color = C.Text, style = T.labelMd)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ArrowForward, null, Modifier.size(16.dp), tint = C.Text)
                }
            }
        }
    }

    if (showAdvanced && info != null) {
        AdvancedSheet(info, o, info.isPlaylist, { vm.homeOptions = it }, { showAdvanced = false })
    }
}

@Composable
private fun MediaCard(vm: MainViewModel, info: MediaInfo, o: DownloadOptions) {
    val ctx = LocalContext.current
    val set = { n: DownloadOptions -> vm.homeOptions = n }
    SCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Verified, null, Modifier.size(20.dp), tint = C.Gold)
            Spacer(Modifier.width(8.dp))
            SectionLabel("Mídia identificada", Modifier.weight(1f), color = C.Gold)
            Mono(if (info.isPlaylist) "${info.site} • playlist" else "Pronta para conversão", color = C.Muted, style = T.labelMd)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(C.CardHighest)) {
            Thumb(info.thumb, Modifier.fillMaxWidth().aspectRatio(16f / 9f), shape = RoundedCornerShape(10.dp))
            Row(Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val best = info.maxHeight
                if (best > 0) DarkBadge(tierLabel(best))
                info.formats.filter { it.hasVideo }.maxByOrNull { it.fps ?: 0.0 }?.fps?.takeIf { it >= 50 }?.let { DarkBadge("${it.toInt()} FPS", C.Text) }
                if (info.isLive) DarkBadge("AO VIVO", C.ErrorStrong)
            }
            Box(
                Modifier.align(Alignment.Center).size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xCC1D4ED8))
                    .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.pageUrl.ifEmpty { vm.homeInfoUrl })).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.PlayArrow, "Ver no site", Modifier.size(32.dp), tint = Color.White) }
            if (info.duration != null) DarkBadge(formatDuration(info.duration), C.Text, Modifier.align(Alignment.BottomEnd).padding(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(info.title, style = T.headlineMd.copy(fontSize = 22.sp, lineHeight = 28.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Videocam, null, Modifier.size(16.dp), tint = C.BluePale)
            Spacer(Modifier.width(6.dp))
            Text(
                listOfNotNull(info.channel, info.views?.let { formatCount(it) + " views" }).joinToString("  •  "),
                style = T.bodySm.copy(fontSize = 14.sp), color = C.BluePale, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(14.dp))

        Segmented(
            listOf(
                Seg(Mode.VIDEO, "Vídeo ${o.container.uppercase()}", Icons.Outlined.Videocam),
                Seg(Mode.AUDIO, "Áudio ${o.audioFormat.uppercase()}", Icons.Outlined.Headphones),
            ),
            o.mode, { set(o.copy(mode = it, formatId = null)) },
        )
        Spacer(Modifier.height(14.dp))
        SectionLabel(if (o.mode == Mode.VIDEO) "Resolução & taxa de bits" else "Qualidade do áudio")
        Spacer(Modifier.height(8.dp))
        if (o.mode == Mode.VIDEO) {
            val options = videoQualityOptions(info, o.fps30)
            DropField(o.quality.takeIf { q -> options.any { it.value == q } } ?: "best", options, { set(o.copy(quality = it, formatId = null)) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            DropField(o.container, videoContainers, { set(o.copy(container = it)) }, Modifier.fillMaxWidth(), prefix = "Formato: ")
        } else {
            val lossless = o.audioFormat == "wav" || o.audioFormat == "flac"
            val options = audioBitrateOptions(info, lossless)
            DropField(if (lossless) 0 else o.audioBitrate, options, { set(o.copy(audioBitrate = it)) }, Modifier.fillMaxWidth(), enabled = !lossless)
            Spacer(Modifier.height(10.dp))
            DropField(o.audioFormat, audioFormats, { set(o.copy(audioFormat = it)) }, Modifier.fillMaxWidth(), prefix = "Formato: ")
        }
        Spacer(Modifier.height(10.dp))
        AudioTrackSelect(info, o, set)
        if (info.audioLangs.size > 1) Spacer(Modifier.height(10.dp))
        if (o.mode == Mode.VIDEO) SubtitleSelect(info, o, set)
    }
}

@Composable
private fun ActionCard(vm: MainViewModel, o: DownloadOptions, onAdvanced: () -> Unit) {
    val set = { n: DownloadOptions -> vm.homeOptions = n }
    SCard(padding = 16.dp) {
        DestinationRow(o.destination, o.mode == Mode.AUDIO, { set(o.copy(destination = it)) })
        Spacer(Modifier.height(12.dp))
        OrganizeSelect(o.subfolder, o.subfolderCustom, { v, c -> set(o.copy(subfolder = v, subfolderCustom = c)) })
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clickable(onClick = onAdvanced).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, Modifier.size(20.dp), tint = C.Gold)
            Spacer(Modifier.width(8.dp))
            Text("Opções avançadas", style = T.labelLg, color = C.Gold)
        }
        Spacer(Modifier.height(10.dp))
        BtnPrimary("Baixar Agora", { vm.downloadHome(true) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Download)
        Spacer(Modifier.height(10.dp))
        BtnGhost("Adicionar à fila", { vm.downloadHome(false) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.PlaylistAdd)
    }
}

/** Chip de estado (online, velocidade, versão): só informa, não é clicável. */
@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(10.dp)).background(C.Container).border(1.dp, C.Border, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        style = T.labelLg.copy(fontSize = 12.5.sp), color = color, maxLines = 1,
    )
}
