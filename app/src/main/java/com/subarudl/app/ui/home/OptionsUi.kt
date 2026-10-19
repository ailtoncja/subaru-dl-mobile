package com.subarudl.app.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.FormatInfo
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.Mode
import com.subarudl.app.data.Storage
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.Gutter
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SwitchItem
import com.subarudl.app.util.applySubtitle
import com.subarudl.app.util.audioTrackOptions
import com.subarudl.app.util.commonSubLangs
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.formatDuration
import com.subarudl.app.util.languageName
import com.subarudl.app.util.subLangsFor
import com.subarudl.app.util.subtitleChoices
import com.subarudl.app.util.subtitleLabel
import com.subarudl.app.util.subtitleValue
import com.subarudl.app.util.videoCodecs

/** Abre o seletor de pastas do Android e guarda a permissão para continuar escrevendo nela. */
@Composable
fun rememberFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            onPicked(uri.toString())
        }
    }
    return { launcher.launch(null) }
}

private val clearList @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

/** "Salvar em": a pasta padrão do Android (Filmes, Música ou Imagens) ou uma pasta escolhida. */
@Composable
fun DestinationItem(destination: String?, audio: Boolean, onChange: (String?) -> Unit, gallery: Boolean = false, title: String = "Salvar em") {
    val ctx = LocalContext.current
    val pick = rememberFolderPicker { onChange(it) }
    val label = if (gallery) Storage.galleryLabel(ctx, destination) else Storage.destinationLabel(ctx, destination, audio)
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(Icons.Outlined.FolderOpen, null) },
        trailingContent = {
            Row {
                if (destination != null) TextButton({ onChange(null) }) { Text("Padrão") }
                TextButton(pick) { Text("Alterar") }
            }
        },
        colors = clearList,
    )
}

val subfolderOptions = listOf(
    Opt("none", "Sem subpastas"),
    Opt("playlist", "Uma pasta por playlist"),
    Opt("channel", "Uma pasta por canal"),
    Opt("channel-playlist", "Canal e depois playlist"),
    Opt("year-month", "Por ano e mês"),
    Opt("artist-album", "Artista e depois álbum"),
    Opt("custom", "Personalizado…"),
)

@Composable
fun OrganizeField(value: String, custom: String, onChange: (String, String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropField("Organizar em pastas", value, subfolderOptions, { onChange(it, custom) }, Modifier.fillMaxWidth(), leading = Icons.Outlined.AccountTree)
        if (value == "custom") {
            OutlinedTextField(
                value = custom, onValueChange = { onChange(value, it) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Modelo da pasta (yt-dlp)") }, supportingText = { Text("Ex.: %(uploader)s/%(upload_date>%Y)s") },
            )
        }
    }
}

/** Chips de escolha única (qualidade, taxa do áudio): mostram o tamanho estimado junto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Opt<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FlowRow(modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o.value == selected
            FilterChip(
                selected = on, onClick = { onSelect(o.value) }, enabled = enabled,
                label = { Text(if (o.sub != null) "${o.label} · ${o.sub}" else o.label) },
                leadingIcon = if (on) ({ Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
            )
        }
    }
}

/** Escolha da faixa de áudio (a original é sempre a padrão). Só aparece quando o vídeo tem mais de uma. */
@Composable
fun AudioTrackField(info: MediaInfo, o: DownloadOptions, onChange: (DownloadOptions) -> Unit, modifier: Modifier = Modifier) {
    if (info.audioLangs.size < 2) return
    DropField(
        "Faixa de áudio", o.audioLang ?: "", audioTrackOptions(info.audioLangs, info.originalLang),
        { onChange(o.copy(audioLang = it.ifEmpty { null })) }, modifier.fillMaxWidth(), leading = Icons.Outlined.RecordVoiceOver,
    )
}

/** Legenda do vídeo: as que existem de verdade, as automáticas e se vai embutida ou em arquivo. */
@Composable
fun SubtitleFields(info: MediaInfo, o: DownloadOptions, onChange: (DownloadOptions) -> Unit, modifier: Modifier = Modifier) {
    if (info.isPlaylist) {
        GenericSubtitleFields(o, onChange, modifier)
        return
    }
    val s = o.subs
    val (manual, auto) = subtitleChoices(info)
    if (manual.isEmpty() && auto.isEmpty()) {
        Text("Este vídeo não tem legendas.", modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val value = subtitleValue(s)
    val options = buildList {
        add(Opt("", "Nenhuma"))
        if (value == "padrao") add(Opt("padrao", subtitleLabel(s) + " (padrão)"))
        manual.forEach { add(Opt(it.value, it.label)) }
        auto.forEach { add(Opt(it.value, it.label)) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropField("Legenda", value, options, { onChange(o.copy(subs = applySubtitle(s, it))) }, Modifier.fillMaxWidth(), leading = Icons.Outlined.ClosedCaption)
        if (value.isNotEmpty()) EmbedField(o, onChange)
    }
}

/**
 * Legenda para playlists e lotes: o app não sabe as legendas de cada vídeo, então escolhe-se o idioma e o yt-dlp procura
 * em cada um (a feita pelo autor e, se faltar, a automática).
 */
@Composable
fun GenericSubtitleFields(o: DownloadOptions, onChange: (DownloadOptions) -> Unit, modifier: Modifier = Modifier) {
    val s = o.subs
    val current = if (!s.enabled) "" else s.langs.firstOrNull { it in commonSubLangs } ?: s.langs.firstOrNull() ?: ""
    val options = buildList {
        add(Opt("", "Nenhuma"))
        commonSubLangs.forEach { add(Opt(it, languageName(it))) }
        if (current.isNotEmpty() && current !in commonSubLangs) add(Opt(current, languageName(current)))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropField(
            "Legenda", current, options,
            { code -> onChange(o.copy(subs = if (code.isEmpty()) s.copy(enabled = false) else s.copy(enabled = true, all = false, langs = subLangsFor(code), auto = if (s.enabled) s.auto else true))) },
            Modifier.fillMaxWidth(), leading = Icons.Outlined.ClosedCaption,
        )
        if (s.enabled) EmbedField(o, onChange)
    }
}

@Composable
private fun EmbedField(o: DownloadOptions, onChange: (DownloadOptions) -> Unit) {
    val s = o.subs
    DropField(
        "Como salvar a legenda", if (s.embed) "embed" else "file",
        listOf(Opt("embed", "Dentro do vídeo"), Opt("file", "Arquivo separado (.${s.format})")),
        { onChange(o.copy(subs = s.copy(embed = it == "embed"))) }, Modifier.fillMaxWidth(),
    )
}

/** Opções avançadas, recolhidas numa linha que abre no lugar. */
@Composable
fun AdvancedSection(info: MediaInfo?, o: DownloadOptions, isPlaylist: Boolean, onChange: (DownloadOptions) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        ListItem(
            headlineContent = { Text("Opções avançadas") },
            supportingContent = { Text("Trecho, codec, formato exato, capa e metadados") },
            leadingContent = { Icon(Icons.Outlined.Tune, null) },
            trailingContent = { Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (open) "Recolher" else "Abrir") },
            colors = clearList,
            modifier = Modifier.clickable { open = !open },
        )
        AnimatedVisibility(open) { AdvancedOptions(info, o, isPlaylist, onChange) }
    }
}

@Composable
private fun AdvancedOptions(info: MediaInfo?, o: DownloadOptions, isPlaylist: Boolean, onChange: (DownloadOptions) -> Unit) {
    Column {
        if (o.mode == Mode.VIDEO) {
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropField("Codec do vídeo", o.codecVideo, videoCodecs, { onChange(o.copy(codecVideo = it)) }, Modifier.fillMaxWidth())
                if (info != null && info.formats.isNotEmpty() && !isPlaylist) {
                    val choices = info.formats.filter { it.hasVideo || it.hasAudio }
                        .sortedWith(compareByDescending<FormatInfo> { it.hasVideo }.thenByDescending { it.height ?: 0 }.thenByDescending { it.tbr ?: 0.0 })
                    DropField(
                        "Formato exato",
                        o.formatId ?: "",
                        listOf(Opt("", "Escolher automaticamente")) + choices.map {
                            Opt(
                                it.id,
                                "${it.id} · ${it.ext}" + (it.height?.let { h -> " · ${h}p" } ?: "") +
                                    (if (!it.hasVideo) " · só áudio" else if (!it.hasAudio) " · sem áudio" else "") +
                                    (it.size?.let { s -> " · " + formatBytes(s) } ?: ""),
                            )
                        },
                        { id ->
                            val f = choices.firstOrNull { it.id == id }
                            onChange(o.copy(formatId = id.ifEmpty { null }, formatVideoOnly = f != null && f.hasVideo && !f.hasAudio))
                        },
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            SwitchItem("Limitar a 30 quadros por segundo", "Economiza espaço; alguns vídeos só têm 30 fps em qualidade menor", o.fps30, { onChange(o.copy(fps30 = it)) })
        }
        Row(Modifier.padding(horizontal = Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(o.sectionStart, { onChange(o.copy(sectionStart = it)) }, Modifier.weight(1f), singleLine = true, label = { Text("Começa em") }, placeholder = { Text("00:00") })
            OutlinedTextField(o.sectionEnd, { onChange(o.copy(sectionEnd = it)) }, Modifier.weight(1f), singleLine = true, label = { Text("Termina em") }, placeholder = { Text(info?.duration?.let { formatDuration(it) } ?: "fim") })
        }
        if (info != null && info.chapters.isNotEmpty()) {
            SwitchItem("Separar por capítulos", "${info.chapters.size} capítulos viram arquivos separados", o.splitChapters, { onChange(o.copy(splitChapters = it)) })
        }
        if (info?.isLive == true) SwitchItem("Ao vivo: baixar desde o começo", null, o.liveFromStart, { onChange(o.copy(liveFromStart = it)) })
        if (isPlaylist) {
            SwitchItem("Numerar os arquivos", "01, 02, 03… no começo do nome", o.numbering, { onChange(o.copy(numbering = it)) })
            SwitchItem("Ordem invertida", "Começa pelo último vídeo", o.reverse, { onChange(o.copy(reverse = it)) })
        }
        SwitchItem("Gravar título, canal e data no arquivo", null, o.embedMetadata, { onChange(o.copy(embedMetadata = it)) })
        SwitchItem("Embutir a capa", "A miniatura vira a capa do arquivo", o.embedThumb, { onChange(o.copy(embedThumb = it)) })
        SwitchItem("Salvar a miniatura separada", "Um .jpg ao lado do arquivo", o.saveThumb, { onChange(o.copy(saveThumb = it)) })
        SwitchItem("Manter como veio do site", "Não converte nem muda o formato", o.keepOriginal, { onChange(o.copy(keepOriginal = it)) })
    }
}
