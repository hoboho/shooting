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
    background = Color(0xFFF7F9F7),
    onBackground = Color(0xFF17201D),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF17201D),
    surfaceVariant = Color(0xFFE4EAE5),
    onSurfaceVariant = Color(0xFF4B5650),
    outline = Color(0xFF728078),
    outlineVariant = Color(0xFFD1D9D3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DE1CA),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF075443),
    onPrimaryContainer = Color(0xFFD8F2E9),
    secondary = Color(0xFF94D2DE),
    onSecondary = Color(0xFF00353D),
    secondaryContainer = Color(0xFF1F4D57),
    onSecondaryContainer = TealContainer,
    tertiary = Color(0xFFFFB4AB),
    onTertiary = Color(0xFF690005),
    background = Color(0xFF101614),
    onBackground = Color(0xFFDEE7E1),
    surface = Color(0xFF171E1B),
    onSurface = Color(0xFFDEE7E1),
    surfaceVariant = Color(0xFF414B46),
    onSurfaceVariant = Color(0xFFC0CAC3),
    outline = Color(0xFF89958E),
    outlineVariant = Color(0xFF414B46),
)

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val ShootingTypography = Typography(
    displayLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 38.sp, letterSpacing = (-0.35).sp),
    headlineMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 34.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val ShootingShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ShootingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
