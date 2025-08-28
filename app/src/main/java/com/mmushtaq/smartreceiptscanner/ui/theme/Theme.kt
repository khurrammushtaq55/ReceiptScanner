package com.mmushtaq.smartreceiptscanner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Brand palettes used when dynamic color isn't available (or you disable it)
private val LightColors = lightColorScheme(
    primary = md_seed,            // brand color
    onPrimary = md_onSeed,
    secondary = md_secondary,
    onSecondary = md_onSecondary,
    background = md_bg_light,
    onBackground = md_onBg_light,
    surface = md_surface_light,
    onSurface = md_onSurface_light
)

private val DarkColors = darkColorScheme(
    primary = md_seed_dark,
    onPrimary = md_onSeed_dark,
    secondary = md_secondary_dark,
    onSecondary = md_onSecondary_dark,
    background = md_bg_dark,
    onBackground = md_onBg_dark,
    surface = md_surface_dark,
    onSurface = md_onSurface_dark
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val colors =
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDarkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            if (useDarkTheme) DarkColors else LightColors
        }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
