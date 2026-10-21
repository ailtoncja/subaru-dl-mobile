package com.subarudl.app.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subarudl.app.LinkJob
import com.subarudl.app.MainViewModel
import com.subarudl.app.R
import com.subarudl.app.Section
import com.subarudl.app.data.Storage
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.components.sideFor
import com.subarudl.app.util.extractLinks

/** Texto da área de transferência (só dá para ler com o app na frente). */
fun readClipboard(ctx: Context): String {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
}

/** A tela principal: colar ou compartilhar o link e acompanhar os downloads no mesmo lugar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, snackbar: SnackbarHostState) {
    val ctx = LocalContext.current
    val items by vm.manager.items.collectAsState()
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val groups = remember(items, query) { Groups(items, query) }
    val fabExpanded = listState.isScrollingUp()

    val paste: () -> Unit = {
        val text = readClipboard(ctx)
        if (extractLinks(text).isNotEmpty()) vm.submit(text)
        else vm.toast("Nenhum link copiado. No app do vídeo, toque em Compartilhar e depois em Copiar link.")
    }

    BackHandler(enabled = searching) { searching = false; query = "" }
    LaunchedEffect(vm.addedTick) { if (vm.addedTick > 0) listState.animateScrollToItem(0) }
    LaunchedEffect(vm.focus) {
        val f = vm.focus ?: return@LaunchedEffect
        vm.focus = null
        headerIndex(groups, f)?.let { listState.animateScrollToItem(it) }
    }

    // Em tablet o conteúdo fica numa coluna central e a barra de cima alinha com ela; a rolagem vale na tela toda
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = sideFor(maxWidth)
        val barInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = side, right = side))
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (searching) SearchBar(query, { query = it }, onClose = { searching = false; query = "" }, barInsets)
                else TopAppBar(
                    title = { Text("Subaru DL") },
                    windowInsets = barInsets,
                    actions = {
                        if (items.isNotEmpty()) IconButton({ searching = true }) { Icon(Icons.Outlined.Search, "Buscar nos downloads") }
                        IconButton({ vm.openSettings() }) { Icon(Icons.Outlined.Settings, "Ajustes") }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Colar link") },
                    icon = { Icon(Icons.Outlined.ContentPaste, null) },
                    onClick = paste,
                    expanded = fabExpanded,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().imePadding(),
                state = listState,
                contentPadding = PaddingValues(start = side, end = side, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                if (!searching) item(key = "bar") { LinkBar(vm, paste) }
                if (groups.isEmpty) item(key = "empty") { if (query.isBlank()) EmptyState() else NoResults(query) }
                else {
                    downloadSections(vm, groups, actions = !searching, onDetails = { details = it }, onOpen = { it.outputs.firstOrNull()?.takeIf(Storage::canOpen)?.let { f -> openFile(ctx, vm, f) } ?: run { details = it.id } })
                    if (!searching) item(key = "storage") { StorageLine(items) }
                }
            }
        }
    }

    details?.let { id -> ItemSheet(vm, id) { details = null } }
    vm.homeInfo?.let { VideoSheet(vm, it) }
    if (vm.batchSources.isNotEmpty()) BatchSheet(vm)
    if (vm.galLinks.isNotEmpty()) GallerySheet(vm)
}

/** A lista está subindo ou parada: o botão "Colar link" mostra o texto, como no Arquivos e no Seal; descendo, vira só o ícone. */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            (if (previousIndex != firstVisibleItemIndex) previousIndex > firstVisibleItemIndex else previousOffset >= firstVisibleItemScrollOffset).also {
                previousIndex = firstVisibleItemIndex
                previousOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

/** Posição do título de uma seção na lista (o campo de link é o item 0). */
private fun headerIndex(g: Groups, s: Section): Int? {
    var i = 1
    if (g.running.isNotEmpty()) { if (s == Section.ACTIVE) return i; i += 1 + g.running.size }
    if (g.waiting.isNotEmpty()) { if (s == Section.ACTIVE) return i; i += 1 + g.waiting.size }
    if (g.failed.isNotEmpty()) { if (s == Section.FAILED) return i; i += 1 + g.failed.size }
    if (g.done.isNotEmpty() && s == Section.DONE) return i
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit, insets: WindowInsets) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TopAppBar(
        navigationIcon = { IconButton(onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Fechar a busca") } },
        title = {
            TextField(
                query, onQuery, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                placeholder = { Text("Buscar nos downloads") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        },
        actions = { if (query.isNotEmpty()) IconButton({ onQuery("") }) { Icon(Icons.Outlined.Close, "Limpar a busca") } },
        windowInsets = insets,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

/** Campo do link, a escolha Vídeo/Áudio/Imagens e o que está acontecendo com o link colado. */
@Composable
private fun LinkBar(vm: MainViewModel, onPaste: () -> Unit) {
    val engine by vm.engine.collectAsState()
    val net by vm.manager.net.collectAsState()
    val settings by vm.settings.collectAsState()
    Column(Modifier.padding(horizontal = Gutter).padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinkField(vm, onPaste)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinkJob.entries.forEach { j ->
                val on = vm.job == j
                FilterChip(
                    selected = on, onClick = { vm.job = j }, label = { Text(j.label) },
                    leadingIcon = if (on) ({ Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
                )
            }
        }
        when {
            engine.error != null -> Notice(Icons.Outlined.ErrorOutline, "As ferramentas não puderam ser preparadas", engine.error, error = true)
            !engine.ready -> Working("Preparando as ferramentas (só na primeira vez)…")
            vm.homeLoading -> Working("Lendo o link…")
            vm.batchLoading && vm.batchSources.isEmpty() -> Working("Lendo os links…")
        }
        vm.homeError?.let { msg ->
            val hint = vm.homeHint
            val login = needsLogin(hint)
            Notice(
                Icons.Outlined.ErrorOutline,
                when {
                    login -> "${siteSubject(vm.homeUrl) ?: "O site"} pediu login para este link."
                    hint != null -> hint
                    else -> "Não consegui ler esse link"
                },
                body = if (hint == null) msg else null,
                error = true,
                detail = if (hint != null) msg else null,
                actions = buildList {
                    if (login) add("Entrar na conta" to { vm.openSettings("contas") })
                    if (vm.canTryImages) add("Baixar como imagens" to { vm.retryAsImages() })
                    if (vm.homeUrl.isNotBlank()) add("Tentar de novo" to { vm.analyze(vm.homeUrl) })
                },
            )
        }
        UpdateNotice(vm)
        if (!net.online) Notice(Icons.Outlined.WifiOff, "Sem internet", "Os downloads continuam sozinhos quando a conexão voltar.")
        else if (settings.wifiOnly && !net.wifi) {
            Notice(
                Icons.Outlined.WifiOff, "Esperando o Wi-Fi", "\"Só no Wi-Fi\" está ligado nos Ajustes; os downloads começam quando houver Wi-Fi.",
                actions = listOf("Usar os dados móveis" to { vm.update { it.copy(wifiOnly = false) } }),
            )
        }
    }
}

@Composable
private fun LinkField(vm: MainViewModel, onPaste: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    val value = vm.homeUrl
    TextField(
        value = value,
        onValueChange = { new ->
            // Colou pelo teclado: lê o link na hora, sem precisar de outro toque
            val pasted = new.length - value.length > 8 && extractLinks(new).isNotEmpty()
            vm.homeUrl = new
            if (vm.homeError != null) { vm.homeError = null; vm.homeHint = null }
            if (pasted) { vm.submit(new); focusManager.clearFocus() }
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cole ou compartilhe um link") },
        leadingIcon = { Icon(Icons.Outlined.Link, null) },
        trailingIcon = {
            when {
                value.isNotEmpty() -> IconButton({ vm.clearHome() }) { Icon(Icons.Outlined.Close, if (vm.homeLoading) "Parar de ler o link" else "Limpar o link") }
                else -> IconButton(onPaste) { Icon(Icons.Outlined.ContentPaste, "Colar o link copiado") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHigh, unfocusedContainerColor = scheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { vm.submit(value); focusManager.clearFocus() }),
    )
}

@Composable
private fun Working(text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Aviso no alto da tela: erro do link, falta de internet, espera pelo Wi-Fi, versão nova do app. */
@Composable
internal fun Notice(
    icon: ImageVector,
    title: String,
    body: String?,
    error: Boolean = false,
    detail: String? = null,
    actions: List<Pair<String, () -> Unit>> = emptyList(),
    /** Barra de andamento embaixo do texto (0 a 1; negativo = sem saber quanto falta). */
    progress: Float? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var showDetail by rememberSaveable(detail) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = if (error) scheme.errorContainer else scheme.surfaceContainerHigh)) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = if (error) scheme.onErrorContainer else scheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (error) scheme.onErrorContainer else scheme.onSurface)
                body?.let {
                    // O detalhe técnico fica curto: o texto inteiro está no Registro
                    Text(
                        it, style = if (error) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = if (error) scheme.onErrorContainer else scheme.onSurfaceVariant,
                        maxLines = if (error) 3 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis,
                    )
                }
                // O erro técnico (em inglês, do yt-dlp) só aparece para quem pedir; inteiro, ele está no Registro
                if (detail != null) {
                    TextButton(
                        { showDetail = !showDetail }, Modifier.offset(x = (-12).dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = if (error) scheme.onErrorContainer else scheme.primary),
                    ) {
                        Text(if (showDetail) "Esconder o detalhe técnico" else "Ver o detalhe técnico")
                        Icon(if (showDetail) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                    }
                    AnimatedVisibility(showDetail) {
                        Text(
                            detail, style = MaterialTheme.typography.bodySmall, color = if (error) scheme.onErrorContainer else scheme.onSurfaceVariant,
                            maxLines = 8, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                progress?.let { p ->
                    val bar = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)
                    if (p < 0f) LinearProgressIndicator(bar) else LinearProgressIndicator({ p }, bar)
                }
            }
        }
        if (actions.isEmpty()) Spacer(Modifier.height(14.dp))
        // Com letra grande, os botões que não cabem descem para a linha de baixo
        else FlowRow(Modifier.fillMaxWidth().padding(start = 44.dp, end = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {
            // No aviso de erro os botões usam a cor do texto do aviso, não o amarelo
            val colors = if (error) ButtonDefaults.textButtonColors(contentColor = scheme.onErrorContainer) else ButtonDefaults.textButtonColors()
            actions.forEach { (label, onClick) -> TextButton(onClick, colors = colors) { Text(label) } }
        }
    }
}

@Composable
private fun EmptyState() {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_logo), null, Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("Baixe vídeos, músicas e fotos", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Em qualquer app, toque em Compartilhar e escolha Subaru DL. Ou copie o link e toque em Colar link.",
            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Funciona com YouTube, Instagram, TikTok, X, Facebook e mais de 1.800 sites.",
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoResults(query: String) {
    Text(
        "Nada encontrado para \"$query\".", Modifier.fillMaxWidth().padding(32.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
    )
}
