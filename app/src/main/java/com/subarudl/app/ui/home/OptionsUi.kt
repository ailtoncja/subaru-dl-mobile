package com.subarudl.app.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.subarudl.app.data.DownloadOptions
import com.subarudl.app.data.FormatInfo
import com.subarudl.app.data.MediaInfo
import com.subarudl.app.data.Mode
import com.subarudl.app.data.Storage
import com.subarudl.app.ui.components.BtnGhost
import com.subarudl.app.ui.components.CheckSubaru
import com.subarudl.app.ui.components.DropField
import com.subarudl.app.ui.components.Mono
import com.subarudl.app.ui.components.Opt
import com.subarudl.app.ui.components.SectionLabel
import com.subarudl.app.ui.components.SwitchSubaru
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T
import com.subarudl.app.util.applySubtitle
import com.subarudl.app.util.audioTrackOptions
import com.subarudl.app.util.formatBytes
import com.subarudl.app.util.formatDuration
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

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

@Composable
fun DestinationRow(destination: String?, audio: Boolean, onChange: (String?) -> Unit, gallery: Boolean = false, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val pick = rememberFolderPicker { onChange(it) }
    val label = if (gallery) Storage.galleryLabel(ctx, destination) else Storage.destinationLabel(ctx, destination, audio)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp), tint = C.Muted)
        Spacer(Modifier.width(8.dp))
        Mono("Salvar em: ", color = C.Muted, style = T.labelMd)
        Mono(label, Modifier.weight(1f, fill = false), color = C.Text, style = T.labelMd)
        Spacer(Modifier.width(10.dp))
        Text("Alterar", Modifier.clickableNoRipple(pick), style = T.labelMd, color = C.Gold)
        if (destination != null) {
            Spacer(Modifier.width(10.dp))
            Text("Padrão", Modifier.clickableNoRipple { onChange(null) }, style = T.labelMd, color = C.Muted)
        }
    }
}

val subfolderOptions = listOf(
    Opt("none", "Sem subpastas"),
    Opt("playlist", "Por playlist"),
    Opt("channel", "Por canal"),
    Opt("channel-playlist", "Canal → playlist"),
    Opt("year-month", "Por ano e mês"),
    Opt("artist-album", "Artista → álbum"),
    Opt("custom", "Personalizado…"),
)

@Composable
fun OrganizeSelect(value: String, custom: String, onChange: (String, String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropField(value, subfolderOptions, { onChange(it, custom) }, Modifier.fillMaxWidth(), leading = Icons.Outlined.AccountTree, prefix = "Organizar: ")
        if (value == "custom") {
            OutlinedTextField(
                value = custom, onValueChange = { onChange(value, it) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Modelo da pasta (yt-dlp)") }, supportingText = { Text("Ex.: %(uploader)s/%(upload_date>%Y)s") },
                colors = fieldColors(), textStyle = T.labelLg,
            )
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = C.BlueLight, unfocusedBorderColor = C.BorderStrong, focusedContainerColor = C.Field, unfocusedContainerColor = C.Field,
    focusedTextColor = C.Text, unfocusedTextColor = C.Text, cursorColor = C.Gold, focusedLabelColor = C.BluePale, unfocusedLabelColor = C.Muted,
    focusedSupportingTextColor = C.Muted, unfocusedSupportingTextColor = C.Muted,
)

/** Escolha da faixa de áudio (a original é sempre a padrão). */
@Composable
fun AudioTrackSelect(info: MediaInfo, o: DownloadOptions, onChange: (DownloadOptions) -> Unit, modifier: Modifier = Modifier) {
    if (info.audioLangs.size < 2) return
    DropField(
        value = o.audioLang ?: "",
        options = audioTrackOptions(info.audioLangs, info.originalLang),
        onSelect = { onChange(o.copy(audioLang = it.ifEmpty { null })) },
        modifier = modifier.fillMaxWidth(), leading = Icons.Outlined.RecordVoiceOver, prefix = "Áudio: ",
    )
}

/** Legenda do vídeo: as que existem de verdade, as automáticas e se vai embutida ou separada. */
@Composable
fun SubtitleSelect(info: MediaInfo, o: DownloadOptions, onChange: (DownloadOptions) -> Unit, modifier: Modifier = Modifier) {
    val s = o.subs
    if (info.isPlaylist) {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CheckSubaru(s.enabled, { on -> onChange(o.copy(subs = s.copy(enabled = on, auto = if (on) true else s.auto))) })
            Spacer(Modifier.width(12.dp))
            Text("Legendas ${(s.langs.firstOrNull() ?: "pt").uppercase()} (ou automática)", style = T.bodyMd)
            Spacer(Modifier.weight(1f))
            Mono(".${s.format.uppercase()}", color = C.Dim)
        }
        return
    }
    val (manual, auto) = subtitleChoices(info)
    if (manual.isEmpty() && auto.isEmpty()) {
        Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ClosedCaption, null, Modifier.size(20.dp), tint = C.Dim)
            Spacer(Modifier.width(10.dp))
            Text("Este vídeo não tem legendas", style = T.bodySm, color = C.Dim)
        }
        return
    }
    val value = subtitleValue(s)
    val options = buildList {
        add(Opt("", "nenhuma"))
        if (value == "padrao") add(Opt("padrao", subtitleLabel(s) + " (padrão)"))
        manual.forEach { add(Opt(it.value, it.label)) }
        auto.forEach { add(Opt(it.value, it.label)) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropField(value, options, { onChange(o.copy(subs = applySubtitle(s, it))) }, Modifier.fillMaxWidth(), leading = Icons.Outlined.ClosedCaption, prefix = "Legenda: ")
        if (value.isNotEmpty()) {
            DropField(
                if (s.embed) "embed" else "file",
                listOf(Opt("embed", "Embutida no vídeo"), Opt("file", "Arquivo separado (.${s.format})")),
                { onChange(o.copy(subs = s.copy(embed = it == "embed"))) },
                Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- Opções avançadas ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSheet(info: MediaInfo?, o: DownloadOptions, isPlaylist: Boolean, onChange: (DownloadOptions) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = C.Card, contentColor = C.Text) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Opções avançadas", style = T.headlineSm)

            if (o.mode == Mode.VIDEO) {
                SectionLabel("Vídeo")
                DropField(o.codecVideo, videoCodecs, { onChange(o.copy(codecVideo = it)) }, Modifier.fillMaxWidth(), prefix = "Codec: ")
                SwitchRow("Limitar a 30 FPS", "Economiza espaço; alguns vídeos só têm 30 FPS em qualidade menor", o.fps30) { onChange(o.copy(fps30 = it)) }
                if (info != null && info.formats.isNotEmpty()) {
                    val choices = info.formats.filter { it.hasVideo || it.hasAudio }
                        .sortedWith(compareByDescending<FormatInfo> { it.hasVideo }.thenByDescending { it.height ?: 0 }.thenByDescending { it.tbr ?: 0.0 })
                    DropField(
                        o.formatId ?: "",
                        listOf(Opt("", "Escolher automaticamente")) + choices.map {
                            Opt(
                                it.id,
                                "${it.id} • ${it.ext}" + (it.height?.let { h -> " • ${h}p" } ?: "") +
                                    (if (!it.hasVideo) " • só áudio" else if (!it.hasAudio) " • sem áudio" else "") +
                                    (it.size?.let { s -> " • " + formatBytes(s) } ?: ""),
                            )
                        },
                        { id ->
                            val f = choices.firstOrNull { it.id == id }
                            onChange(o.copy(formatId = id.ifEmpty { null }, formatVideoOnly = f != null && f.hasVideo && !f.hasAudio))
                        },
                        Modifier.fillMaxWidth(), prefix = "Formato: ",
                    )
                }
            }

            SectionLabel("Trecho")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(o.sectionStart, { onChange(o.copy(sectionStart = it)) }, Modifier.weight(1f), singleLine = true, label = { Text("Início") }, placeholder = { Text("00:00") }, colors = fieldColors(), textStyle = T.labelLg)
                OutlinedTextField(o.sectionEnd, { onChange(o.copy(sectionEnd = it)) }, Modifier.weight(1f), singleLine = true, label = { Text("Fim") }, placeholder = { Text(info?.duration?.let { formatDuration(it) } ?: "fim") }, colors = fieldColors(), textStyle = T.labelLg)
            }
            if (info != null && info.chapters.isNotEmpty()) {
                SwitchRow("Separar por capítulos", "${info.chapters.size} capítulos viram arquivos separados", o.splitChapters) { onChange(o.copy(splitChapters = it)) }
            }
            if (info?.isLive == true) SwitchRow("Live: baixar desde o começo", null, o.liveFromStart) { onChange(o.copy(liveFromStart = it)) }

            if (isPlaylist) {
                SectionLabel("Playlist")
                SwitchRow("Numerar os arquivos", "Prefixo 01, 02, 03… no nome", o.numbering) { onChange(o.copy(numbering = it)) }
                SwitchRow("Ordem invertida", "Começa pelo último vídeo", o.reverse) { onChange(o.copy(reverse = it)) }
            }

            SectionLabel("Pós-processamento")
            SwitchRow("Gravar título, canal e data no arquivo", null, o.embedMetadata) { onChange(o.copy(embedMetadata = it)) }
            SwitchRow("Embutir a capa", "Miniatura como capa do arquivo", o.embedThumb) { onChange(o.copy(embedThumb = it)) }
            SwitchRow("Salvar a miniatura em separado", "Um .jpg ao lado do arquivo", o.saveThumb) { onChange(o.copy(saveThumb = it)) }
            SwitchRow("Manter os streams originais", "Não converte nem muda o formato: sai como veio do site", o.keepOriginal) { onChange(o.copy(keepOriginal = it)) }

            BtnGhost("Fechar", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, gold: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bodyMd)
            if (sub != null) Text(sub, style = T.labelSm, color = C.Muted)
        }
        Spacer(Modifier.width(12.dp))
        SwitchSubaru(checked, onChange, gold)
    }
}
