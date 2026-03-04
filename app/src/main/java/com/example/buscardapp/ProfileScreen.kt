package com.example.buscardapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

// Cores definidas em AppColors.kt

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()

    var isEditingProfile by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editFirstName    by remember { mutableStateOf("") }
    var editLastName     by remember { mutableStateOf("") }

    LaunchedEffect(uiState.userProfile) {
        editFirstName = uiState.userProfile?.firstName ?: ""
        editLastName  = uiState.userProfile?.lastName  ?: ""
    }

    val displayName = if (!uiState.userProfile?.firstName.isNullOrBlank())
        "${uiState.userProfile?.firstName} ${uiState.userProfile?.lastName}".trim()
    else "Utilizador"

    val initials = displayName.split(" ")
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    // ── Diálogo Apagar Conta ───────────────────────────────────────────────────
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                profileViewModel.deleteAccount { authViewModel.logout() }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // ── Diálogo Editar Perfil ──────────────────────────────────────────────────
    if (isEditingProfile) {
        EditProfileDialog(
            firstName     = editFirstName,
            lastName      = editLastName,
            onFirstChange = { editFirstName = it },
            onLastChange  = { editLastName  = it },
            onSave        = { profileViewModel.updateProfile(editFirstName, editLastName) { isEditingProfile = false } },
            onDismiss     = { isEditingProfile = false }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(BgDeep), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Cabeçalho com gradiente verde escuro ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0D3320), Color(0xFF0A1A12))),
                        RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(GreenPrimary.copy(0.2f), Color.Transparent)),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 52.dp, bottom = 60.dp)
            ) {
                Column {
                    Text(
                        text          = "PERFIL",
                        color         = GreenPrimary.copy(0.7f),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                                .border(2.dp, Color.White.copy(0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials.ifEmpty { "?" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(displayName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(uiState.userEmail, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── Stats sobrepostos ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-28).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    value    = "€${String.format("%.2f", uiState.userCard?.saldo ?: 0.0)}",
                    label    = "Saldo",
                    icon     = Icons.Default.AccountBalanceWallet,
                    accent   = GreenPrimary
                )
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${uiState.userCard?.tripsLeft ?: 0}",
                    label    = "Viagens",
                    icon     = Icons.Default.ConfirmationNumber,
                    accent   = BluePrimary
                )
            }

            // ── Definições ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-12).dp)
            ) {
                SectionLabel("DEFINIÇÕES")
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BgCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                ) {
                    Column {
                        // ── Editar Perfil ─────────────────────────────────────
                        ProfileRow(
                            icon     = Icons.Default.Person,
                            iconTint = GreenPrimary,
                            iconBg   = GreenPrimary.copy(0.15f),
                            label    = "Editar Perfil",
                            onClick  = { isEditingProfile = true }
                        )

                        RowDivider()

                        // ── Modo Escuro (botão de tema movido para aqui) ───────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileIconBadge(
                                icon = Icons.Default.DarkMode,
                                tint = Color(0xFFA855F7),
                                bg   = Color(0xFFA855F7).copy(0.15f)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text       = "Modo Escuro",
                                color      = TextPrimary,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier   = Modifier.weight(1f)
                            )
                            Switch(
                                checked         = isDarkMode,
                                onCheckedChange = onThemeChange,
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor   = Color.White,
                                    checkedTrackColor   = GreenPrimary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = InputBg
                                )
                            )
                        }

                        RowDivider()

                        // ── Terminar Sessão ───────────────────────────────────
                        ProfileRow(
                            icon     = Icons.Default.Logout,
                            iconTint = RedAccent,
                            iconBg   = RedAccent.copy(0.15f),
                            label    = "Terminar Sessão",
                            tint     = RedAccent,
                            onClick  = { authViewModel.logout() }
                        )

                        RowDivider()

                        // ── Apagar Conta ──────────────────────────────────────
                        ProfileRow(
                            icon        = Icons.Default.DeleteForever,
                            iconTint    = RedAccent,
                            iconBg      = RedAccent.copy(0.15f),
                            label       = "Apagar Conta",
                            tint        = RedAccent,
                            showChevron = false,
                            onClick     = { showDeleteDialog = true }
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileStatCard(modifier: Modifier, value: String, label: String, icon: ImageVector, accent: Color) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = accent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    tint: Color = TextPrimary,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileIconBadge(icon = icon, tint = iconTint, bg = iconBg)
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (showChevron) {
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileIconBadge(icon: ImageVector, tint: Color, bg: Color) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = SurfaceBorder)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        color         = TextSecondary,
        letterSpacing = 1.5.sp,
        modifier      = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

// ── Diálogo: Editar Perfil ─────────────────────────────────────────────────────
@Composable
private fun EditProfileDialog(
    firstName: String, lastName: String,
    onFirstChange: (String) -> Unit, onLastChange: (String) -> Unit,
    onSave: () -> Unit, onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BgCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Text("Editar Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value         = firstName,
                    onValueChange = onFirstChange,
                    label         = { Text("Primeiro Nome", color = TextSecondary) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenPrimary,
                        focusedLabelColor    = GreenPrimary,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        unfocusedBorderColor = SurfaceBorder,
                        cursorColor          = GreenPrimary,
                        unfocusedContainerColor = InputBg,
                        focusedContainerColor   = InputBg
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = lastName,
                    onValueChange = onLastChange,
                    label         = { Text("Apelido", color = TextSecondary) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenPrimary,
                        focusedLabelColor    = GreenPrimary,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        unfocusedBorderColor = SurfaceBorder,
                        cursorColor          = GreenPrimary,
                        unfocusedContainerColor = InputBg,
                        focusedContainerColor   = InputBg
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Button(
                        onClick  = onSave,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GreenPrimary, GreenDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Guardar", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Diálogo: Apagar Conta ──────────────────────────────────────────────────────
@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BgCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Ícone de aviso
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(RedAccent.copy(0.15f))
                        .border(1.dp, RedAccent.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = RedAccent, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Apagar Conta?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "Esta ação é permanente. Todos os teus dados serão eliminados: perfil, cartão e histórico de viagens.",
                    fontSize  = 13.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = RedAccent)
                    ) {
                        Text("Apagar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}