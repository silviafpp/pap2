package com.example.buscardapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Esquema Escuro — usa a paleta da app ──────────────────────────────────────
private val AppDarkColorScheme = darkColorScheme(
    primary          = Color(0xFF22C55E),   // GreenPrimary
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF16A34A),   // GreenDark
    secondary        = Color(0xFF3B82F6),   // BluePrimary
    onSecondary      = Color.White,
    background       = Color(0xFF0A0F14),   // BgDeep
    onBackground     = Color(0xFFF0F4F8),   // TextPrimary
    surface          = Color(0xFF111920),   // BgCard
    onSurface        = Color(0xFFF0F4F8),   // TextPrimary
    surfaceVariant   = Color(0xFF182130),   // InputBg
    onSurfaceVariant = Color(0xFF8A9BB0),   // TextSecondary
    outline          = Color(0xFF1E2D3D),   // SurfaceBorder
    error            = Color(0xFFEF4444),   // RedAccent
    onError          = Color.White,
)

// ── Esquema Claro — mantido simples caso seja necessário ──────────────────────
private val AppLightColorScheme = lightColorScheme(
    primary          = Color(0xFF16A34A),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF22C55E),
    secondary        = Color(0xFF3B82F6),
    onSecondary      = Color.White,
    background       = Color(0xFFF0F4F1),
    onBackground     = Color(0xFF1A2332),
    surface          = Color.White,
    onSurface        = Color(0xFF1A2332),
    surfaceVariant   = Color(0xFFF8FAF9),
    onSurfaceVariant = Color(0xFF6B7A8D),
    outline          = Color(0xFFE8EDF0),
    error            = Color(0xFFEF4444),
    onError          = Color.White,
)

@Composable
fun BusCardAppTheme(
    darkTheme: Boolean = false,  // false = tema claro por omissão; o utilizador pode mudar no Perfil
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    // ── Controla a cor da status bar e navigation bar do sistema ──────────────
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Fundo da status bar e navigation bar = mesmo fundo da app
            val bgColor = if (darkTheme) Color(0xFF0A0F14) else Color(0xFFF0F4F1)
            window.statusBarColor     = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()

            // Ícones da status bar e navigation bar: claros no dark, escuros no light
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars     = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}