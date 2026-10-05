package com.subarudl.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.subarudl.app.R

/** Cores do design "Rally Precision Dark" (Stitch). */
object C {
    val Bg = Color(0xFF0B0F18)
    val Surface = Color(0xFF0F131C)
    val Card = Color(0xFF151A26)
    val CardHigh = Color(0xFF1C2230)
    val CardHighest = Color(0xFF262C3A)
    val Field = Color(0xFF090D16)
    val Border = Color(0x14FFFFFF)
    val BorderStrong = Color(0x26FFFFFF)
    val Text = Color(0xFFF1F4FB)
    val Muted = Color(0xFF94A3B8)
    val Dim = Color(0xFF64748B)

    val Blue = Color(0xFF1D4ED8)
    val BlueLight = Color(0xFF2563EB)
    val BluePale = Color(0xFFB7C4FF)
    val Gold = Color(0xFFFACC15)
    val GoldDeep = Color(0xFFEEC200)
    val OnGold = Color(0xFF241B00)
    val CyanPale = Color(0xFF7BD0FF)
    val Error = Color(0xFFFFB4AB)
    val ErrorStrong = Color(0xFFF87171)
    val Success = Color(0xFF4ADE80)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val SpaceGrotesk = FontFamily(
    variable(R.font.spacegrotesk, 400), variable(R.font.spacegrotesk, 500), variable(R.font.spacegrotesk, 600), variable(R.font.spacegrotesk, 700),
)
val Manrope = FontFamily(
    variable(R.font.manrope, 400), variable(R.font.manrope, 500), variable(R.font.manrope, 600), variable(R.font.manrope, 700),
)
val Mono = FontFamily(
    variable(R.font.jetbrainsmono, 400), variable(R.font.jetbrainsmono, 500), variable(R.font.jetbrainsmono, 600), variable(R.font.jetbrainsmono, 700),
)

object T {
    val headlineLg = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp, color = C.Text)
    val headlineMd = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp, color = C.Text)
    val headlineSm = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = C.Text)
    val title = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, color = C.Text)
    val button = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 0.3.sp, color = C.Text)
    val bodyLg = TextStyle(fontFamily = Manrope, fontSize = 16.sp, lineHeight = 24.sp, color = C.Text)
    val bodyMd = TextStyle(fontFamily = Manrope, fontSize = 15.sp, lineHeight = 22.sp, color = C.Text)
    val bodySm = TextStyle(fontFamily = Manrope, fontSize = 13.sp, lineHeight = 19.sp, color = C.Muted)
    val labelLg = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp, color = C.Text)
    val labelMd = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.7.sp, color = C.Muted)
    val labelSm = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp, color = C.Muted)
}

/** O app é só escuro, como o do PC. */
@Composable
fun SubaruTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = C.BluePale, onPrimary = Color(0xFF002682), primaryContainer = C.Blue, onPrimaryContainer = Color(0xFFCAD3FF),
        secondary = C.Gold, onSecondary = C.OnGold, secondaryContainer = C.GoldDeep, onSecondaryContainer = Color(0xFF645000),
        tertiary = C.CyanPale, background = C.Surface, onBackground = C.Text, surface = C.Surface, onSurface = C.Text,
        surfaceVariant = C.CardHighest, onSurfaceVariant = C.Muted, surfaceContainer = C.CardHigh, surfaceContainerHigh = C.CardHigh,
        surfaceContainerHighest = C.CardHighest, surfaceContainerLow = C.Card, surfaceContainerLowest = C.Field,
        outline = Color(0xFF8E90A0), outlineVariant = Color(0xFF434655), error = C.Error, onError = Color(0xFF690005),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            bodyLarge = T.bodyLg, bodyMedium = T.bodyMd, bodySmall = T.bodySm,
            titleLarge = T.headlineSm, titleMedium = T.title, labelLarge = T.labelLg, labelMedium = T.labelMd, labelSmall = T.labelSm,
        ),
        content = content,
    )
}
