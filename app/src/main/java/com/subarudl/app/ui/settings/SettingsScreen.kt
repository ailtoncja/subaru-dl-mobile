package com.subarudl.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subarudl.app.BuildConfig
import com.subarudl.app.MainViewModel
import com.subarudl.app.Screen
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.Cookies
import com.subarudl.app.data.LogLevel
import com.subarudl.app.data.Mode
import com.subarudl.app.engine.Engine
import com.subarudl.app.service.Notifier
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.components.sideFor
import com.subarudl.app.ui.components.InfoItem
import com.subarudl.app.ui.components.NavItem
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SectionTitle
import com.subarudl.app.ui.components.SwitchItem
import com.subarudl.app.ui.home.DestinationItem
import com.subarudl.app.ui.home.OrganizeField
import com.subarudl.app.util.audioFormats
import com.subarudl.app.util.videoContainers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel, page: String?, snackbar: SnackbarHostState) {
    val back = { if (page != null) vm.settingsPage = null else vm.screen = Screen.HOME }
    when (page) {
        null -> MainSettings(vm, snackbar, back)
        "legendas" -> SubtitlesPage(vm, snackbar, back)
        "nomes" -> NamesPage(vm, snackbar, back)
        "galeria" -> GalleryPage(vm, snackbar, back)
        "rede" -> NetworkPage(vm, snackbar, back)
        "contas" -> AccountsPage(vm, snackbar, back)
        "ferramentas" -> ToolsPage(vm, snackbar, back)
        "logs" -> LogsPage(snackbar, back)
        else -> MainSettings(vm, snackbar, back)
    }
}

// ---------- Moldura das páginas ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(
    title: String,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Em tablet a lista fica numa coluna central e a barra de cima alinha com ela
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = sideFor(maxWidth)
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
                    actions = actions,
                    windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = side, right = side)),
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().imePadding(),
                state = listState,
                contentPadding = PaddingValues(start = side, end = side, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
                content = content,
            )
        }
    }
}

/** Campo de texto de um ajuste: mostra o que a pessoa digita e guarda quando o valor faz sentido. */
@Composable
private fun SettingField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    sub: String? = null,
    single: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
    mono: Boolean = false,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        text, { text = it; onCommit(it) }, Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
        singleLine = single, minLines = if (single) 1 else 3, label = { Text(label) },
        supportingText = sub?.let { { Text(it) } },
        // Campo técnico: teclado sem sugestões, para "youtube:" não virar "YouTube:"
        keyboardOptions = KeyboardOptions(
            keyboardType = if (mono && keyboard == KeyboardType.Text) KeyboardType.Uri else keyboard,
            capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
        ),
        textStyle = if (mono) MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Opt<T>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().selectable(o.value == selected, role = Role.RadioButton) { onSelect(o.value); onDismiss() }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(o.value == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(o.label, style = MaterialTheme.typography.bodyLarge)
                            if (o.sub != null) Text(o.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Fechar") } },
    )
}

private val qualityChoices = listOf(
    Opt("best", "A melhor que houver"), Opt("2160", "2160p (4K)"), Opt("1440", "1440p (2K)"), Opt("1080", "1080p (Full HD)"),
    Opt("720", "720p (HD)"), Opt("480", "480p"), Opt("360", "360p"),
)
private val bitrateChoices = listOf(
    Opt(320, "320 kbps", "A melhor qualidade de MP3"), Opt(256, "256 kbps"), Opt(192, "192 kbps"), Opt(128, "128 kbps", "Arquivos menores"), Opt(96, "96 kbps"),
)

// ---------- Página principal ----------

@Composable
private fun MainSettings(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val engine by vm.engine.collectAsState()
    val ctx = LocalContext.current
    var dialog by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Page("Ajustes", snackbar, onBack) {
        item { SectionTitle("Downloads") }
        item { NavItem("Qualidade do vídeo", qualityChoices.firstOrNull { it.value == s.quality }?.label ?: "${s.quality}p", Icons.Outlined.Hd, chevron = false) { dialog = "quality" } }
        item { NavItem("Qualidade do áudio", "${s.audioBitrate} kbps", Icons.Outlined.Equalizer, chevron = false) { dialog = "bitrate" } }
        item {
            NavItem(
                "Formatos",
                "Vídeo em ${s.container.uppercase()} · áudio em ${s.audioFormat.uppercase()} · começa em ${if (s.defaultMode == Mode.AUDIO) "Áudio" else "Vídeo"}",
                Icons.Outlined.SwapVert, chevron = false,
            ) { dialog = "format" }
        }
        item { SwitchItem("Ler o link copiado ao abrir", "Se houver um link copiado quando você abre o app, ele já é lido", s.clipboardCapture, { v -> vm.update { it.copy(clipboardCapture = v) } }, Icons.Outlined.ContentPaste) }

        item { SectionTitle("Pastas") }
        item { DestinationItem(s.videoDest, false, { u -> vm.update { it.copy(videoDest = u) } }, title = "Vídeos") }
        item { DestinationItem(s.audioDest, true, { u -> vm.update { it.copy(audioDest = u) } }, title = "Músicas") }
        item { DestinationItem(s.galleryDest, false, { u -> vm.update { it.copy(galleryDest = u) } }, gallery = true, title = "Imagens") }

        item { SectionTitle("Rede") }
        item { SwitchItem("Só no Wi-Fi", "Não usa os dados móveis para baixar", s.wifiOnly, { v -> vm.update { it.copy(wifiOnly = v) } }, Icons.Outlined.Wifi) }
        item {
            ListItem(
                headlineContent = { Text("Downloads ao mesmo tempo") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        (1..4).forEach { n ->
                            SegmentedButton(
                                selected = s.concurrent == n, onClick = { vm.update { it.copy(concurrent = n) } },
                                shape = SegmentedButtonDefaults.itemShape(n - 1, 4),
                            ) { Text(n.toString()) }
                        }
                    }
                },
                leadingContent = { Icon(Icons.Outlined.Lan, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        item { SwitchItem("Turbo", "Baixa por várias conexões ao mesmo tempo (aria2); ajuda em sites lentos", s.turbo, { v -> vm.update { it.copy(turbo = v) } }, Icons.Outlined.Speed) }

        item { SectionTitle("Avisos") }
        item { SwitchItem("Avisar quando terminar", "Notificação com o resultado de cada download", s.notifyDone, { v -> vm.update { it.copy(notifyDone = v) } }, Icons.Outlined.Notifications) }
        item {
            SwitchItem(
                "Som do motor Boxer", "Ronco de motor quando um download termina bem",
                s.soundDone, { v -> vm.update { it.copy(soundDone = v) }; if (v) Notifier.previewBoxer(ctx) }, Icons.AutoMirrored.Outlined.VolumeUp,
            )
        }

        item { SectionTitle("Mais") }
        item { NavItem("Legendas e acabamento", "Idiomas, formato da legenda, capa e metadados", Icons.Outlined.ClosedCaption) { vm.settingsPage = "legendas" } }
        item { NavItem("Pastas e nomes", "Subpastas e o nome dos arquivos", Icons.Outlined.DriveFileRenameOutline) { vm.settingsPage = "nomes" } }
        item { NavItem("Imagens (gallery-dl)", "Organização, pausas e o que já foi baixado", Icons.Outlined.PhotoLibrary) { vm.settingsPage = "galeria" } }
        item { NavItem("Rede avançada", "Proxy, limite de velocidade e argumentos extras", Icons.Outlined.Lan) { vm.settingsPage = "rede" } }
        item { NavItem("Contas e cookies", "Entrar em sites para vídeos privados e perfis", Icons.Outlined.AccountCircle) { vm.settingsPage = "contas" } }
        item { NavItem("Ferramentas e atualizações", "yt-dlp, gallery-dl, ffmpeg e aria2", Icons.Outlined.Build) { vm.settingsPage = "ferramentas" } }
        item { NavItem("Registro", "Tudo o que as ferramentas estão fazendo", Icons.AutoMirrored.Outlined.Article) { vm.settingsPage = "logs" } }

        item { SectionTitle("Sobre") }
        item {
            InfoItem(
                "Subaru DL ${BuildConfig.VERSION_NAME}",
                "yt-dlp ${engine.ytdlp ?: "…"} · " + when {
                    engine.ready -> "ferramentas prontas"
                    engine.error != null -> "erro nas ferramentas"
                    else -> "preparando as ferramentas"
                },
                Icons.Outlined.Info,
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Restaurar as configurações", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Volta tudo ao padrão; a lista, as contas e os arquivos ficam") },
                leadingContent = { Icon(Icons.Outlined.RestartAlt, null, tint = MaterialTheme.colorScheme.error) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.selectable(false) { confirmReset = true },
            )
        }
    }

    when (dialog) {
        "quality" -> ChoiceDialog("Qualidade do vídeo", qualityChoices, s.quality, { v -> vm.update { it.copy(quality = v) } }, { dialog = null })
        "bitrate" -> ChoiceDialog("Qualidade do áudio", bitrateChoices, s.audioBitrate, { v -> vm.update { it.copy(audioBitrate = v) } }, { dialog = null })
        "format" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Formatos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropField("Vídeo", s.container, videoContainers, { v -> vm.update { it.copy(container = v) } }, Modifier.fillMaxWidth())
                    DropField("Áudio", s.audioFormat, audioFormats, { v -> vm.update { it.copy(audioFormat = v) } }, Modifier.fillMaxWidth())
                    DropField("Ao abrir, começar em", s.defaultMode, listOf(Opt(Mode.VIDEO, "Vídeo"), Opt(Mode.AUDIO, "Áudio")), { v -> vm.update { it.copy(defaultMode = v) } }, Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton({ dialog = null }) { Text("Pronto") } },
        )
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        icon = { Icon(Icons.Outlined.RestartAlt, null) },
        title = { Text("Restaurar as configurações?") },
        text = { Text("Todas as configurações voltam ao padrão. A lista de downloads, as contas e os arquivos baixados não mudam.") },
        confirmButton = { TextButton({ vm.settingsReset(); confirmReset = false; vm.toast("Configurações restauradas") }) { Text("Restaurar") } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancelar") } },
    )
}

// ---------- Subpáginas ----------

@Composable
private fun SubtitlesPage(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    Page("Legendas e acabamento", snackbar, onBack) {
        item { SectionTitle("Legendas") }
        item { SwitchItem("Baixar legendas", "Nos vídeos que tiverem legenda nos idiomas abaixo", s.subs.enabled, { v -> vm.update { it.copy(subs = it.subs.copy(enabled = v)) } }) }
        item { SwitchItem("Usar a legenda automática", "Quando o vídeo não tiver uma feita pelo autor", s.subs.auto, { v -> vm.update { it.copy(subs = it.subs.copy(auto = v)) } }) }
        item {
            SwitchItem(
                "Dentro do vídeo",
                if (s.subs.embed) "A legenda vai gravada no próprio vídeo" else "A legenda fica num arquivo .${s.subs.format} ao lado do vídeo",
                s.subs.embed, { v -> vm.update { it.copy(subs = it.subs.copy(embed = v)) } },
            )
        }
        item {
            SettingField(
                "Idiomas (códigos separados por vírgula)", s.subs.langs.joinToString(","),
                { v -> vm.update { it.copy(subs = it.subs.copy(langs = v.split(",").map { x -> x.trim() }.filter { x -> x.isNotEmpty() })) } },
                "Ex.: pt,pt-BR,en",
            )
        }
        item {
            DropField(
                "Formato da legenda", s.subs.format, listOf("srt", "vtt", "ass").map { Opt(it, it.uppercase()) },
                { v -> vm.update { it.copy(subs = it.subs.copy(format = v)) } }, Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
            )
        }
        item { SectionTitle("Acabamento") }
        item { SwitchItem("Gravar título, canal e data no arquivo", null, s.embedMetadata, { v -> vm.update { it.copy(embedMetadata = v) } }) }
        item { SwitchItem("Embutir a capa", "A miniatura vira a capa do arquivo", s.embedThumb, { v -> vm.update { it.copy(embedThumb = v) } }) }
        item { SwitchItem("Salvar a miniatura separada", "Um .jpg ao lado do arquivo", s.saveThumb, { v -> vm.update { it.copy(saveThumb = v) } }) }
    }
}

@Composable
private fun NamesPage(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    Page("Pastas e nomes", snackbar, onBack) {
        item { SectionTitle("Pastas") }
        item { OrganizeField(s.subfolder, s.subfolderCustom, { v, c -> vm.update { it.copy(subfolder = v, subfolderCustom = c) } }, Modifier.padding(horizontal = Gutter, vertical = 6.dp)) }
        item { SwitchItem("Numerar os itens de playlists", "01, 02, 03… no começo do nome", s.numbering, { v -> vm.update { it.copy(numbering = v) } }) }
        item { SectionTitle("Nome do arquivo") }
        item {
            DropField(
                "Nome", s.nameMode,
                listOf(Opt("title", "Título"), Opt("title-quality", "Título e qualidade"), Opt("playlist-title-quality", "Número, título e qualidade (playlists)"), Opt("custom", "Personalizado…")),
                { v -> vm.update { it.copy(nameMode = v) } }, Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
            )
        }
        if (s.nameMode == "custom") item { SettingField("Modelo do nome (yt-dlp)", s.nameCustom, { v -> vm.update { it.copy(nameCustom = v) } }, "Ex.: %(uploader)s - %(title)s", mono = true) }
        item { SwitchItem("Nomes simples", "Só letras, números e _ (sem acentos nem espaços)", s.safeNames, { v -> vm.update { it.copy(safeNames = v) } }) }
    }
}

@Composable
private fun GalleryPage(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    Page("Imagens (gallery-dl)", snackbar, onBack) {
        item { DestinationItem(s.galleryDest, false, { u -> vm.update { it.copy(galleryDest = u) } }, gallery = true) }
        item {
            DropField(
                "Organizar em pastas", s.galleryOrganize,
                listOf(Opt("flat", "Sem subpastas"), Opt("site", "Uma pasta por site"), Opt("profile", "Uma pasta por perfil"), Opt("default", "Site e depois perfil"), Opt("custom", "Personalizado…")),
                { v -> vm.update { it.copy(galleryOrganize = v) } }, Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
            )
        }
        if (s.galleryOrganize == "custom") item { SettingField("Pastas separadas por /", s.galleryCustom, { v -> vm.update { it.copy(galleryCustom = v) } }, "Ex.: {category}/{user}", mono = true) }
        item { SwitchItem("Pular o que já baixei", "Lembra cada arquivo baixado e não baixa de novo", s.gallerySkipDone, { v -> vm.update { it.copy(gallerySkipDone = v) } }) }
        item {
            SettingField(
                "Pausa entre arquivos (segundos)", if (s.gallerySleep == 0.0) "" else s.gallerySleep.toString().replace('.', ','),
                { v -> vm.update { it.copy(gallerySleep = v.replace(',', '.').toDoubleOrNull() ?: 0.0) } },
                "Ajuda quando o site limita os pedidos (erro 429)", keyboard = KeyboardType.Decimal,
            )
        }
    }
}

@Composable
private fun NetworkPage(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    Page("Rede avançada", snackbar, onBack) {
        item { SettingField("Limite de velocidade", s.rateLimit, { v -> vm.update { it.copy(rateLimit = v.trim()) } }, "Ex.: 2M ou 500K. Vazio = sem limite") }
        item {
            SettingField(
                "Tentativas quando falha", s.retries.toString(),
                { v -> v.toIntOrNull()?.let { n -> vm.update { it.copy(retries = n.coerceIn(0, 20)) } } }, "De 0 a 20", keyboard = KeyboardType.Number,
            )
        }
        item { SettingField("Proxy", s.proxy, { v -> vm.update { it.copy(proxy = v.trim()) } }, "Ex.: http://usuario:senha@host:8080 ou socks5://host:1080", keyboard = KeyboardType.Uri) }
        item { SettingField("Argumentos de extrator", s.extractorArgs, { v -> vm.update { it.copy(extractorArgs = v) } }, "Um por linha. Ex.: youtube:player_client=tv", single = false, mono = true) }
        item { SwitchItem("Usar o player.js mais novo do YouTube", "Pode resolver falhas de assinatura", s.playerJsActual, { v -> vm.update { it.copy(playerJsActual = v) } }) }
        item { SettingField("Argumentos extras do yt-dlp", s.extraArgs, { v -> vm.update { it.copy(extraArgs = v) } }, "Avançado. Ex.: --geo-bypass --sleep-requests 1", mono = true) }
    }
}

@Composable
private fun ToolsPage(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val engine by vm.engine.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    Page("Ferramentas e atualizações", snackbar, onBack) {
        item { InfoItem("yt-dlp", "Baixa vídeos e áudios de mais de 1.800 sites", trailing = { Version(engine.ytdlp ?: "—") }) }
        item { InfoItem("gallery-dl", "Baixa perfis, álbuns e galerias de imagens", trailing = { Version(engine.gallerydl ?: "—") }) }
        item { InfoItem("ffmpeg", "Junta vídeo e áudio, converte e embute capas", trailing = { Version("embutido") }) }
        item { InfoItem("aria2", "O Turbo: baixa por várias conexões", trailing = { Version("embutido") }) }
        item { InfoItem("QuickJS", "Resolve o JavaScript do YouTube", trailing = { Version("2025-04-26") }) }
        item { InfoItem("Python", "Roda o yt-dlp e o gallery-dl", trailing = { Version("3.12") }) }
        item {
            Column(Modifier.padding(horizontal = Gutter, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ scope.launch { msg = withContext(Dispatchers.IO) { Engine.updateYtdlp(ctx) } } }, Modifier.fillMaxWidth(), enabled = !engine.updating) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Atualizar o yt-dlp")
                }
                FilledTonalButton({ scope.launch { msg = withContext(Dispatchers.IO) { Engine.updateGalleryDl(ctx) } } }, Modifier.fillMaxWidth(), enabled = !engine.updating) {
                    Text("Atualizar o gallery-dl")
                }
                if (engine.updating) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Atualizando…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                msg?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            DropField(
                "Versão do yt-dlp", s.updateChannel,
                listOf(Opt("stable", "Estável (recomendada)"), Opt("nightly", "Nightly (correções mais cedo)"), Opt("master", "Master")),
                { v -> vm.update { it.copy(updateChannel = v) } }, Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
            )
        }
        item { SwitchItem("Atualizar o yt-dlp sozinho", "Confere a cada 12 horas, quando o app abre", s.autoUpdateTools, { v -> vm.update { it.copy(autoUpdateTools = v) } }) }
    }
}

@Composable
private fun Version(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------- Contas e cookies ----------

@Composable
private fun AccountsPage(vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
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
    val scheme = MaterialTheme.colorScheme
    Page("Contas e cookies", snackbar, onBack) {
        item {
            Text(
                "Para baixar vídeos privados, só para membros, com restrição de idade ou perfis que pedem login, entre na sua conta aqui. Os cookies ficam só neste celular.",
                Modifier.padding(horizontal = Gutter, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
            )
        }
        item {
            ListItem(
                headlineContent = { Text(if (status.exists) "${status.lines} cookies de ${status.domains.size} sites" else "Nenhuma conta conectada") },
                supportingContent = if (status.domains.isNotEmpty()) ({ Text(status.domains.take(8).joinToString(", ") + if (status.domains.size > 8) "…" else "", maxLines = 3) }) else null,
                leadingContent = { Icon(if (status.exists) Icons.Outlined.CheckCircle else Icons.Outlined.AccountCircle, null, tint = if (status.exists) scheme.tertiary else scheme.onSurfaceVariant) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        item { SwitchItem("Usar os cookies nos downloads", null, s.cookiesEnabled, { v -> vm.update { it.copy(cookiesEnabled = v) } }) }
        item {
            Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ login = true }, Modifier.fillMaxWidth()) { Text("Entrar em um site") }
                FilledTonalButton({ receive = true }, Modifier.fillMaxWidth()) { Text("Receber do PC pelo Wi-Fi") }
                Hint("Usa o Subaru DL no computador? Ele tira os cookies do navegador e manda para cá com um código.")
                FilledTonalButton({ importer.launch(arrayOf("text/plain", "text/*", "application/octet-stream")) }, Modifier.fillMaxWidth()) { Text("Importar um cookies.txt") }
                Hint("Também dá para abrir ou compartilhar o arquivo com o Subaru DL, vindo do WhatsApp, Telegram, Drive ou cabo USB.")
                Spacer(Modifier.height(4.dp))
                TextButton({ Cookies.clear(ctx); vm.toast("Cookies apagados") }, Modifier.fillMaxWidth(), enabled = status.exists) {
                    Text("Desconectar tudo", color = if (status.exists) scheme.error else scheme.onSurface.copy(alpha = 0.38f))
                }
            }
        }
    }
    if (login) LoginDialog(onDone = { n -> login = false; vm.toast(if (n > 0) "Conta conectada ($n cookies)" else "Nenhum cookie foi guardado") })
    if (receive) ReceiveCookiesSheet(
        onDone = { r -> receive = false; vm.toast("${r.cookies} cookies de ${r.sites} sites recebidos do PC") },
        onDismiss = { receive = false },
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------- Registro ----------

@Composable
private fun LogsPage(snackbar: SnackbarHostState, onBack: () -> Unit) {
    val lines by AppLog.lines.collectAsState()
    val ctx = LocalContext.current
    val state = rememberLazyListState()
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) state.scrollToItem(lines.lastIndex) }
    Page(
        "Registro", snackbar, onBack,
        actions = {
            IconButton({
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("registro", AppLog.asText()))
            }) { Icon(Icons.Outlined.ContentCopy, "Copiar o registro") }
            IconButton({ AppLog.clear() }) { Icon(Icons.Outlined.DeleteOutline, "Limpar o registro") }
        },
        listState = state,
    ) {
        if (lines.isEmpty()) item {
            Text("Nada registrado ainda.", Modifier.padding(Gutter), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        items(lines) { l ->
            Text(
                "${fmt.format(Date(l.time))}  ${l.text}",
                Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = when (l.level) { LogLevel.ERROR -> scheme.error; LogLevel.WARN -> scheme.primary; LogLevel.CMD -> scheme.secondary; else -> scheme.onSurfaceVariant },
            )
        }
    }
}
