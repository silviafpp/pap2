package com.example.buscardapp

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

// ── Cores ─────────────────────────────────────────────────────────────────────
private val Green700   = Color(0xFF006D4E)
private val Green800   = Color(0xFF004D36)
private val Green400   = Color(0xFF00C07A)
private val GreenLight = Color(0xFFE8F5EF)
private val Red500     = Color(0xFFEF4444)
private val RedLight   = Color(0xFFFFEEEE)

// ── Tema helper ───────────────────────────────────────────────────────────────
private data class ProfileColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceSecondary: Color,
    val divider: Color,
    val cardStat: Color
)

private fun profileColors(dark: Boolean) = ProfileColors(
    background          = if (dark) Color(0xFF0D1117) else Color(0xFFF0F4F1),
    surface             = if (dark) Color(0xFF1A2332) else Color.White,
    surfaceVariant      = if (dark) Color(0xFF243040) else Color(0xFFF8FAF9),
    onSurface           = if (dark) Color(0xFFECEFF4) else Color(0xFF1A2332),
    onSurfaceSecondary  = if (dark) Color(0xFF8A9BB0) else Color(0xFF6B7A8D),
    divider             = if (dark) Color(0xFF2C3E50) else Color(0xFFE8EDF0),
    cardStat            = if (dark) Color(0xFF1A2332) else Color.White
)

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val c = profileColors(isDarkMode)

    var isEditingProfile  by remember { mutableStateOf(false) }
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var editFirstName     by remember { mutableStateOf("") }
    var editLastName      by remember { mutableStateOf("") }

    // Sincronizar campos de edição com o perfil carregado
    LaunchedEffect(uiState.userProfile) {
        editFirstName = uiState.userProfile?.firstName ?: ""
        editLastName  = uiState.userProfile?.lastName  ?: ""
    }

    val displayName = if (!uiState.userProfile?.firstName.isNullOrBlank())
        "${uiState.userProfile?.firstName} ${uiState.userProfile?.lastName}".trim()
    else "Utilizador"

    val initials = displayName.split(" ")
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    // ── Diálogo de Confirmação: Apagar Conta ──────────────────────────────────
    if (showDeleteDialog) {
        DeleteAccountDialog(
            isDark    = isDarkMode,
            colors    = c,
            onConfirm = {
                showDeleteDialog = false
                profileViewModel.deleteAccount { authViewModel.logout() }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // ── Diálogo de Edição de Perfil ───────────────────────────────────────────
    if (isEditingProfile) {
        EditProfileDialog(
            isDark        = isDarkMode,
            colors        = c,
            firstName     = editFirstName,
            lastName      = editLastName,
            onFirstChange = { editFirstName = it },
            onLastChange  = { editLastName  = it },
            onSave        = {
                profileViewModel.updateProfile(editFirstName, editLastName) {
                    isEditingProfile = false
                }
            },
            onDismiss = { isEditingProfile = false }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green700)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Cabeçalho com gradiente ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Green700, Green800)),
                    RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 60.dp)
        ) {
            Column {
                Text(
                    text       = "Perfil",
                    color      = Color.White.copy(alpha = 0.7f),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar com inicial
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Green400, Color(0xFF00A86B)))
                            )
                            .border(2.dp, Color.White.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = initials.ifEmpty { "?" },
                            color      = Color.White,
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text       = displayName,
                            color      = Color.White,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email, null,
                                tint     = Color.White.copy(0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text     = uiState.userEmail,
                                color    = Color.White.copy(0.75f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Stats Cards sobrepostos ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-32).dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value    = String.format("%.2f€", uiState.userCard?.saldo ?: 0.0),
                label    = "Saldo",
                icon     = Icons.Default.AccountBalanceWallet,
                tint     = Green700,
                bgColor  = c.cardStat,
                isDark   = isDarkMode
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value    = "${uiState.userCard?.tripsLeft ?: 0}",
                label    = "Viagens",
                icon     = Icons.Default.ConfirmationNumber,
                tint     = Color(0xFF2563EB),
                bgColor  = c.cardStat,
                isDark   = isDarkMode
            )
        }

        // ── Secção Definições ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .offset(y = (-16).dp)
        ) {
            SectionLabel("DEFINIÇÕES", c)

            // Card de definições
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                color    = c.surface,
                shadowElevation = if (!isDarkMode) 4.dp else 0.dp,
                tonalElevation  = if (isDarkMode) 2.dp else 0.dp
            ) {
                Column {
                    // ── Editar Perfil ─────────────────────────────────────────
                    SettingsRow(
                        icon        = Icons.Default.Person,
                        iconTint    = Green700,
                        iconBg      = GreenLight,
                        label       = "Editar Perfil",
                        labelColor  = c.onSurface,
                        onClick     = { isEditingProfile = true }
                    )

                    SettingsDivider(c)

                    // ── Modo Escuro ───────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(
                            icon   = Icons.Default.DarkMode,
                            tint   = Color(0xFF7C3AED),
                            bg     = Color(0xFFF3EEFF)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text       = "Modo Escuro",
                            color      = c.onSurface,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier   = Modifier.weight(1f)
                        )
                        Switch(
                            checked         = isDarkMode,
                            onCheckedChange = onThemeChange,
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor       = Color.White,
                                checkedTrackColor       = Green700,
                                uncheckedThumbColor     = Color.White,
                                uncheckedTrackColor     = c.divider
                            )
                        )
                    }

                    SettingsDivider(c)

                    // ── Terminar Sessão ───────────────────────────────────────
                    SettingsRow(
                        icon       = Icons.Default.Logout,
                        iconTint   = Red500,
                        iconBg     = RedLight,
                        label      = "Terminar Sessão",
                        labelColor = Red500,
                        onClick    = { authViewModel.logout() }
                    )

                    SettingsDivider(c)

                    // ── Apagar Conta ──────────────────────────────────────────
                    SettingsRow(
                        icon       = Icons.Default.DeleteForever,
                        iconTint   = Red500,
                        iconBg     = RedLight,
                        label      = "Apagar Conta",
                        labelColor = Red500,
                        onClick    = { showDeleteDialog = true },
                        showChevron = false
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    bgColor: Color,
    isDark: Boolean
) {
    Surface(
        modifier        = modifier.height(110.dp),
        shape           = RoundedCornerShape(16.dp),
        color           = bgColor,
        shadowElevation = if (!isDark) 4.dp else 0.dp,
        tonalElevation  = if (isDark) 2.dp else 0.dp
    ) {
        Column(
            modifier             = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement  = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text       = value,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isDark) Color(0xFFECEFF4) else Color(0xFF1A2332)
            )
            Text(
                text     = label,
                fontSize = 12.sp,
                color    = tint
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    labelColor: Color,
    onClick: () -> Unit,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = iconTint, bg = iconBg)
        Spacer(Modifier.width(14.dp))
        Text(
            text       = label,
            color      = labelColor,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight, null,
                tint     = labelColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color, bg: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsDivider(c: ProfileColors) {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = c.divider
    )
}

@Composable
private fun SectionLabel(text: String, c: ProfileColors) {
    Text(
        text          = text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        color         = c.onSurfaceSecondary,
        letterSpacing = 1.2.sp,
        modifier      = Modifier.padding(bottom = 10.dp, start = 4.dp)
    )
}

// ── Diálogo: Editar Perfil ────────────────────────────────────────────────────
@Composable
private fun EditProfileDialog(
    isDark: Boolean,
    colors: ProfileColors,
    firstName: String,
    lastName: String,
    onFirstChange: (String) -> Unit,
    onLastChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = if (isDark) 4.dp else 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text       = "Editar Perfil",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.onSurface
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value         = firstName,
                    onValueChange = onFirstChange,
                    label         = { Text("Primeiro Nome") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Green700,
                        focusedLabelColor    = Green700,
                        focusedTextColor     = colors.onSurface,
                        unfocusedTextColor   = colors.onSurface,
                        unfocusedBorderColor = colors.divider,
                        cursorColor          = Green700
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = lastName,
                    onValueChange = onLastChange,
                    label         = { Text("Apelido") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Green700,
                        focusedLabelColor    = Green700,
                        focusedTextColor     = colors.onSurface,
                        unfocusedTextColor   = colors.onSurface,
                        unfocusedBorderColor = colors.divider,
                        cursorColor          = Green700
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, colors.divider)
                    ) {
                        Text("Cancelar", color = colors.onSurfaceSecondary)
                    }
                    Button(
                        onClick  = onSave,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Green700)
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Diálogo: Apagar Conta ─────────────────────────────────────────────────────
@Composable
private fun DeleteAccountDialog(
    isDark: Boolean,
    colors: ProfileColors,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = RoundedCornerShape(24.dp),
            color          = colors.surface,
            tonalElevation = if (isDark) 4.dp else 0.dp
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone de aviso
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(RedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        null,
                        tint     = Red500,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text       = "Apagar Conta?",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "Esta ação é permanente e irá eliminar todos os teus dados, incluindo o cartão e histórico de viagens.",
                    fontSize  = 13.sp,
                    color     = colors.onSurfaceSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, colors.divider)
                    ) {
                        Text("Cancelar", color = colors.onSurfaceSecondary)
                    }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Red500)
                    ) {
                        Text("Apagar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}