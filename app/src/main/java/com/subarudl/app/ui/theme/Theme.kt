package com.subarudl.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** As três cores Subaru. */
object Subaru {
    val Marinho = Color(0xFF0C1430)
    val Amarelo = Color(0xFFFFD200)
    val Dourado = Color(0xFFD9B44A)
}

/**
 * Esquema escuro do Material 3 montado com as cores Subaru: o chão é o azul-marinho e as camadas sobem no mesmo tom;
 * o amarelo é a cor principal (colar, progresso, seleção) e o dourado marca o que terminou.
 */
private val Esquema = darkColorScheme(
    primary = Subaru.Amarelo, onPrimary = Color(0xFF3A2F00),
    primaryContainer = Color(0xFF574600), onPrimaryContainer = Color(0xFFFFE37F),
    inversePrimary = Color(0xFF725D00),
    secondary = Color(0xFFBCC5EB), onSecondary = Color(0xFF262F4E),
    secondaryContainer = Color(0xFF2D3960), onSecondaryContainer = Color(0xFFDCE1FF),
    tertiary = Subaru.Dourado, onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF574400), onTertiaryContainer = Color(0xFFFFE08E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Subaru.Marinho, onBackground = Color(0xFFE2E6F4),
    surface = Subaru.Marinho, onSurface = Color(0xFFE2E6F4),
    surfaceVariant = Color(0xFF283358), onSurfaceVariant = Color(0xFFC3C9DE),
    surfaceTint = Color(0xFFB4C3FF),
    inverseSurface = Color(0xFFE2E6F4), inverseOnSurface = Color(0xFF283050),
    outline = Color(0xFF8D94AE), outlineVariant = Color(0xFF3A4468),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2D3860), surfaceDim = Subaru.Marinho,
    surfaceContainerLowest = Color(0xFF070D22),
    surfaceContainerLow = Color(0xFF111A39),
    surfaceContainer = Color(0xFF162042),
    surfaceContainerHigh = Color(0xFF1D284D),
    surfaceContainerHighest = Color(0xFF253159),
)

/** Algarismos de largura igual para tamanhos, velocidades e tempos: não "tremem" enquanto mudam. */
val TextStyle.tabular: TextStyle get() = copy(fontFeatureSettings = "tnum")

/** O app é só escuro. A letra é a Roboto do sistema, na escala do Material 3. */
@Composable
fun SubaruTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, typography = Typography(), content = content)
}
