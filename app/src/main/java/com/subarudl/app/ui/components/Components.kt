package com.subarudl.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.subarudl.app.ui.theme.C
import com.subarudl.app.ui.theme.T

// ---------- Cartões ----------

@Composable
fun SCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = C.Card,
        border = BorderStroke(1.dp, C.Border),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Box {
            Column(Modifier.padding(start = if (accent != null) padding + 3.dp else padding, end = padding, top = padding, bottom = padding), content = content)
            if (accent != null) Box(Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight().background(accent))
        }
    }
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier, color: Color = C.Muted, style: TextStyle = T.labelSm, maxLines: Int = 1) {
    Text(text, modifier, color = color, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = C.Muted) {
    Text(text.uppercase(), modifier, color = color, style = T.labelMd.copy(letterSpacing = 1.sp))
}

// ---------- Selos ----------

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = C.Gold, background: Color = Color(0x26FACC15), border: Color = Color(0x4DFACC15)) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(background).border(1.dp, border, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
        color = color, style = T.labelSm.copy(fontSize = 11.sp), maxLines = 1,
    )
}

@Composable
fun BlueBadge(text: String, modifier: Modifier = Modifier) = Badge(text, modifier, color = C.BluePale, background = Color(0x331D4ED8), border = Color(0x4D3B82F6))

@Composable
fun DarkBadge(text: String, color: Color = C.Gold, modifier: Modifier = Modifier) =
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xCC000000)).padding(horizontal = 7.dp, vertical = 3.dp),
        color = color, style = T.labelSm.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold), maxLines = 1,
    )

// ---------- Botões ----------

@Composable
fun BtnPrimary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = 52.dp) {
    Row(
        modifier.height(height).clip(RoundedCornerShape(8.dp))
            .background(if (enabled) C.Blue else C.Blue.copy(alpha = 0.35f))
            .border(1.dp, Color(0x663B82F6), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = T.button, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun BtnGold(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, tag: String? = null, enabled: Boolean = true) {
    Row(
        modifier.height(54.dp).clip(RoundedCornerShape(8.dp)).background(if (enabled) C.GoldDeep else C.GoldDeep.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp), tint = C.OnGold)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = T.button, color = C.OnGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (tag != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                tag, Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x33000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                style = T.labelSm.copy(fontSize = 11.sp), color = C.OnGold,
            )
        }
    }
}

@Composable
fun BtnGhost(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = C.Text, enabled: Boolean = true, height: Dp = 46.dp) {
    Row(
        modifier.height(height).clip(RoundedCornerShape(8.dp)).background(Color(0x0DFFFFFF)).border(1.dp, C.Border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = if (enabled) color else C.Dim)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = T.labelLg, color = if (enabled) color else C.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.Text, background: Color = Color(0x14FFFFFF), size: Dp = 40.dp, description: String? = null) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(8.dp)).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(size * 0.5f), tint = tint) }
}

// ---------- Controles ----------

@Composable
fun SwitchSubaru(checked: Boolean, onChange: (Boolean) -> Unit, gold: Boolean = false) {
    val track by animateColorAsState(if (checked) (if (gold) C.Gold else Color(0xFFB7C4FF)) else C.CardHighest, label = "track")
    val thumb = if (checked) (if (gold) Color(0xFF3C2F00) else Color(0xFF1D3FB8)) else C.Muted
    Box(
        Modifier.width(52.dp).height(30.dp).clip(CircleShape).background(track).clickable { onChange(!checked) }.padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(if (checked) 24.dp else 18.dp).clip(CircleShape).background(thumb)) }
}

@Composable
fun CheckSubaru(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(5.dp))
            .background(if (checked) C.Blue else C.Field)
            .border(1.5.dp, if (checked) C.BluePale.copy(alpha = 0.6f) else C.BorderStrong, RoundedCornerShape(5.dp))
            .clickable { onChange(!checked) },
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(Icons.Filled.Check, null, Modifier.size(size * 0.72f), tint = Color.White) }
}

data class Seg<T>(val value: T, val label: String, val icon: ImageVector? = null)

@Composable
fun <T> Segmented(options: List<Seg<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Field).border(1.dp, C.Border, RoundedCornerShape(10.dp)).padding(4.dp)) {
        options.forEach { o ->
            val on = o.value == selected
            Row(
                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(7.dp)).background(if (on) C.Blue else Color.Transparent).clickable { onSelect(o.value) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                if (o.icon != null) {
                    Icon(o.icon, null, Modifier.size(19.dp), tint = if (on) Color.White else C.Muted)
                    Spacer(Modifier.width(8.dp))
                }
                Text(o.label, style = T.labelLg, color = if (on) Color.White else C.Muted, maxLines = 1)
            }
        }
    }
}

data class Opt<T>(val value: T, val label: String, val sub: String? = null)

/** Caixa que mostra o valor escolhido e abre uma lista (o "campo de seleção" do design). */
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
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(8.dp)).background(C.Field).border(1.dp, C.Border, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, null, Modifier.size(20.dp), tint = C.Muted)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                (prefix ?: "") + (current?.label ?: placeholder),
                Modifier.weight(1f), style = T.labelLg.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) C.Text else C.Dim, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ExpandMore, null, Modifier.size(22.dp), tint = C.Muted)
        }
        DropdownMenu(open, { open = false }, containerColor = C.CardHigh, modifier = Modifier.heightIn(max = maxMenuHeight)) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(o.label, style = T.labelLg.copy(fontWeight = FontWeight.Medium), color = if (o.value == value) C.Gold else C.Text)
                            if (o.sub != null) Text(o.sub, style = T.labelSm, color = C.Muted)
                        }
                    },
                    onClick = { open = false; onSelect(o.value) },
                )
            }
        }
    }
}

@Composable
fun ChipSmall(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.clip(RoundedCornerShape(6.dp)).background(if (selected) Color(0x33FACC15) else Color(0x14FFFFFF))
            .border(1.dp, if (selected) Color(0x66FACC15) else C.Border, RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        style = T.labelMd.copy(fontWeight = FontWeight.SemiBold), color = if (selected) C.Gold else C.Muted,
    )
}

@Composable
fun SiteChip(text: String, icon: ImageVector, tint: Color = C.Muted) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x0DFFFFFF)).border(1.dp, C.Border, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(17.dp), tint = tint)
        Spacer(Modifier.width(7.dp))
        Text(text, style = T.labelMd, color = C.Text, maxLines = 1)
    }
}

@Composable
fun RowScope.TabPill(text: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) C.Blue else Color.Transparent).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = T.labelLg.copy(fontSize = 13.sp), color = if (selected) Color.White else C.Muted, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) Color(0x33FFFFFF) else Color(0x1AFFFFFF)).padding(horizontal = 7.dp, vertical = 1.dp),
                style = T.labelSm, color = if (selected) Color.White else C.Muted,
            )
        }
    }
}

// ---------- Progresso e gráficos ----------

/** Barra azul com o marcador dourado, como no design. */
@Composable
fun ProgressTrack(progress: Float, modifier: Modifier = Modifier, indeterminate: Boolean = false) {
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val r = size.height / 2
        drawRoundRect(Color(0x1FFFFFFF), cornerRadius = CornerRadius(r, r))
        if (indeterminate || p <= 0f) return@Canvas
        val w = size.width * p
        drawRoundRect(Brush.horizontalGradient(listOf(C.Blue, C.BlueLight)), size = Size(w, size.height), cornerRadius = CornerRadius(r, r))
        if (w > 12.dp.toPx()) drawRoundRect(C.Gold, topLeft = Offset(w - 7.dp.toPx(), -1.dp.toPx()), size = Size(7.dp.toPx(), size.height + 2.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color = C.CyanPale) {
    Canvas(modifier.fillMaxWidth().height(64.dp)) {
        val data = if (values.size < 2) listOf(0f, 0f) else values
        val max = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val stepX = size.width / (data.size - 1)
        val pts = data.mapIndexed { i, v -> Offset(i * stepX, size.height - (v / max) * (size.height - 8.dp.toPx()) - 4.dp.toPx()) }
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
fun Thumb(url: String?, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(6.dp), icon: ImageVector = Icons.Outlined.Image) {
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
fun Divider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(C.Border))

@Composable
fun Empty(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) Icon(icon, null, Modifier.size(34.dp), tint = C.Dim)
        Spacer(Modifier.height(8.dp))
        Text(text, style = T.bodySm, color = C.Muted)
    }
}
