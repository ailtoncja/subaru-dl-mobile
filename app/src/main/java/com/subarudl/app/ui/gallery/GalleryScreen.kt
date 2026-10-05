package com.subarudl.app.ui.gallery

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.subarudl.app.GalleryLink
import com.subarudl.app.MainViewModel
import com.subarudl.app.data.GalleryFile
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.BtnPrimary
import com.subarudl.app.ui.components.CheckSubaru
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SCard
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.home.DestinationRow
import com.subarudl.app.ui.home.SwitchRow
import com.subarudl.app.ui.home.fieldColors
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.Manrope
import com.subarudl.app.ui.theme.T

private val organizeOptions = listOf(
    Opt("flat", "Sem subpastas"),
    Opt("site", "Por site"),
    Opt("profile", "Só o perfil"),
    Opt("default", "Por site → perfil"),
    Opt("custom", "Personalizado…"),
)

@Composable
fun GalleryScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val clipboard = LocalClipboardManager.current
    val links = vm.galLinks
    val o = vm.galOptions

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(C.Gold))
                Spacer(Modifier.width(8.dp))
                Text("Galeria", style = T.headlineLg)
            }
            Text("Perfis, posts, álbuns e capítulos de mangá — pelo gallery-dl", style = T.bodySm.copy(fontSize = 14.sp), color = C.Muted, modifier = Modifier.padding(top = 4.dp))
        }

        item {
            SCard(padding = 14.dp) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(C.Field).border(1.dp, C.Border, RoundedCornerShape(8.dp)).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(22.dp), tint = C.Muted)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (vm.galText.isEmpty()) Text("Perfil, post ou álbum (vários, um por linha)", style = T.bodySm.copy(fontSize = 14.sp), color = C.Dim)
                        BasicTextField(
                            vm.galText, { vm.galText = it }, textStyle = T.bodyMd.copy(color = C.Text, fontFamily = Manrope), cursorBrush = SolidColor(C.Gold), maxLines = 5,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                    if (vm.galText.isNotEmpty()) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.galText = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, null, Modifier.size(18.dp), tint = C.Muted) }
                    }
                    Row(
                        Modifier.height(38.dp).clip(RoundedCornerShape(6.dp)).background(C.Blue).clickable {
                            val t = clipboard.getText()?.text.orEmpty()
                            if (t.contains("http")) vm.galText = (vm.galText + "\n" + t).trim() else vm.toast("Nenhum link na área de transferência")
                        }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.ContentPaste, null, Modifier.size(17.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Colar", style = T.labelMd.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BtnGhost("Ver arquivos", { vm.galleryPrepare(vm.galText) }, Modifier.weight(1f), icon = Icons.Outlined.Visibility, enabled = vm.galText.isNotBlank(), height = 52.dp)
                    BtnPrimary("Baixar tudo", { vm.galleryDownloadText(vm.galText) }, Modifier.weight(1f), icon = Icons.Outlined.Download, enabled = vm.galText.isNotBlank())
                }
                Spacer(Modifier.height(14.dp))
                DestinationRow(o.destination ?: settings.galleryDest, false, { vm.galOptions = o.copy(destination = it) }, gallery = true)
                Spacer(Modifier.height(10.dp))
                DropField(o.organize.takeIf { x -> organizeOptions.any { it.value == x } } ?: "default", organizeOptions, { vm.galOptions = o.copy(organize = it) }, Modifier.fillMaxWidth(), leading = Icons.Outlined.AccountTree, prefix = "Organizar: ")
                if (o.organize == "custom") {
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        o.customFormat, { vm.galOptions = o.copy(customFormat = it) }, Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Pastas separadas por /") }, supportingText = { Text("Ex.: {category}/{user}") }, colors = fieldColors(), textStyle = T.labelLg,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SwitchRow("Pular o que já baixei", "Lembra cada arquivo baixado e não baixa de novo", settings.gallerySkipDone) { v -> vm.settings { it.copy(gallerySkipDone = v) } }
            }
        }

        // ---- Prévia por link ----
        if (links.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("${links.size} ${if (links.size == 1) "link" else "links"} • escolha o que baixar", Modifier.weight(1f))
                    Text("Limpar", Modifier.clickable { vm.galLinks = emptyList() }.padding(6.dp), style = T.labelMd, color = C.Gold)
                }
            }
            links.forEachIndexed { index, link ->
                item(key = "link-$index-${link.url}") { LinkHeader(vm, index, link) }
                if (link.expanded) {
                    if (link.loaded && link.files.isNotEmpty()) {
                        item(key = "tools-$index") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BtnGhost("Marcar todos", { vm.updateGal(index) { it.copy(selected = it.files.map { f -> f.index }.toSet()) } }, Modifier.weight(1f), height = 40.dp)
                                BtnGhost("Desmarcar", { vm.updateGal(index) { it.copy(selected = emptySet()) } }, Modifier.weight(1f), height = 40.dp)
                            }
                        }
                        items(link.files.chunked(3), key = { "grid-$index-${it.first().index}" }) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { f ->
                                    val on = f.index in link.selected
                                    FileTile(f, link.url, on, Modifier.weight(1f)) {
                                        vm.updateGal(index) { cur -> cur.copy(selected = if (on) cur.selected - f.index else cur.selected + f.index) }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (!link.end) item(key = "more-$index") {
                            BtnGhost(if (link.loading) "Carregando…" else "Carregar mais arquivos", { vm.galleryLoad(index, true) }, Modifier.fillMaxWidth(), enabled = !link.loading)
                        }
                    }
                }
            }
            item {
                val count = links.count { !it.loaded || it.selected.isNotEmpty() }
                BtnPrimary("Baixar $count ${if (count == 1) "link" else "links"}", { vm.galleryDownload() }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Collections, enabled = count > 0)
            }
        }
    }
}

@Composable
private fun LinkHeader(vm: MainViewModel, index: Int, link: GalleryLink) {
    SCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(link.url.removePrefix("https://").removePrefix("http://").removePrefix("www."), style = T.bodyMd.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (link.loaded) Mono("${link.selected.size} de ${link.files.size}${if (link.end) "" else "+"} marcados", color = C.Muted, style = T.labelMd)
                else if (!link.loading && link.error == null) Mono("Toque em Ver para listar os arquivos", color = C.Dim, style = T.labelMd)
            }
            if (link.loading) CircularProgressIndicator(Modifier.size(20.dp), color = C.Gold, strokeWidth = 2.dp)
            else {
                Row(
                    Modifier.height(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x14FFFFFF)).clickable {
                        if (!link.loaded) vm.galleryLoad(index, false) else vm.updateGal(index) { it.copy(expanded = !it.expanded) }
                    }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(if (link.expanded) "Fechar" else "Ver", style = T.labelMd, color = C.Text) }
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.galLinks = vm.galLinks.filterIndexed { i, _ -> i != index } }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, null, Modifier.size(18.dp), tint = C.Muted)
            }
        }
        link.error?.let { Text(it, style = T.bodySm, color = C.Error, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun FileTile(f: GalleryFile, page: String, on: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    val referer = remember(page) { runCatching { java.net.URI(page).let { "${it.scheme}://${it.host}/" } }.getOrNull() }
    Box(
        modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(C.CardHighest)
            .border(2.dp, if (on) C.Gold else Color.Transparent, RoundedCornerShape(8.dp)).clickable(onClick = onToggle),
    ) {
        if (f.video) Icon(Icons.Filled.PlayArrow, null, Modifier.align(Alignment.Center).size(34.dp), tint = C.Muted)
        else {
            val request = remember(f.url, referer) {
                ImageRequest.Builder(ctx).data(f.url).apply {
                    if (referer != null) httpHeaders(NetworkHeaders.Builder().add("Referer", referer).add("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36").build())
                }.build()
            }
            AsyncImage(request, null, Modifier.fillMaxSize().then(if (on) Modifier else Modifier), contentScale = ContentScale.Crop, alpha = if (on) 1f else 0.4f)
        }
        Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { CheckSubaru(on, { onToggle() }, size = 22.dp) }
        Text(f.index.toString(), Modifier.align(Alignment.BottomEnd).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xBF000000)).padding(horizontal = 5.dp), style = T.labelSm, color = C.Text)
    }
}
