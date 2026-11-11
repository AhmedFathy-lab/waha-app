package com.waha.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Emerald, onPrimary = White,
    primaryContainer = Mint, onPrimaryContainer = DarkGreen,
    secondary = EmeraldDark, onSecondary = White,
    background = MintSoft, onBackground = InkDark,
    surface = White, onSurface = InkDark,
    surfaceVariant = Mint, onSurfaceVariant = EmeraldDark,
)
private val Dark = darkColorScheme(
    primary = Mint, onPrimary = DarkGreen,
    primaryContainer = EmeraldDark, onPrimaryContainer = Mint,
    secondary = Emerald, onSecondary = White,
    background = DarkGreen, onBackground = MintSoft,
    surface = NightSurface, onSurface = MintSoft,
    surfaceVariant = NightCard, onSurfaceVariant = Mint,
)

val WahaTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 12.sp),
)

/** خلفية التدرج النعناعي/الداكن المستخدمة في كل الشاشات. */
@Composable
fun wahaBackground(): Brush = if (MaterialTheme.colorScheme.background == DarkGreen)
    Brush.verticalGradient(listOf(DarkGreen, NightSurface))
else Brush.verticalGradient(listOf(Mint, MintSoft, White))

@Composable
fun WahaTheme(dark: Boolean? = null, content: @Composable () -> Unit) {
    val useDark = dark ?: isSystemInDarkTheme()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (useDark) Dark else Light,
            typography = WahaTypography,
            content = content,
        )
    }
}
