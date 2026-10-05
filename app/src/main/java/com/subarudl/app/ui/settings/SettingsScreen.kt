package com.subarudl.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subarudl.app.BuildConfig
import com.subarudl.app.MainViewModel
import com.subarudl.app.R
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.Cookies
import com.subarudl.app.data.LogLevel
import com.subarudl.app.data.Mode
import com.subarudl.app.data.Status
import com.subarudl.app.data.Storage
import com.subarudl.app.engine.Engine
import com.subarudl.app.ui.components.Badge
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SCard
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.components.SwitchSubaru
import com.subarudl.app.ui.home.DestinationRow
import com.subarudl.app.ui.home.OrganizeSelect
import com.subarudl.app.ui.home.fieldColors
import com.subarudl.app.ui.home.rememberFolderPicker
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T
import com.subarudl.app.util.audioFormats
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.tierLabel
import com.subarudl.app.util.videoContainers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel, page: String?, onPage: (String?) -> Unit) {
    when (page) {
        null -> MainSettings(vm, onPage)
        "logs" -> LogsPage(onBack = { onPage(null) })
        else -> SubPage(vm, page, onBack = { onPage(null) })
    }
}

// ---------- Peças ----------

@Composable
private fun GroupTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = C.BluePale)
        Spacer(Modifier.width(8.dp))
        Text(title, style = T.headlineSm.copy(fontSize = 21.sp))
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Card).border(1.dp, C.Border, RoundedCornerShape(12.dp)),
    ) { content() }
}

@Composable
private fun RowItem(icon: ImageVector, title: String, sub: String?, divider: Boolean = true, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = C.BluePale)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = T.title.copy(fontSize = 16.sp), maxLines = 2)
                if (sub != null) Mono(sub, color = C.Muted, style = T.labelSm, maxLines = 3)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
    }
}

@Composable
private fun ValueChip(text: String, gold: Boolean = false) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(6.dp)).background(if (gold) Color(0xFFEEC200) else Color(0x14FFFFFF)).padding(horizontal = 10.dp, vertical = 6.dp),
        style = T.labelMd.copy(fontWeight = FontWeight.SemiBold), color = if (gold) C.OnGold else C.CyanPale, maxLines = 1,
    )
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Opt<T>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = C.CardHigh,
        title = { Text(title, style = com.subarudl.app.ui.theme.T.headlineSm) },
        text = {
            Column {
                options.forEach { o ->
                    Row(Modifier.fillMaxWidth().clickable { onSelect(o.value); onDismiss() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(o.value == selected, { onSelect(o.value); onDismiss() }, colors = RadioButtonDefaults.colors(selectedColor = C.Gold, unselectedColor = C.Muted))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(o.label, style = com.subarudl.app.ui.theme.T.labelLg)
                            if (o.sub != null) Text(o.sub, style = com.subarudl.app.ui.theme.T.labelSm, color = C.Muted)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Fechar", color = C.Gold) } },
    )
}

private val qualityChoices = listOf("best" to "Melhor disponível", "2160" to "2160p 4K UHD", "1440" to "1440p 2K QHD", "1080" to "1080p Full HD", "720" to "720p HD", "480" to "480p SD", "360" to "360p")
private val bitrateChoices = listOf(320 to "320 kbps (Alta fidelidade)", 256 to "256 kbps", 192 to "192 kbps", 128 to "128 kbps", 96 to "96 kbps")

// ---------- Tela principal ----------

@Composable
private fun MainSettings(vm: MainViewModel, onPage: (String?) -> Unit) {
    val s by vm.settings.collectAsState()
    val engine by vm.engine.collectAsState()
    val items by vm.manager.items.collectAsState()
    val ctx = LocalContext.current
    var dialog by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val pickVideo = rememberFolderPicker { u -> vm.update { it.copy(videoDest = u, audioDest = it.audioDest ?: u) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ---- Cartão do app ----
        item {
            SCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Image(painterResource(R.drawable.ic_logo), null, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, C.Border, RoundedCornerShape(12.dp)))
                        Box(Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(if (engine.ready) C.Gold else if (engine.error != null) C.ErrorStrong else C.Dim))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Subaru DL Mobile", style = T.headlineSm.copy(fontSize = 21.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Badge("Edição Especial", Modifier.padding(vertical = 4.dp))
                        Mono("v${BuildConfig.VERSION_NAME} • yt-dlp ${engine.ytdlp ?: "…"}", color = C.Muted, style = T.labelMd)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SectionLabel("Motor", color = C.CyanPale)
                        Text(if (engine.ready) "Pronto" else if (engine.error != null) "Erro" else "Preparando", style = T.headlineSm.copy(fontSize = 20.sp), color = C.CyanPale)
                    }
                }
            }
        }

        // ---- Downloads & Qualidade ----
        item { GroupTitle(Icons.Outlined.Download, "Downloads & Qualidade") }
        item {
            Group {
                RowItem(Icons.Outlined.Hd, "Resolução de Vídeo", "Prioridade máxima ao indexar streams", onClick = { dialog = "quality" }) {
                    ValueChip(if (s.quality == "best") "Melhor" else qualityChoices.firstOrNull { it.first == s.quality }?.second ?: s.quality); Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted)
                }
                RowItem(Icons.Outlined.Equalizer, "Qualidade de Áudio", "Taxa de bits para MP3/M4A/OPUS", onClick = { dialog = "bitrate" }) {
                    ValueChip("${s.audioBitrate} kbps")
                }
                RowItem(Icons.Outlined.SwapVert, "Formato Padrão", "Conversor multithread nativo", onClick = { dialog = "format" }) {
                    ValueChip("${s.container.uppercase()} / ${s.audioFormat.uppercase()}", gold = true)
                }
                RowItem(Icons.Outlined.ContentPaste, "Smart Clipboard Capture", "Analisa o link copiado ao abrir o app", divider = false) {
                    SwitchSubaru(s.clipboardCapture, { v -> vm.update { it.copy(clipboardCapture = v) } })
                }
            }
        }

        // ---- Armazenamento & Rede ----
        item { GroupTitle(Icons.Outlined.FolderSpecial, "Armazenamento & Rede") }
        item {
            Group {
                Column {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderSpecial, null, Modifier.size(22.dp), tint = C.BluePale) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Diretório de Chegada", style = T.title.copy(fontSize = 16.sp))
                            Mono(Storage.destinationLabel(ctx, s.videoDest, false), color = C.Muted, style = T.labelSm)
                        }
                        Text("Mudar Pasta", Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x14FFFFFF)).clickable(onClick = pickVideo).padding(horizontal = 12.dp, vertical = 8.dp), style = T.labelMd, color = C.Text)
                    }
                    val space = remember(items.size) { Storage.space(ctx) }
                    val used = Storage.appUsage(items)
                    Column(Modifier.padding(horizontal = 14.dp)) {
                        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x1FFFFFFF))) {
                            val total = space.total.coerceAtLeast(1)
                            Box(Modifier.weight((used.toFloat() / total).coerceIn(0.01f, 1f)).fillMaxSize().background(C.Blue))
                            Box(Modifier.weight((1f - space.free.toFloat() / total - used.toFloat() / total).coerceIn(0.01f, 1f)).fillMaxSize().background(C.Gold))
                            Box(Modifier.weight((space.free.toFloat() / total).coerceIn(0.01f, 1f)).fillMaxSize())
                        }
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Mono("Subaru DL: ${formatBytes(used)}", color = C.Muted, style = T.labelSm, modifier = Modifier.weight(1f))
                            Mono("Livre: ${formatBytes(space.free)}", color = C.Muted, style = T.labelSm)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
                }
                RowItem(Icons.Outlined.Wifi, "Baixar Somente no Wi-Fi", "Proteção contra consumo de dados móveis") {
                    SwitchSubaru(s.wifiOnly, { v -> vm.update { it.copy(wifiOnly = v) } })
                }
                RowItem(Icons.Outlined.Lan, "Downloads Simultâneos", "Carga do boxe de tração") {
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(C.Field).padding(3.dp)) {
                        (1..4).forEach { n ->
                            Text(
                                n.toString(),
                                Modifier.clip(RoundedCornerShape(6.dp)).background(if (s.concurrent == n) C.Blue else Color.Transparent).clickable { vm.update { it.copy(concurrent = n) } }.padding(horizontal = 11.dp, vertical = 7.dp),
                                style = T.labelMd, color = if (s.concurrent == n) Color.White else C.Muted,
                            )
                        }
                    }
                }
                RowItem(Icons.Outlined.Speed, "Turbo Boost WRC", "Aceleração via aria2c e fragmentação paralela", divider = false) {
                    Badge("AWD"); Spacer(Modifier.width(8.dp)); SwitchSubaru(s.turbo, { v -> vm.update { it.copy(turbo = v) } }, gold = true)
                }
            }
        }

        // ---- Aparência & Som ----
        item { GroupTitle(Icons.Outlined.AutoFixHigh, "Aparência & Som de Telemetria") }
        item {
            Group {
                RowItem(Icons.Outlined.DarkMode, "Tema da Interface", "Rally Precision Dark (Azul WRC & Ouro)") { Badge("Padrão WRC") }
                RowItem(Icons.Outlined.Notifications, "Notificação de Conclusão", "Aviso com o resultado de cada download") {
                    SwitchSubaru(s.notifyDone, { v -> vm.update { it.copy(notifyDone = v) } })
                }
                RowItem(Icons.Outlined.VolumeUp, "Efeito Sonoro Boxer WRC", "Som ao finalizar um download", divider = false) {
                    SwitchSubaru(s.soundDone, { v -> vm.update { it.copy(soundDone = v) } }, gold = true)
                }
            }
        }

        // ---- Mais ajustes ----
        item { GroupTitle(Icons.Outlined.Build, "Mais ajustes") }
        item {
            Group {
                RowItem(Icons.Outlined.ClosedCaption, "Legendas e pós-processamento", "Idiomas, formato, capa e metadados", onClick = { onPage("legendas") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
                RowItem(Icons.Outlined.FolderSpecial, "Pastas e nomes", "Organização em subpastas e modelo do nome", onClick = { onPage("nomes") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
                RowItem(Icons.Outlined.PhotoLibrary, "Galeria (gallery-dl)", "Organização, pausas e arquivos já baixados", onClick = { onPage("galeria") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
                RowItem(Icons.Outlined.Lan, "Rede avançada", "Proxy, limite de velocidade e argumentos extras", onClick = { onPage("rede") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
                RowItem(Icons.Outlined.AccountCircle, "Contas e cookies", "Entrar em sites para vídeos privados e perfis", onClick = { onPage("contas") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
                RowItem(Icons.Outlined.Build, "Dependências e atualizações", "yt-dlp, gallery-dl, ffmpeg e QuickJS", onClick = { onPage("ferramentas") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
                RowItem(Icons.Outlined.Terminal, "Logs", "Tudo o que o motor está fazendo", divider = false, onClick = { onPage("logs") }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = C.Muted) }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x1AF87171)).border(1.dp, Color(0x33F87171), RoundedCornerShape(10.dp)).clickable { confirmReset = true }.padding(16.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(20.dp), tint = C.Error)
                Spacer(Modifier.width(8.dp))
                Text("Restaurar Parâmetros de Fábrica", style = T.labelLg, color = C.Error)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Mono("SUBARU DL • BOXER MOTORSPORT TELEMETRY", color = C.Gold, style = T.labelSm)
                Mono("Build ${BuildConfig.VERSION_NAME} • yt-dlp + gallery-dl + ffmpeg", color = C.Dim, style = T.labelSm)
            }
        }
    }

    when (dialog) {
        "quality" -> ChoiceDialog("Resolução de vídeo", qualityChoices.map { Opt(it.first, it.second) }, s.quality, { v -> vm.update { it.copy(quality = v) } }, { dialog = null })
        "bitrate" -> ChoiceDialog("Qualidade do áudio", bitrateChoices.map { Opt(it.first, it.second) }, s.audioBitrate, { v -> vm.update { it.copy(audioBitrate = v) } }, { dialog = null })
        "format" -> AlertDialog(
            onDismissRequest = { dialog = null }, containerColor = C.CardHigh,
            title = { Text("Formato padrão", style = T.headlineSm) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropField(s.container, videoContainers, { v -> vm.update { it.copy(container = v) } }, Modifier.fillMaxWidth(), prefix = "Vídeo: ")
                    DropField(s.audioFormat, audioFormats, { v -> vm.update { it.copy(audioFormat = v) } }, Modifier.fillMaxWidth(), prefix = "Áudio: ")
                    DropField(s.defaultMode, listOf(Opt(Mode.VIDEO, "Começar em Vídeo"), Opt(Mode.AUDIO, "Começar em Áudio")), { v -> vm.update { it.copy(defaultMode = v) } }, Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton({ dialog = null }) { Text("Pronto", color = C.Gold) } },
        )
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, containerColor = C.CardHigh,
        title = { Text("Restaurar parâmetros de fábrica?", style = T.headlineSm) },
        text = { Text("Todas as configurações voltam ao padrão. A fila, o histórico, as contas e os arquivos baixados não mudam.", style = T.bodyMd) },
        confirmButton = { TextButton({ vm.settingsReset(); confirmReset = false; vm.toast("Configurações restauradas") }) { Text("Restaurar", color = C.Error) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancelar", color = C.Muted) } },
    )
}

// ---------- Subpáginas ----------

@Composable
private fun PageFrame(title: String, onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", Modifier.size(22.dp), tint = C.Text)
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = T.headlineMd)
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, sub: String? = null, single: Boolean = true, mono: Boolean = true) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), singleLine = single, minLines = if (single) 1 else 3, label = { Text(label) },
        supportingText = sub?.let { { Text(it) } }, colors = fieldColors(), textStyle = if (mono) T.labelLg else T.bodyMd,
    )
}

@Composable
private fun SubPage(vm: MainViewModel, page: String, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val engine by vm.engine.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickGallery = rememberFolderPicker { u -> vm.update { it.copy(galleryDest = u) } }

    when (page) {
        "legendas" -> PageFrame("Legendas e pós-processamento", onBack) {
            SCard {
                SectionLabel("Legendas")
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Baixar legendas por padrão", "Em vídeos que tiverem a legenda do idioma abaixo", s.subs.enabled) { v -> vm.update { it.copy(subs = it.subs.copy(enabled = v)) } }
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Usar a legenda automática", "Quando o vídeo não tiver legenda feita pelo autor", s.subs.auto) { v -> vm.update { it.copy(subs = it.subs.copy(auto = v)) } }
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Embutir no vídeo", "Desligado salva um arquivo .srt ao lado", s.subs.embed) { v -> vm.update { it.copy(subs = it.subs.copy(embed = v)) } }
                Spacer(Modifier.height(12.dp))
                Field("Idiomas (códigos separados por vírgula)", s.subs.langs.joinToString(","), { v -> vm.update { it.copy(subs = it.subs.copy(langs = v.split(",").map { x -> x.trim() }.filter { x -> x.isNotEmpty() })) } }, "Ex.: pt,pt-BR,en")
                Spacer(Modifier.height(10.dp))
                DropField(s.subs.format, listOf("srt", "vtt", "ass").map { Opt(it, it.uppercase()) }, { v -> vm.update { it.copy(subs = it.subs.copy(format = v)) } }, Modifier.fillMaxWidth(), prefix = "Formato: ")
            }
            SCard {
                SectionLabel("Pós-processamento")
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Gravar título, canal e data no arquivo", null, s.embedMetadata) { v -> vm.update { it.copy(embedMetadata = v) } }
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Embutir a capa", "Miniatura como capa do arquivo", s.embedThumb) { v -> vm.update { it.copy(embedThumb = v) } }
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Salvar a miniatura em separado", "Um .jpg ao lado do arquivo", s.saveThumb) { v -> vm.update { it.copy(saveThumb = v) } }
            }
        }

        "nomes" -> PageFrame("Pastas e nomes", onBack) {
            SCard {
                SectionLabel("Pastas")
                Spacer(Modifier.height(10.dp))
                OrganizeSelect(s.subfolder, s.subfolderCustom, { v, c -> vm.update { it.copy(subfolder = v, subfolderCustom = c) } })
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Numerar os itens de playlists", "Prefixo 01, 02, 03…", s.numbering) { v -> vm.update { it.copy(numbering = v) } }
                Spacer(Modifier.height(12.dp))
                DestinationRow(s.audioDest, true, { u -> vm.update { it.copy(audioDest = u) } })
                Spacer(Modifier.height(8.dp))
                DestinationRow(s.videoDest, false, { u -> vm.update { it.copy(videoDest = u) } })
            }
            SCard {
                SectionLabel("Nome do arquivo")
                Spacer(Modifier.height(10.dp))
                DropField(
                    s.nameMode,
                    listOf(Opt("title", "Título"), Opt("title-quality", "Título-qualidade"), Opt("playlist-title-quality", "Número-título-qualidade (listas)"), Opt("custom", "Personalizado…")),
                    { v -> vm.update { it.copy(nameMode = v) } }, Modifier.fillMaxWidth(),
                )
                if (s.nameMode == "custom") {
                    Spacer(Modifier.height(8.dp))
                    Field("Modelo do nome (yt-dlp)", s.nameCustom, { v -> vm.update { it.copy(nameCustom = v) } }, "Ex.: %(uploader)s - %(title)s")
                }
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Nomes seguros", "Só letras, números e _ (sem acentos ou espaços)", s.safeNames) { v -> vm.update { it.copy(safeNames = v) } }
            }
        }

        "galeria" -> PageFrame("Galeria (gallery-dl)", onBack) {
            SCard {
                DestinationRow(s.galleryDest, false, { u -> vm.update { it.copy(galleryDest = u) } }, gallery = true)
                Spacer(Modifier.height(12.dp))
                DropField(
                    s.galleryOrganize,
                    listOf(Opt("flat", "Sem subpastas"), Opt("site", "Por site"), Opt("profile", "Só o perfil"), Opt("default", "Por site → perfil"), Opt("custom", "Personalizado…")),
                    { v -> vm.update { it.copy(galleryOrganize = v) } }, Modifier.fillMaxWidth(), prefix = "Organizar: ",
                )
                if (s.galleryOrganize == "custom") {
                    Spacer(Modifier.height(8.dp))
                    Field("Pastas separadas por /", s.galleryCustom, { v -> vm.update { it.copy(galleryCustom = v) } }, "Ex.: {category}/{user}")
                }
                Spacer(Modifier.height(12.dp))
                com.subarudl.app.ui.home.SwitchRow("Pular o que já baixei", "Lembra cada arquivo baixado e não baixa de novo", s.gallerySkipDone) { v -> vm.update { it.copy(gallerySkipDone = v) } }
                Spacer(Modifier.height(12.dp))
                Field("Pausa entre arquivos (segundos)", if (s.gallerySleep == 0.0) "" else s.gallerySleep.toString(), { v -> vm.update { it.copy(gallerySleep = v.replace(',', '.').toDoubleOrNull() ?: 0.0) } }, "Ajuda quando o site limita as requisições (erro 429)")
            }
        }

        "rede" -> PageFrame("Rede avançada", onBack) {
            SCard {
                Field("Limite de velocidade", s.rateLimit, { v -> vm.update { it.copy(rateLimit = v) } }, "Ex.: 2M ou 500K. Vazio = sem limite")
                Spacer(Modifier.height(10.dp))
                Field("Tentativas em caso de falha", s.retries.toString(), { v -> vm.update { it.copy(retries = v.toIntOrNull()?.coerceIn(0, 20) ?: 3) } })
                Spacer(Modifier.height(10.dp))
                Field("Proxy", s.proxy, { v -> vm.update { it.copy(proxy = v) } }, "Ex.: http://usuario:senha@host:8080 ou socks5://host:1080")
            }
            SCard {
                Field("Argumentos de extrator", s.extractorArgs, { v -> vm.update { it.copy(extractorArgs = v) } }, "Um por linha. Ex.: youtube:player_client=tv", single = false)
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Usar o player.js mais novo do YouTube", "Pode resolver falhas de assinatura", s.playerJsActual) { v -> vm.update { it.copy(playerJsActual = v) } }
                Spacer(Modifier.height(10.dp))
                Field("Argumentos extras do yt-dlp", s.extraArgs, { v -> vm.update { it.copy(extraArgs = v) } }, "Avançado. Ex.: --geo-bypass --sleep-requests 1")
            }
        }

        "contas" -> AccountsPage(vm, onBack)

        "ferramentas" -> PageFrame("Dependências e atualizações", onBack) {
            SCard {
                ToolRow("yt-dlp", engine.ytdlp ?: "—", "Baixa vídeos e áudios de mais de 1.800 sites")
                ToolRow("gallery-dl", engine.gallerydl ?: "—", "Baixa perfis, álbuns e galerias de imagens")
                ToolRow("ffmpeg", "embutido", "Junta vídeo e áudio, converte e embute capas")
                ToolRow("QuickJS", "2025-04-26", "Resolve o JavaScript do YouTube")
                ToolRow("aria2c", "embutido", "Turbo Boost: download em várias conexões")
                ToolRow("Python", "3.12", "Roda o yt-dlp e o gallery-dl", last = true)
            }
            SCard {
                var msg by remember { mutableStateOf<String?>(null) }
                BtnPrimary(if (engine.updating) "Atualizando…" else "Atualizar yt-dlp", {
                    scope.launch { msg = withContext(Dispatchers.IO) { Engine.updateYtdlp(ctx) } }
                }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Refresh, enabled = !engine.updating)
                Spacer(Modifier.height(10.dp))
                BtnGhost("Atualizar gallery-dl", {
                    scope.launch { msg = withContext(Dispatchers.IO) { Engine.updateGalleryDl(ctx) } }
                }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Refresh, enabled = !engine.updating)
                if (engine.updating) { Spacer(Modifier.height(10.dp)); CircularProgressIndicator(Modifier.size(22.dp), color = C.Gold, strokeWidth = 2.5.dp) }
                msg?.let { Spacer(Modifier.height(10.dp)); Text(it, style = T.bodySm.copy(fontSize = 14.sp), color = C.CyanPale) }
                Spacer(Modifier.height(14.dp))
                DropField(s.updateChannel, listOf(Opt("stable", "Estável (recomendado)"), Opt("nightly", "Nightly (correções mais cedo)"), Opt("master", "Master")), { v -> vm.update { it.copy(updateChannel = v) } }, Modifier.fillMaxWidth(), prefix = "Canal do yt-dlp: ")
                Spacer(Modifier.height(10.dp))
                com.subarudl.app.ui.home.SwitchRow("Atualizar o yt-dlp sozinho", "Confere a cada 12 horas, ao abrir o app", s.autoUpdateTools) { v -> vm.update { it.copy(autoUpdateTools = v) } }
            }
        }
    }
}

@Composable
private fun ToolRow(name: String, version: String, sub: String, last: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = T.title.copy(fontSize = 16.sp))
                Mono(sub, color = C.Muted, style = T.labelSm, maxLines = 2)
            }
            Spacer(Modifier.width(10.dp))
            ValueChip(version)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
    }
}

// ---------- Contas e cookies ----------

@Composable
private fun AccountsPage(vm: MainViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val revision by Cookies.revision.collectAsState()
    val status = remember(revision) { Cookies.status(ctx) }
    var login by remember { mutableStateOf(false) }
    var receive by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val r = Cookies.importFile(ctx, uri)
            vm.toast(if (r.cookies > 0) "${r.cookies} cookies de ${r.sites} sites importados" else "Esse arquivo não parece um cookies.txt")
        }
    }
    PageFrame("Contas e cookies", onBack) {
        SCard {
            Text("Para baixar vídeos privados, só para membros, com restrição de idade ou perfis que pedem login, entre na sua conta aqui. Os cookies ficam só neste aparelho.", style = T.bodySm.copy(fontSize = 14.sp), color = C.Muted)
            Spacer(Modifier.height(14.dp))
            com.subarudl.app.ui.home.SwitchRow("Usar os cookies nos downloads", null, s.cookiesEnabled) { v -> vm.update { it.copy(cookiesEnabled = v) } }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (status.exists) C.Success else C.Dim))
                Spacer(Modifier.width(8.dp))
                Mono(if (status.exists) "${status.lines} cookies de ${status.domains.size} sites" else "Nenhuma conta conectada", color = C.Text, style = T.labelMd)
            }
            if (status.domains.isNotEmpty()) Mono(status.domains.take(8).joinToString(", ") + if (status.domains.size > 8) "…" else "", color = C.Muted, style = T.labelSm, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
            BtnPrimary("Entrar em um site", { login = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.AccountCircle)
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.height(10.dp))
            BtnGhost("Receber do PC pelo Wi-Fi", { receive = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Wifi)
            Text("Já usa o Subaru DL no computador? Ele gera o cookies.txt a partir do navegador e envia para cá em um toque.", style = T.bodySm, color = C.Dim, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            BtnGhost("Importar cookies.txt", { importer.launch(arrayOf("text/plain", "text/*", "application/octet-stream")) }, Modifier.fillMaxWidth())
            Text("Também funciona abrir o arquivo (ou compartilhá-lo) com o Subaru DL, vindo do WhatsApp, Telegram, Drive ou do USB.", style = T.bodySm, color = C.Dim, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            BtnGhost("Desconectar tudo", { Cookies.clear(ctx); vm.toast("Cookies apagados") }, Modifier.fillMaxWidth(), color = C.Error, enabled = status.exists)
        }
    }
    if (login) LoginDialog(onDone = { n -> login = false; if (n > 0) vm.toast("Conta conectada ($n cookies)") else vm.toast("Nenhum cookie foi guardado") })
    if (receive) ReceiveCookiesSheet(
        onDone = { r -> receive = false; vm.toast("${r.cookies} cookies de ${r.sites} sites recebidos do PC") },
        onDismiss = { receive = false },
    )
}

// ---------- Logs ----------

@Composable
private fun LogsPage(onBack: () -> Unit) {
    val lines by AppLog.lines.collectAsState()
    val ctx = LocalContext.current
    val state = rememberLazyListState()
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) state.scrollToItem(lines.lastIndex) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14FFFFFF)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", Modifier.size(22.dp), tint = C.Text)
            }
            Spacer(Modifier.width(12.dp))
            Text("Logs", style = T.headlineMd, modifier = Modifier.weight(1f))
            BtnGhost("Copiar", {
                (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("logs", AppLog.asText()))
            }, height = 38.dp)
            Spacer(Modifier.width(8.dp))
            BtnGhost("Limpar", { AppLog.clear() }, height = 38.dp)
        }
        LazyColumn(state = state, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(C.Field).padding(10.dp)) {
            items(lines) { l ->
                Text(
                    "${fmt.format(Date(l.time))}  ${l.text}",
                    style = T.labelSm.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = when (l.level) { LogLevel.ERROR -> C.ErrorStrong; LogLevel.WARN -> C.Gold; LogLevel.CMD -> C.CyanPale; else -> C.Muted },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
