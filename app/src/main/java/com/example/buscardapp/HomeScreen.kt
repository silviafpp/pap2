package com.example.buscardapp

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ── Paleta ───────────────────────────────────────────────────────────────────
private val BgDeep        = Color(0xFF0A0F14)
private val BgCard        = Color(0xFF111920)
private val GreenPrimary  = Color(0xFF22C55E)
private val GreenDark     = Color(0xFF16A34A)
private val TextPrimary   = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF8A9BB0)
private val InputBg       = Color(0xFF182130)
private val SurfaceBorder = Color(0xFF1E2D3D)
private val BluePrimary   = Color(0xFF3B82F6)

// ── Estados do fluxo de criação de cartão ─────────────────────────────────────
private enum class CardFlowStep {
    NONE,           // nada aberto
    CHOOSE_TYPE,    // escolher físico ou digital
    CHOOSE_PASS,    // escolher tipo de passe digital
    SCAN_NFC        // aguardar scan do cartão físico
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onCardClick: () -> Unit = {},
    physicalCardUid: String? = null,
    onPhysicalCardConsumed: () -> Unit = {},
    userViewModel: UserViewModel = viewModel()
) {
    var userProfile    by remember { mutableStateOf<UserProfile?>(null) }
    var userCard       by remember { mutableStateOf<UserCard?>(null) }
    var isLoading      by remember { mutableStateOf(true) }
    var showCardWallet by remember { mutableStateOf(false) }
    var showTopUp      by remember { mutableStateOf(false) }
    var cardFlowStep   by remember { mutableStateOf(CardFlowStep.NONE) }

    val scope           = rememberCoroutineScope()
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val topUpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cardState       by userViewModel.cardState.collectAsState()

    // ── Carregar dados ────────────────────────────────────────────────────────
    fun carregarDados() {
        scope.launch {
            isLoading = true
            try {
                val user = SupabaseClient.supabase.auth.currentUserOrNull()
                if (user != null) {
                    userProfile = SupabaseClient.supabase.postgrest["profiles"]
                        .select { filter { eq("id", user.id) } }
                        .decodeSingleOrNull<UserProfile>()
                    userCard = SupabaseClient.supabase.postgrest["user_cards"]
                        .select { filter { eq("user_id", user.id) } }
                        .decodeSingleOrNull<UserCard>()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { carregarDados() }

    // Recarrega quando cartão é criado com sucesso
    LaunchedEffect(cardState) {
        if (cardState is CardState.Success) {
            carregarDados()
            userViewModel.clearState()
        }
    }

    // ── Quando a MainActivity detecta um cartão NFC físico ────────────────────
    LaunchedEffect(physicalCardUid) {
        if (physicalCardUid != null && cardFlowStep == CardFlowStep.SCAN_NFC) {
            cardFlowStep = CardFlowStep.NONE
            userViewModel.registarCartaoFisico(physicalCardUid) {
                carregarDados()
                onPhysicalCardConsumed()
            }
        }
    }

    // ── Glow animado ──────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Box(
            modifier = Modifier.size(300.dp).offset((-50).dp, (-100).dp).blur(130.dp)
                .background(GreenPrimary.copy(alpha = glowPulse * 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (!userProfile?.firstName.isNullOrEmpty())
                            "Bem-vindo, ${userProfile?.firstName}" else "Bem-vindo",
                        color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                    Text("A sua viagem começa aqui", color = TextSecondary, fontSize = 14.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botão editar cartão (só com cartão ativo)
                    AnimatedVisibility(
                        visible = userProfile?.hasCard == true && !isLoading,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(GreenPrimary.copy(0.15f))
                                .border(1.dp, GreenPrimary.copy(0.35f), RoundedCornerShape(14.dp))
                                .clickable { showTopUp = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    // Sol
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(InputBg).border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Cartão ────────────────────────────────────────────────────
            AnimatedContent(
                targetState = Triple(isLoading, userProfile?.hasCard, cardState is CardState.Loading),
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "cardTransition"
            ) { (loading, hasCard, creating) ->
                when {
                    loading || creating -> ShimmerCard()
                    hasCard == true -> ActiveCard(
                        profile = userProfile,
                        card = userCard,
                        glowPulse = glowPulse,
                        isValid = userViewModel.isCardValid(userCard, userProfile?.cardType),
                        onCardClick = { showCardWallet = true }
                    )
                    else -> EmptyCard(onClick = { cardFlowStep = CardFlowStep.CHOOSE_TYPE })
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Stats ─────────────────────────────────────────────────────
            AnimatedVisibility(visible = userProfile?.hasCard == true && !isLoading) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val (statVal, statLabel) = when (userProfile?.cardType) {
                            "SEMANAL" -> "${userCard?.tripsLeft ?: 0}" to "Viagens restantes"
                            "DIARIO"  -> "€${String.format("%.2f", userCard?.saldo ?: 0.0)}" to "Saldo disponível"
                            "MENSAL"  -> "∞" to "Viagens ilimitadas"
                            else      -> "—" to "—"
                        }
                        StatCard(Modifier.weight(1f), statVal, statLabel, Icons.Default.DirectionsBus, GreenPrimary)
                        StatCard(Modifier.weight(1f), "${userCard?.totalTrips ?: 0}", "Total de viagens", Icons.Default.Route, Color(0xFF60A5FA))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Recent Trips ──────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Viagens Recentes", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Ver tudo", color = GreenPrimary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("Route 45", "Ponta Delgada → Furnas", "Hoje, 14:45"),
                Triple("Route 12", "Lagoa → Ribeira Grande", "Hoje, 09:30"),
                Triple("Route 8",  "Vila Franca → Nordeste",  "Ontem")
            ).forEachIndexed { i, (route, path, time) ->
                TripItem(route, path, "€2.50", time)
                if (i < 2) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ── FLUXO DE CRIAÇÃO DE CARTÃO ────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════════════

    // PASSO 1 — Escolher: Físico ou Digital
    if (cardFlowStep == CardFlowStep.CHOOSE_TYPE) {
        ModalBottomSheet(
            onDismissRequest = { cardFlowStep = CardFlowStep.NONE },
            sheetState = sheetState,
            containerColor = BgCard,
            dragHandle = { DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
                Text("Adicionar Cartão", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Escolha como pretende utilizar o seu cartão",
                    color = TextSecondary, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Opção Cartão Físico
                CardTypeOption(
                    icon = Icons.Default.CreditCard,
                    title = "Cartão Físico",
                    description = "Associe um cartão NFC físico à sua conta",
                    color = BluePrimary,
                    badge = "NFC"
                ) {
                    cardFlowStep = CardFlowStep.SCAN_NFC
                }

                Spacer(Modifier.height(12.dp))

                // Opção Cartão Digital
                CardTypeOption(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Cartão Digital",
                    description = "Use o seu telemóvel como cartão de transporte",
                    color = GreenPrimary,
                    badge = "HCE"
                ) {
                    cardFlowStep = CardFlowStep.CHOOSE_PASS
                }
            }
        }
    }

    // PASSO 2A — Escolher tipo de passe (Digital)
    if (cardFlowStep == CardFlowStep.CHOOSE_PASS) {
        ModalBottomSheet(
            onDismissRequest = { cardFlowStep = CardFlowStep.NONE },
            sheetState = sheetState,
            containerColor = BgCard,
            dragHandle = { DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
                // Botão voltar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { cardFlowStep = CardFlowStep.CHOOSE_TYPE }
                        .padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Voltar", color = TextSecondary, fontSize = 14.sp)
                }

                Text("Escolha o tipo de passe", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Selecione a duração do seu passe digital",
                    color = TextSecondary, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                PassOption(Icons.Default.Today, "Passe Diário", "Carrega saldo e usa quando quiseres", "A partir de €3.50", BluePrimary) {
                    cardFlowStep = CardFlowStep.NONE
                    userViewModel.criarCartao("DIARIO") {}
                }
                Spacer(Modifier.height(12.dp))
                PassOption(Icons.Default.DateRange, "Passe Semanal", "10 viagens válidas por 7 dias", "€18.00", GreenPrimary) {
                    cardFlowStep = CardFlowStep.NONE
                    userViewModel.criarCartao("SEMANAL") {}
                }
                Spacer(Modifier.height(12.dp))
                PassOption(Icons.Default.CalendarMonth, "Passe Mensal", "Viagens ilimitadas durante 30 dias", "€55.00", Color(0xFFA855F7)) {
                    cardFlowStep = CardFlowStep.NONE
                    userViewModel.criarCartao("MENSAL") {}
                }
            }
        }
    }

    // PASSO 2B — Scan do cartão NFC físico
    if (cardFlowStep == CardFlowStep.SCAN_NFC) {
        ModalBottomSheet(
            onDismissRequest = { cardFlowStep = CardFlowStep.NONE },
            sheetState = sheetState,
            containerColor = BgCard,
            dragHandle = { DragHandle() }
        ) {
            NfcScanSheet(
                cardState = cardState,
                onBack = { cardFlowStep = CardFlowStep.CHOOSE_TYPE },
                onCancel = { cardFlowStep = CardFlowStep.NONE }
            )
        }
    }

    // ── Wallet Dialog NFC ─────────────────────────────────────────────────────
    if (showCardWallet) {
        CardWalletDialog(
            profile = userProfile,
            card = userCard,
            isValid = userViewModel.isCardValid(userCard, userProfile?.cardType),
            onDismiss = { showCardWallet = false }
        )
    }

    // ── Bottom Sheet: Carregar / Renovar ──────────────────────────────────────
    if (showTopUp) {
        ModalBottomSheet(
            onDismissRequest = { showTopUp = false },
            sheetState = topUpSheetState,
            containerColor = BgCard
        ) {
            when (userProfile?.cardType) {
                "DIARIO" -> TopUpSheet(userCard?.saldo ?: 0.0) { amount ->
                    showTopUp = false
                    userViewModel.carregarSaldo(amount) { carregarDados() }
                }
                "MENSAL" -> RenewSheet(renewalDate = userCard?.renewalDate) {
                    showTopUp = false
                    userViewModel.renovarPasseMensal { carregarDados() }
                }
                "SEMANAL" -> RenewSheet(expiryDate = userCard?.expiryDate, tripsLeft = userCard?.tripsLeft) {
                    showTopUp = false
                    userViewModel.criarCartao("SEMANAL") {}
                }
            }
        }
    }
}

// ── Sheet: Scan NFC Físico ────────────────────────────────────────────────────
@Composable
private fun NfcScanSheet(
    cardState: CardState,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcScan")
    val pulse by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = EaseOut)),
        label = "p1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, 500, EaseOut)),
        label = "p2"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Voltar
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onBack() }.padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Voltar", color = TextSecondary, fontSize = 14.sp)
        }

        Text("Adicionar Cartão Físico", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Aproxime o seu cartão NFC físico da parte de trás do telemóvel",
            color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 36.dp)
        )

        // Animação NFC
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            // Ondas
            listOf(pulse2, pulse).forEach { p ->
                Box(
                    modifier = Modifier.size((80 + p * 80).dp).clip(CircleShape)
                        .border(1.5.dp, BluePrimary.copy(alpha = (1f - p) * 0.6f), CircleShape)
                        .alpha(1f - p)
                )
            }
            // Ícone central
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(BluePrimary.copy(0.15f))
                    .border(1.5.dp, BluePrimary.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, null, tint = BluePrimary, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // Feedback de estado
        when (cardState) {
            is CardState.Loading -> {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(BluePrimary.copy(0.1f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = BluePrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("A registar cartão...", color = BluePrimary, fontSize = 13.sp)
                }
            }
            is CardState.Success -> {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(GreenPrimary.copy(0.1f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cartão físico adicionado!", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            is CardState.Error -> {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(0.1f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(cardState.message, color = Color.Red, fontSize = 13.sp)
                }
            }
            else -> {
                // Aguardando — instrução visual
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(BluePrimary.copy(0.1f))
                        .border(1.dp, BluePrimary.copy(0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(BluePrimary))
                    Spacer(Modifier.width(8.dp))
                    Text("À espera do cartão NFC...", color = BluePrimary, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onCancel) {
            Text("Cancelar", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

// ── Card Type Option (Físico / Digital) ───────────────────────────────────────
@Composable
private fun CardTypeOption(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    badge: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(InputBg)
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(color.copy(0.2f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(badge, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(description, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Cartão Ativo ──────────────────────────────────────────────────────────────
@Composable
private fun ActiveCard(
    profile: UserProfile?,
    card: UserCard?,
    glowPulse: Float,
    isValid: Boolean,
    onCardClick: () -> Unit
) {
    val accent = if (isValid) GreenPrimary else Color(0xFF6B7280)
    Box {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).offset(y = 10.dp).blur(40.dp)
                .background(accent.copy(alpha = glowPulse * 0.2f), RoundedCornerShape(24.dp))
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(0f to Color(0xFF0D3320), 0.5f to Color(0xFF0F4A2B), 1f to Color(0xFF1A6B3C)))
                .border(1.dp, accent.copy(0.35f), RoundedCornerShape(24.dp))
                .clickable { onCardClick() }
                .padding(22.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..5) drawCircle(Color.White.copy(0.03f), 80f + i * 40f, androidx.compose.ui.geometry.Offset(size.width - 60f, size.height / 2))
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Azores Bus Card", color = Color.White.copy(0.7f), fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.clip(CircleShape).background(accent.copy(0.25f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                        Spacer(Modifier.width(5.dp))
                        Text(if (isValid) "Active" else "Inativo", color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val mainValue = when (profile?.cardType) {
                    "DIARIO"  -> "€${String.format("%.2f", card?.saldo ?: 0.0)}"
                    "SEMANAL" -> "${card?.tripsLeft ?: 0} viagens"
                    "MENSAL"  -> "∞ viagens"
                    else      -> "—"
                }
                Text(mainValue, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    CardInfoCol("Tipo", profile?.cardType?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "—", Modifier.weight(1f))
                    CardInfoCol("Titular", "${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim(), Modifier.weight(1f))
                    CardInfoCol(
                        if (card?.hasPhysicalCard == true) "Modo" else "Viagens",
                        if (card?.hasPhysicalCard == true) "Físico + Digital" else "${card?.totalTrips ?: 0} total",
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun CardInfoCol(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color.White.copy(0.5f), fontSize = 10.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// ── Cartão Vazio ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp)
            .clip(RoundedCornerShape(24.dp)).background(InputBg)
            .border(1.5.dp, Brush.linearGradient(listOf(SurfaceBorder, GreenPrimary.copy(0.3f))), RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(GreenPrimary.copy(0.15f)).border(1.dp, GreenPrimary.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = GreenPrimary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Adicionar cartão", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Toque para escolher o tipo de cartão", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ── Shimmer ───────────────────────────────────────────────────────────────────
@Composable
private fun ShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s"
    )
    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(InputBg.copy(alpha = alpha)))
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, icon: ImageVector, accent: Color) {
    Box(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Column {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ── Trip Item ─────────────────────────────────────────────────────────────────
@Composable
private fun TripItem(route: String, path: String, price: String, time: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCard).border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GreenPrimary.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsBus, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GreenPrimary.copy(0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(route, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(path, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(time, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Text(price, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ── Pass Option ───────────────────────────────────────────────────────────────
@Composable
private fun PassOption(icon: ImageVector, title: String, description: String, price: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(InputBg).border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(description, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text(price, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ── Wallet Dialog NFC ─────────────────────────────────────────────────────────
@Composable
private fun CardWalletDialog(profile: UserProfile?, card: UserCard?, isValid: Boolean, onDismiss: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "nfc")
    val w1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = EaseOut)), "w1")
    val w2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, 500, EaseOut)), "w2")
    val w3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, 1000, EaseOut)), "w3")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.93f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aproxime do leitor", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 32.dp))
                Box(contentAlignment = Alignment.Center) {
                    listOf(w3, w2, w1).forEach { p ->
                        Box(modifier = Modifier.size((220 + p * 130).dp).clip(RoundedCornerShape((24 + p * 40).dp))
                            .border(1.5.dp, GreenPrimary.copy(alpha = (1f - p) * 0.5f), RoundedCornerShape((24 + p * 40).dp)).alpha(1f - p))
                    }
                    Box(
                        modifier = Modifier.width(300.dp).height(185.dp).clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(0f to Color(0xFF0D3320), 1f to Color(0xFF1A6B3C)))
                            .border(1.dp, GreenPrimary.copy(0.5f), RoundedCornerShape(24.dp)).padding(22.dp)
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            for (i in 0..5) drawCircle(Color.White.copy(0.03f), 80f + i * 40f, androidx.compose.ui.geometry.Offset(size.width - 40f, size.height / 2))
                        }
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Azores Bus Card", color = Color.White.copy(0.7f), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.Wifi, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            val v = when (profile?.cardType) {
                                "DIARIO"  -> "€${String.format("%.2f", card?.saldo ?: 0.0)}"
                                "SEMANAL" -> "${card?.tripsLeft ?: 0} viagens"
                                "MENSAL"  -> "∞ viagens"
                                else      -> "—"
                            }
                            Text(v, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim(), color = Color.White.copy(0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(profile?.cardType ?: "—", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (isValid) GreenPrimary.copy(0.15f) else Color.Red.copy(0.15f))
                        .border(1.dp, if (isValid) GreenPrimary.copy(0.3f) else Color.Red.copy(0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isValid) GreenPrimary else Color.Red))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isValid) "NFC Pronto" else "Cartão Inválido", color = if (isValid) GreenPrimary else Color.Red, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss) { Text("Fechar", color = TextSecondary) }
            }
        }
    }
}

// ── Top Up Sheet ──────────────────────────────────────────────────────────────
@Composable
private fun TopUpSheet(currentBalance: Double, onTopUp: (Double) -> Unit) {
    var selected by remember { mutableStateOf<Double?>(null) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
        DragHandle()
        Text("Carregar Saldo", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Saldo atual: €${String.format("%.2f", currentBalance)}", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(5.0, 10.0, 20.0, 50.0).forEach { amount ->
                val sel = selected == amount
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (sel) GreenPrimary.copy(0.2f) else InputBg)
                        .border(1.5.dp, if (sel) GreenPrimary else SurfaceBorder, RoundedCornerShape(14.dp))
                        .clickable { selected = amount }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("€${amount.toInt()}", color = if (sel) GreenPrimary else TextPrimary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        ActionButton(if (selected != null) "Carregar €${selected!!.toInt()}" else "Selecione um valor", selected != null) { selected?.let { onTopUp(it) } }
    }
}

// ── Renew Sheet ───────────────────────────────────────────────────────────────
@Composable
private fun RenewSheet(renewalDate: String? = null, expiryDate: String? = null, tripsLeft: Int? = null, onRenew: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
        DragHandle()
        Text("Renovar Passe", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        val info = when {
            expiryDate != null  -> "Expira em: $expiryDate\nViagens restantes: ${tripsLeft ?: 0}"
            renewalDate != null -> "Renovado em: $renewalDate\nVálido até ao fim do mês"
            else                -> ""
        }
        Text(info, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        ActionButton("Renovar Passe", true, onRenew)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
private fun DragHandle() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(SurfaceBorder))
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = InputBg),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (enabled) Brush.linearGradient(listOf(GreenPrimary, GreenDark))
                else Brush.linearGradient(listOf(InputBg, InputBg))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = if (enabled) Color.White else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}