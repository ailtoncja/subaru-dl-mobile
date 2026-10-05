package com.subarudl.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T

private val Stadium = RoundedCornerShape(50)

// ---------- Cartões ----------

/** Cartão tonal do Material 3. Com [accent], ganha uma faixa de cor na lateral. */
@Composable
fun SCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") bar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = C.Card)
    val border = BorderStroke(1.dp, C.Border)
    val inner: @Composable () -> Unit = {
        Box {
            Column(Modifier.padding(start = if (accent != null) padding + 4.dp else padding, end = padding, top = padding, bottom = padding), content = content)
            if (accent != null) Box(Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight().background(accent))
        }
    }
    if (onClick != null) Card(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = colors, border = border) { inner() }
    else Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = colors, border = border) { inner() }
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier, color: Color = C.Muted, style: TextStyle = T.labelSm, maxLines: Int = 1) {
    Text(text, modifier, color = color, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Números que mudam (velocidade, tempo, tamanho), em fonte de largura fixa. */
@Composable
fun Num(text: String, modifier: Modifier = Modifier, color: Color = C.Text, size: Int = 13, maxLines: Int = 1) {
    Text(text, modifier, color = color, style = T.number.copy(fontSize = size.sp), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = C.Muted) {
    Text(text.uppercase(), modifier, color = color, style = T.labelMd.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold))
}

// ---------- Selos ----------

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = C.Gold, background: Color = Color(0x26FACC15), border: Color = Color(0x4DFACC15)) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(background).border(1.dp, border, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        color = color, style = T.labelSm.copy(fontWeight = FontWeight.SemiBold), maxLines = 1,
    )
}

@Composable
fun BlueBadge(text: String, modifier: Modifier = Modifier) = Badge(text, modifier, color = C.BluePale, background = Color(0x331D4ED8), border = Color(0x4D3B82F6))

/** Selo sobre imagens (fundo escuro translúcido para dar leitura). */
@Composable
fun DarkBadge(text: String, color: Color = C.Gold, modifier: Modifier = Modifier) =
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xB3000000)).padding(horizontal = 8.dp, vertical = 3.dp),
        color = color, style = T.labelSm.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), maxLines = 1,
    )

// ---------- Botões ----------

@Composable
private fun BtnContent(text: String, icon: ImageVector?, textStyle: TextStyle = T.button) {
    if (icon != null) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun BtnPrimary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = 52.dp) {
    Button(
        onClick, modifier.heightIn(min = height), enabled = enabled, shape = Stadium,
        colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = Color.White, disabledContainerColor = C.Blue.copy(alpha = 0.3f), disabledContentColor = Color.White.copy(alpha = 0.5f)),
    ) { BtnContent(text, icon) }
}

@Composable
fun BtnGold(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, tag: String? = null, enabled: Boolean = true) {
    Button(
        onClick, modifier.heightIn(min = 54.dp), enabled = enabled, shape = Stadium,
        colors = ButtonDefaults.buttonColors(containerColor = C.GoldDeep, contentColor = C.OnGold, disabledContainerColor = C.GoldDeep.copy(alpha = 0.3f), disabledContentColor = C.OnGold.copy(alpha = 0.5f)),
    ) {
        BtnContent(text, icon)
        if (tag != null) {
            Spacer(Modifier.width(10.dp))
            Text(tag, Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x26000000)).padding(horizontal = 7.dp, vertical = 2.dp), style = T.labelSm.copy(fontWeight = FontWeight.Bold), color = C.OnGold)
        }
    }
}

@Composable
fun BtnGhost(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = C.Text, enabled: Boolean = true, height: Dp = 46.dp) {
    FilledTonalButton(
        onClick, modifier.heightIn(min = height), enabled = enabled, shape = Stadium,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = C.CardHigh, contentColor = color, disabledContainerColor = C.Container, disabledContentColor = C.Dim),
        contentPadding = ButtonDefaults.ContentPadding,
    ) { BtnContent(text, icon, T.labelLg.copy(fontWeight = FontWeight.Bold)) }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.Text, background: Color = C.CardHigh, size: Dp = 40.dp, description: String? = null) {
    FilledTonalIconButton(
        onClick, modifier.size(size),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = background, contentColor = tint),
    ) { Icon(icon, description, Modifier.size(size * 0.5f)) }
}

// ---------- Controles ----------

@Composable
fun SwitchSubaru(checked: Boolean, onChange: (Boolean) -> Unit, gold: Boolean = false) {
    Switch(
        checked, onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = if (gold) C.OnGold else Color(0xFF1D3FB8), checkedTrackColor = if (gold) C.Gold else C.BluePale,
            uncheckedThumbColor = C.Dim, uncheckedTrackColor = C.CardHighest, uncheckedBorderColor = C.Outline,
        ),
    )
}

@Composable
fun CheckSubaru(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") size: Dp = 24.dp) {
    Checkbox(
        checked, onChange, modifier,
        colors = CheckboxDefaults.colors(checkedColor = C.BlueLight, checkmarkColor = Color.White, uncheckedColor = C.Dim),
    )
}

data class Seg<T>(val value: T, val label: String, val icon: ImageVector? = null)

@Composable
fun <T> Segmented(options: List<Seg<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o.value == selected, onClick = { onSelect(o.value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = C.Blue, activeContentColor = Color.White, activeBorderColor = C.Blue,
                    inactiveContainerColor = Color.Transparent, inactiveContentColor = C.Muted, inactiveBorderColor = C.Outline,
                ),
                icon = { if (o.icon != null) Icon(o.icon, null, Modifier.size(18.dp)) else Icon(Icons.Filled.Check, null, Modifier.size(0.dp)) },
                modifier = Modifier.heightIn(min = 44.dp),
            ) { Text(o.label, style = T.labelLg, maxLines = 1) }
        }
    }
}

data class Opt<T>(val value: T, val label: String, val sub: String? = null)

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = C.BluePale, unfocusedBorderColor = C.Outline, disabledBorderColor = C.Border,
    focusedContainerColor = C.SurfaceLowest, unfocusedContainerColor = C.SurfaceLowest, disabledContainerColor = C.SurfaceLowest,
    focusedTextColor = C.Text, unfocusedTextColor = C.Text, disabledTextColor = C.Dim, cursorColor = C.Gold,
    focusedLabelColor = C.BluePale, unfocusedLabelColor = C.Muted, focusedLeadingIconColor = C.BluePale, unfocusedLeadingIconColor = C.Muted,
    focusedTrailingIconColor = C.BluePale, unfocusedTrailingIconColor = C.Muted, disabledTrailingIconColor = C.Dim, disabledLeadingIconColor = C.Dim,
    focusedPlaceholderColor = C.Dim, unfocusedPlaceholderColor = C.Dim,
    focusedSupportingTextColor = C.Muted, unfocusedSupportingTextColor = C.Muted,
)

/** Campo de seleção do Material 3 (menu suspenso). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropField(
    value: T,
    options: List<Opt<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    prefix: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    maxMenuHeight: Dp = 360.dp,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.value == value }
    ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { if (enabled) open = it }, modifier = modifier) {
        OutlinedTextField(
            value = (prefix ?: "") + (current?.label ?: placeholder),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            maxLines = 2,
            textStyle = T.bodyMd.copy(fontWeight = FontWeight.Medium),
            leadingIcon = leading?.let { { Icon(it, null, Modifier.size(20.dp)) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open && enabled) },
            colors = textFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }, containerColor = C.CardHigh, modifier = Modifier.heightIn(max = maxMenuHeight)) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(o.label, style = T.bodyMd.copy(fontWeight = FontWeight.Medium), color = if (o.value == value) C.BluePale else C.Text)
                            if (o.sub != null) Text(o.sub, style = T.labelSm, color = C.Muted)
                        }
                    },
                    onClick = { open = false; onSelect(o.value) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun ChipSmall(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected, onClick, label = { Text(text, style = T.labelLg) }, modifier = modifier, shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, labelColor = C.Muted,
            selectedContainerColor = Color(0x33FACC15), selectedLabelColor = C.Gold,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = C.Outline, selectedBorderColor = Color(0x80FACC15)),
    )
}

@Composable
fun SiteChip(text: String, icon: ImageVector, tint: Color = C.Muted) {
    AssistChip(
        onClick = {}, label = { Text(text, style = T.labelLg) },
        leadingIcon = { Icon(icon, null, Modifier.size(AssistChipDefaults.IconSize), tint = tint) },
        shape = Stadium,
        colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent, labelColor = C.Text),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = C.Outline),
    )
}

/** Abas do Material 3 (com contador opcional). */
@Composable
fun Tabs(items: List<Pair<String, Int?>>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SecondaryTabRow(
        selectedTabIndex = selected, modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        containerColor = C.Card, contentColor = C.Gold,
        divider = { HorizontalDivider(color = C.Border) },
    ) {
        items.forEachIndexed { i, (title, count) ->
            Tab(
                selected = i == selected, onClick = { onSelect(i) },
                selectedContentColor = C.Gold, unselectedContentColor = C.Muted,
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = T.labelLg.copy(fontSize = 13.sp), maxLines = 1)
                        if (count != null) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                count.toString(),
                                Modifier.clip(CircleShape).background(if (i == selected) Color(0x33FACC15) else Color(0x1AFFFFFF)).padding(horizontal = 6.dp, vertical = 1.dp),
                                style = T.labelSm.copy(fontWeight = FontWeight.Bold), color = if (i == selected) C.Gold else C.Muted,
                            )
                        }
                    }
                },
            )
        }
    }
}

// ---------- Progresso e gráficos ----------

/** Barra de progresso do Material 3. */
@Composable
fun ProgressTrack(progress: Float, modifier: Modifier = Modifier, indeterminate: Boolean = false) {
    val shape = RoundedCornerShape(50)
    if (indeterminate || progress <= 0f) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth().height(8.dp).clip(shape), color = C.BlueLight, trackColor = C.CardHighest)
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) }, modifier = modifier.fillMaxWidth().height(8.dp).clip(shape),
            color = C.BlueLight, trackColor = C.CardHighest, gapSize = 0.dp, drawStopIndicator = {},
        )
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color = C.CyanPale) {
    Canvas(modifier.fillMaxWidth().height(64.dp)) {
        val data = if (values.size < 2) listOf(0f, 0f) else values
        val max = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val stepX = size.width / (data.size - 1)
        val pts = data.mapIndexed { i, v -> Offset(i * stepX, size.height - (v / max) * (size.height - 8.dp.toPx()) - 4.dp.toPx()) }
        for (k in 1..3) {
            val y = size.height * k / 4
            drawLine(Color(0x14FFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val cur = pts[i]
                val mx = (prev.x + cur.x) / 2
                cubicTo(mx, prev.y, mx, cur.y, cur.x, cur.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(C.Gold, 4.dp.toPx(), pts.last())
    }
}

// ---------- Imagens ----------

@Composable
fun Thumb(url: String?, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp), icon: ImageVector = Icons.Outlined.Image) {
    Box(modifier.clip(shape).background(C.CardHighest), contentAlignment = Alignment.Center) {
        if (url == null) Icon(icon, null, Modifier.size(24.dp), tint = C.Dim)
        else AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

// ---------- Diversos ----------

@Composable
fun HRow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable RowScope.() -> Unit) =
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically, content = content)

@Composable
fun Divider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = C.Border)

@Composable
fun Empty(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(C.Container), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(32.dp), tint = C.BluePale)
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(text, style = T.bodyMd, color = C.Muted, textAlign = TextAlign.Center)
    }
}

/** Círculo tonal com ícone, usado nos títulos de seção. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = C.BluePale, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(CircleShape).background(Color(0x1F1D4ED8)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = tint)
    }
}
