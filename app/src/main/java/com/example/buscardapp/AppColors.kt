package com.example.buscardapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ── Toggle global — atualizado pela MainActivity ──────────────────────────────
var isDarkTheme by mutableStateOf(false)

// ── Cores dinâmicas (recompõem automaticamente com o tema) ────────────────────
val BgDeep        get() = if (isDarkTheme) Color(0xFF0A0F0D) else Color(0xFFF8FAFC)
val BgCard        get() = if (isDarkTheme) Color(0xFF111A14) else Color(0xFFFFFFFF)
val InputBg       get() = if (isDarkTheme) Color(0xFF1A2420) else Color(0xFFF1F5F9)
val InputBorder   get() = if (isDarkTheme) Color(0xFF2D3F35) else Color(0xFFE2E8F0)
val SurfaceBorder get() = if (isDarkTheme) Color(0xFF1E2D24) else Color(0xFFE8ECF0)
val TextPrimary   get() = if (isDarkTheme) Color(0xFFE8F5EC) else Color(0xFF0F172A)
val TextSecondary get() = if (isDarkTheme) Color(0xFF6B8F76) else Color(0xFF64748B)

// ── Cores estáticas ───────────────────────────────────────────────────────────
val GreenPrimary = Color(0xFF22C55E)
val GreenDark    = Color(0xFF16A34A)
val BluePrimary  = Color(0xFF3B82F6)
val RedAccent    = Color(0xFFEF4444)
val ErrorColor   = Color(0xFFEF4444)