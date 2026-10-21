package com.subarudl.app.ui.setup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.subarudl.app.MainViewModel
import com.subarudl.app.data.AppSettings
import com.subarudl.app.data.Cookies
import com.subarudl.app.data.Mode
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SectionTitle
import com.subarudl.app.ui.components.SwitchItem
import com.subarudl.app.ui.components.sideFor
import com.subarudl.app.ui.home.ChoiceChips
import com.subarudl.app.ui.home.DestinationItem
import com.subarudl.app.ui.settings.LoginDialog
import com.subarudl.app.ui.settings.ReceiveCookiesSheet

/*
 * Assistente de primeiro uso: três escolhas rápidas (pastas, jeito de baixar, contas) numa tela cheia.
 * Abre sozinho só na primeira vez; "Pular" fica sempre na barra de cima e nada aqui é obrigatório para baixar.
 * Depois, fica em Ajustes → Sobre → Assistente de primeiro uso.
 */

private val Steps = listOf("Pastas", "Jeito de baixar", "Contas")

private val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Eixo horizontal compartilhado do Material, igual ao das telas: a etapa nova vem do lado para onde se avança. */
private fun stepAxis(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    return (slideInHorizontally(tween(350, easing = EnterEasing)) { w -> sign * w / 8 } + fadeIn(tween(200, delayMillis = 80))) togetherWith
        (slideOutHorizontally(tween(200, easing = ExitEasing)) { w -> -sign * w / 8 } + fadeOut(tween(100)))
}

private val qualityOptions = listOf(Opt("best", "A melhor que houver"), Opt("1080", "1080p"), Opt("720", "720p"), Opt("480", "480p"))
private val audioOptions = listOf(Opt("mp3", "MP3"), Opt("m4a", "M4A"), Opt("opus", "Opus"), Opt("flac", "FLAC"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(vm: MainViewModel, snackbar: SnackbarHostState) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = step == Steps.lastIndex
    val s by vm.settings.collectAsState()

    // Voltar do sistema volta uma etapa; na primeira, fecha (o mesmo que Pular)
    BackHandler { if (step > 0) step-- else vm.closeSetup(finished = false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = sideFor(maxWidth)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Preparar o Subaru DL") },
                    navigationIcon = { if (step > 0) IconButton({ step-- }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Etapa anterior") } },
                    actions = { TextButton({ vm.closeSetup(finished = false) }) { Text("Pular") } },
                    windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = side, right = side)),
                )
            },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(start = Gutter + side, end = Gutter + side, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Dá para mudar tudo depois em Ajustes",
                                Modifier.weight(1f).padding(end = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button({ if (last) vm.closeSetup(finished = true) else step++ }) { Text(if (last) "Concluir" else "Continuar") }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
                StepBar(step, Modifier.padding(start = Gutter + side, end = Gutter + side, top = 4.dp, bottom = 8.dp))
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { stepAxis(forward = targetState > initialState) },
                    label = "etapa",
                    modifier = Modifier.weight(1f),
                ) { current ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = side, end = side, bottom = 24.dp)) {
                        when (current) {
                            0 -> foldersStep(vm, s)
                            1 -> modeStep(vm, s)
                            else -> item { AccountsStep(vm) }
                        }
                    }
                }
            }
        }
    }
}

/** As três etapas como faixas: preenchidas até a atual, com o nome de cada uma embaixo. */
@Composable
private fun StepBar(step: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = "Etapa ${step + 1} de ${Steps.size}: ${Steps[step]}" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Steps.forEachIndexed { i, name ->
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(if (i <= step) scheme.primary else scheme.surfaceContainerHighest, RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(6.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (i == step) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Título e explicação de cada etapa. */
private fun LazyListScope.heading(title: String, text: String) {
    item {
        Column(Modifier.padding(horizontal = Gutter, vertical = 16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- 1. Pastas ----------

private fun LazyListScope.foldersStep(vm: MainViewModel, s: AppSettings) {
    heading(
        "Onde os arquivos vão ficar",
        "Cada tipo já tem uma pasta pronta, que aparece na galeria e nos apps de música. Troque só se preferir outro lugar, como o cartão de memória.",
    )
    item { DestinationItem(s.videoDest, false, { u -> vm.update { it.copy(videoDest = u) } }, title = "Vídeos") }
    item { DestinationItem(s.audioDest, true, { u -> vm.update { it.copy(audioDest = u) } }, title = "Músicas e áudios") }
    item { DestinationItem(s.galleryDest, false, { u -> vm.update { it.copy(galleryDest = u) } }, gallery = true, title = "Imagens") }
}

// ---------- 2. Jeito de baixar ----------

private fun LazyListScope.modeStep(vm: MainViewModel, s: AppSettings) {
    heading(
        "Como baixar quando chegar um link",
        "Isso vira o padrão. Cada link ainda mostra as opções antes de baixar, para mudar só naquele.",
    )
    item { SectionTitle("Começar em") }
    item {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Gutter)) {
            listOf(Mode.VIDEO to "Vídeo", Mode.AUDIO to "Só o áudio").forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = s.defaultMode == mode,
                    onClick = { vm.update { it.copy(defaultMode = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    icon = { SegmentedButtonDefaults.Icon(s.defaultMode == mode) { Icon(if (mode == Mode.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie, null, Modifier.size(18.dp)) } },
                ) { Text(label) }
            }
        }
    }
    item { SectionTitle("Qualidade do vídeo", Modifier.padding(top = 8.dp)) }
    item {
        // Uma qualidade fora da lista curta (4K, 2K, 360p) continua valendo; só não aparece marcada aqui
        ChoiceChips(qualityOptions, s.quality, { v -> vm.update { it.copy(quality = v) } })
    }
    item { Hint("No celular, 720p ou 1080p já ficam ótimos e ocupam bem menos espaço. Se o vídeo não tiver essa, vem a mais próxima abaixo.") }
    item { SectionTitle("Formato do áudio", Modifier.padding(top = 8.dp)) }
    item { ChoiceChips(audioOptions, s.audioFormat, { v -> vm.update { it.copy(audioFormat = v) } }) }
    item { Hint("MP3 toca em qualquer aparelho. FLAC guarda o som sem perda, mas ocupa bem mais.") }
    item { Spacer(Modifier.height(8.dp)) }
    item { SwitchItem("Baixar só no Wi-Fi", "Não gasta os dados móveis; nos dados, os downloads esperam o Wi-Fi", s.wifiOnly, { v -> vm.update { it.copy(wifiOnly = v) } }, Icons.Outlined.Wifi) }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        Modifier.padding(start = Gutter, end = Gutter, top = 4.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------- 3. Contas e cookies ----------

@Composable
private fun AccountsStep(vm: MainViewModel) {
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

    Column {
        Column(Modifier.padding(horizontal = Gutter, vertical = 16.dp)) {
            Text("Suas contas nos sites", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(
                "Serve para vídeos com restrição de idade, privados ou só para membros, e para perfis que pedem login. Fora isso, tudo funciona sem conta. Os cookies ficam só neste celular.",
                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
            )
        }
        ListItem(
            headlineContent = { Text(if (status.exists) "Conectado: ${status.lines} cookies de ${status.domains.size} sites" else "Nenhuma conta conectada") },
            supportingContent = if (status.domains.isNotEmpty()) ({ Text(status.domains.take(6).joinToString(", ") + if (status.domains.size > 6) "…" else "", maxLines = 2) }) else null,
            leadingContent = { Icon(if (status.exists) Icons.Outlined.CheckCircle else Icons.Outlined.AccountCircle, null, tint = if (status.exists) scheme.tertiary else scheme.onSurfaceVariant) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        SectionTitle("Escolha um jeito")
        Way(
            Icons.AutoMirrored.Outlined.Login, "Entrar em um site",
            "Abre o site (YouTube, Instagram, X, TikTok…) aqui dentro. Você entra na conta como sempre e o app guarda o login.",
        ) { login = true }
        Way(
            Icons.Outlined.Computer, "Receber do PC pelo Wi-Fi",
            "Usa o Subaru DL no computador? Ele tira o login do navegador e manda para cá com um código.",
        ) { receive = true }
        Way(
            Icons.Outlined.FileOpen, "Importar um cookies.txt",
            "Um arquivo que veio pelo WhatsApp, Telegram, Drive ou cabo USB.",
        ) { importer.launch(arrayOf("text/plain", "text/*", "application/octet-stream")) }
        Hint("Não precisa agora? Toque em Concluir. Isso fica em Ajustes → Contas e cookies, e o aviso de erro mostra o caminho quando um site pedir login.")
    }

    if (login) LoginDialog(onDone = { n -> login = false; vm.toast(if (n > 0) "Conta conectada ($n cookies)" else "Nenhum cookie foi guardado") })
    if (receive) ReceiveCookiesSheet(
        onDone = { r -> receive = false; vm.toast("${r.cookies} cookies de ${r.sites} sites recebidos do PC") },
        onDismiss = { receive = false },
    )
}

/** Um jeito de conectar a conta: explicação à esquerda e o botão tonal (o amarelo fica com Concluir). */
@Composable
private fun Way(icon: ImageVector, title: String, text: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick, Modifier.fillMaxWidth()) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title)
        }
    }
}
