package ca.rosebankgospelhall.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Burgundy = Color(0xFF6E2536)
val BurgundyDeep = Color(0xFF4A1623)
val Gold = Color(0xFFE8C36A)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A2E3E), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DF), onPrimaryContainer = Color(0xFF3A0714),
    secondary = Color(0xFF5B6770), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E9ED), onSecondaryContainer = Color(0xFF161D22),
    tertiary = Color(0xFF7D5F12), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2A0), onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFCFAF9), onBackground = Color(0xFF211A1B),
    surface = Color(0xFFFCFAF9), onSurface = Color(0xFF211A1B),
    surfaceVariant = Color(0xFFF2E6E8), onSurfaceVariant = Color(0xFF524345),
    outline = Color(0xFF857375), outlineVariant = Color(0xFFD8C2C4),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF8F2F1),
    surfaceContainer = Color(0xFFF3EDEC), surfaceContainerHigh = Color(0xFFEDE6E5),
    surfaceContainerHighest = Color(0xFFE7E0DF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2B8C2), onPrimary = Color(0xFF4A1325),
    primaryContainer = Color(0xFF63283A), onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFBFC8CE), onSecondary = Color(0xFF293238),
    secondaryContainer = Color(0xFF3F484E), onSecondaryContainer = Color(0xFFDBE3E9),
    tertiary = Color(0xFFE5C26B), onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4400), onTertiaryContainer = Color(0xFFFFE2A0),
    background = Color(0xFF171213), onBackground = Color(0xFFEDE0E1),
    surface = Color(0xFF171213), onSurface = Color(0xFFEDE0E1),
    surfaceVariant = Color(0xFF524345), onSurfaceVariant = Color(0xFFD7C1C3),
    outline = Color(0xFFA08C8E), outlineVariant = Color(0xFF524345),
    surfaceContainerLowest = Color(0xFF120D0E), surfaceContainerLow = Color(0xFF1F191A),
    surfaceContainer = Color(0xFF251E1F), surfaceContainerHigh = Color(0xFF2F2829),
    surfaceContainerHighest = Color(0xFF3A3334),
)

private val base = Typography()

private val AppTypography = Typography(
    headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun RosebankTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
