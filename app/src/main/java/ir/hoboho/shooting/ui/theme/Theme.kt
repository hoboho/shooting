package ir.hoboho.shooting.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hoboho.shooting.R

/**
 * Shooting's visual identity, reproduced from the Metra design reference:
 * deep green as the working colour, teal as secondary, a warm off-white surface,
 * Vazirmatn for Persian typography and generous corner radii. Every screen reads
 * these tokens from the theme, so no screen invents its own style.
 */
private val GreenPrimary = Color(0xFF176B5B)
private val GreenContainer = Color(0xFFD8F2E9)
private val GreenOnContainer = Color(0xFF00382E)
private val TealSecondary = Color(0xFF2E7D8A)
private val TealContainer = Color(0xFFCDE7EC)
private val TealOnContainer = Color(0xFF00353D)
private val RedTertiary = Color(0xFFB63B35)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealOnContainer,
    tertiary = RedTertiary,
    onTertiary = Color.White,
    background = Color(0xFFF7F9F7, lineBreak = NoWordBreak),
    onBackground = Color(0xFF17201D, lineBreak = NoWordBreak),
    surface = Color(0xFFFFFEFC, lineBreak = NoWordBreak),
    onSurface = Color(0xFF17201D, lineBreak = NoWordBreak),
    surfaceVariant = Color(0xFFE4EAE5, lineBreak = NoWordBreak),
    onSurfaceVariant = Color(0xFF4B5650, lineBreak = NoWordBreak),
    outline = Color(0xFF728078, lineBreak = NoWordBreak),
    outlineVariant = Color(0xFFD1D9D3, lineBreak = NoWordBreak),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DE1CA, lineBreak = NoWordBreak),
    onPrimary = Color(0xFF00382E, lineBreak = NoWordBreak),
    primaryContainer = Color(0xFF075443, lineBreak = NoWordBreak),
    onPrimaryContainer = Color(0xFFD8F2E9, lineBreak = NoWordBreak),
    secondary = Color(0xFF94D2DE, lineBreak = NoWordBreak),
    onSecondary = Color(0xFF00353D, lineBreak = NoWordBreak),
    secondaryContainer = Color(0xFF1F4D57, lineBreak = NoWordBreak),
    onSecondaryContainer = TealContainer,
    tertiary = Color(0xFFFFB4AB, lineBreak = NoWordBreak),
    onTertiary = Color(0xFF690005, lineBreak = NoWordBreak),
    background = Color(0xFF101614, lineBreak = NoWordBreak),
    onBackground = Color(0xFFDEE7E1, lineBreak = NoWordBreak),
    surface = Color(0xFF171E1B, lineBreak = NoWordBreak),
    onSurface = Color(0xFFDEE7E1, lineBreak = NoWordBreak),
    surfaceVariant = Color(0xFF414B46, lineBreak = NoWordBreak),
    onSurfaceVariant = Color(0xFFC0CAC3, lineBreak = NoWordBreak),
    outline = Color(0xFF89958E, lineBreak = NoWordBreak),
    outlineVariant = Color(0xFF414B46, lineBreak = NoWordBreak),
)

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal, lineBreak = NoWordBreak),
    Font(R.font.vazirmatn_medium, FontWeight.Medium, lineBreak = NoWordBreak),
    Font(R.font.vazirmatn_bold, FontWeight.Bold, lineBreak = NoWordBreak),
)

private val NoWordBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.None)
private val ShootingTypography = Typography(
    displayLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp, lineBreak = NoWordBreak),
    headlineLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 38.sp, letterSpacing = (-0.35).sp, lineBreak = NoWordBreak),
    headlineMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 34.sp, letterSpacing = (-0.25).sp, lineBreak = NoWordBreak),
    headlineSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 30.sp, lineBreak = NoWordBreak),
    titleLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 28.sp, lineBreak = NoWordBreak),
    titleMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, lineBreak = NoWordBreak),
    titleSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, lineBreak = NoWordBreak),
    bodyLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 27.sp, lineBreak = NoWordBreak),
    bodyMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 23.sp, lineBreak = NoWordBreak),
    bodySmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp, lineBreak = NoWordBreak),
    labelLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, lineBreak = NoWordBreak),
    labelMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, lineBreak = NoWordBreak),
    labelSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, lineBreak = NoWordBreak),
)

private val ShootingShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp, lineBreak = NoWordBreak),
    small = RoundedCornerShape(14.dp, lineBreak = NoWordBreak),
    medium = RoundedCornerShape(20.dp, lineBreak = NoWordBreak),
    large = RoundedCornerShape(26.dp, lineBreak = NoWordBreak),
    extraLarge = RoundedCornerShape(32.dp, lineBreak = NoWordBreak),
)

@Composable
fun ShootingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(, lineBreak = NoWordBreak),
    content: @Composable () -> Unit,
) {
    // Persian-only app: pin RTL, otherwise an English-locale device mirrors
    // every icon, padding and navigation item.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ShootingTypography,
            shapes = ShootingShapes,
            content = content,
        )
    }
}
