package com.subarudl.app.ui.components

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.subarudl.app.data.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uma opção de uma lista de escolha: valor, rótulo e uma linha de apoio opcional. */
data class Opt<T>(val value: T, val label: String, val sub: String? = null)

/** Campo de escolha do Material 3 (menu que desce do campo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropField(
    label: String,
    value: T,
    options: List<Opt<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.value == value }
    ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { if (enabled) open = it }, modifier = modifier) {
        OutlinedTextField(
            value = current?.label ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = leading?.let { { Icon(it, null) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open && enabled) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 380.dp)) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(o.label, color = if (o.value == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (o.sub != null) Text(o.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { open = false; onSelect(o.value) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Miniatura de um vídeo ou foto, com um ícone enquanto não há imagem. */
@Composable
fun Thumb(url: String?, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp), icon: ImageVector = Icons.Outlined.Image) {
    // O ícone fica embaixo: se a imagem não carregar, ele continua aparecendo
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** Miniatura de um vídeo que já está no celular: o próprio Android gera o quadro. */
@Composable
fun LocalVideoThumb(uri: String, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp), icon: ImageVector = Icons.Outlined.Image) {
    val ctx = LocalContext.current
    val frame by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { ctx.contentResolver.loadThumbnail(Uri.parse(uri), Size(320, 180), null).asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        frame?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** As primeiras fotos de um download de imagens, em mosaico de 2 por 2. */
@Composable
fun GalleryMosaic(item: QueueItem, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    val pics = item.outputs.filter { it.mime.startsWith("image/") }.take(4)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        if (pics.isEmpty()) Icon(Icons.Outlined.Collections, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else Column(Modifier.fillMaxSize()) {
            pics.chunked(2).forEach { row ->
                Row(Modifier.weight(1f)) {
                    row.forEach { p -> AsyncImage(p.uri, null, Modifier.weight(1f).fillMaxSize(), contentScale = ContentScale.Crop) }
                }
            }
        }
    }
}

/** Título de uma parte da lista (estilo de subtítulo do Material), com uma ação opcional à direita. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

private val clearList @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

/** Linha com chave liga/desliga: a linha inteira liga e desliga. */
@Composable
fun SwitchItem(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit, icon: ImageVector? = null, enabled: Boolean = true) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = sub?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, null) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = clearList,
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    )
}

/** Linha que abre outra página ou uma escolha; o valor atual aparece embaixo do título. */
@Composable
fun NavItem(title: String, sub: String?, icon: ImageVector? = null, chevron: Boolean = true, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = sub?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        leadingContent = icon?.let { { Icon(it, null) } },
        trailingContent = if (chevron) ({ Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }) else null,
        colors = clearList,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Linha de informação, sem toque. */
@Composable
fun InfoItem(title: String, value: String, icon: ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        leadingContent = icon?.let { { Icon(it, null) } },
        trailingContent = trailing,
        colors = clearList,
    )
}

/** Espaço lateral padrão do conteúdo. */
val Gutter: Dp = 16.dp

/** Largura máxima da coluna de conteúdo: em tablet a lista fica no centro, como nos apps do Google. */
val ContentMaxWidth: Dp = 720.dp

/** Margem lateral que centraliza a coluna de conteúdo quando a tela é mais larga que [ContentMaxWidth]. */
fun sideFor(width: Dp): Dp = ((width - ContentMaxWidth) / 2).coerceAtLeast(0.dp)
