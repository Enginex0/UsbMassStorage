package com.enginex0.usbmassstorage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.enginex0.usbmassstorage.data.AccentColor

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D64),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFF80DEEA),
    onSecondary = Color(0xFF003D43),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE1E1E1),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFE1E1E1),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0288D1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB3E5FC),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF00838F),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFEFEFE),
    onSurface = Color(0xFF1C1C1C),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1C1C),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF)
)

private val AlmostBlackDark = darkColorScheme(
    primary = Color(0xFFBBBBBB),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF888888),
    onSecondary = Color(0xFF000000),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF0A0A0A),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

private val AlmostBlackLight = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF333333),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFF0F0F0),
    onSurface = Color(0xFF0A0A0A),
    background = Color(0xFFE8E8E8),
    onBackground = Color(0xFF0A0A0A),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF)
)

private val WhiteDark = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF1A1A1A),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF5F5F5),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F5F5),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1E1E1E)
)

private val WhiteLight = lightColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun UsbMassStorageTheme(
    accent: AccentColor = AccentColor.SYSTEM_DEFAULT,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val colorScheme = when (accent) {
        AccentColor.SYSTEM_DEFAULT -> when {
            Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(LocalContext.current)
            Build.VERSION.SDK_INT >= 31 && !dark -> dynamicLightColorScheme(LocalContext.current)
            dark -> DarkColorScheme
            else -> LightColorScheme
        }
        AccentColor.ALMOST_BLACK -> if (dark) AlmostBlackDark else AlmostBlackLight
        AccentColor.WHITE -> if (dark) WhiteDark else WhiteLight
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
