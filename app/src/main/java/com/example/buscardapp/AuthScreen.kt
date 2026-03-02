package com.example.buscardapp

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Paleta de Cores ──────────────────────────────────────────────────────────
private val BgDeep        = Color(0xFF0A0F14)
private val BgCard        = Color(0xFF111920)
private val GreenPrimary  = Color(0xFF22C55E)
private val GreenDark     = Color(0xFF16A34A)
private val TextPrimary   = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF8A9BB0)
private val InputBg       = Color(0xFF182130)
private val InputBorder   = Color(0xFF1E2D3D)
private val ErrorColor    = Color(0xFFEF4444)

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ── FIX: estado estável para o passo OTP ─────────────────────────────────
    // Não dependemos da mensagem do authState para controlar o ecrã OTP.
    // isOtpStep só passa a true quando o Supabase confirma o envio do código,
    // e só volta a false após sucesso ou erro definitivo.
    var isOtpStep by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    // Quando o estado indica que o código foi enviado, ativamos o ecrã OTP
    LaunchedEffect(authState) {
        when {
            authState?.contains("Verifique") == true -> isOtpStep = true
            authState == "Login efetuado!"            -> isOtpStep = false
            authState == "Código inválido."           -> { /* mantém OTP visível para o user tentar de novo */ }
        }
    }

    val isError = authState?.startsWith("Erro") == true || authState == "Código inválido."

    // Animação de brilho de fundo
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Decoração de fundo — glow verde suave
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = (-80).dp)
                .blur(120.dp)
                .background(GreenPrimary.copy(alpha = glowAlpha * 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 60.dp)
                .blur(100.dp)
                .background(GreenDark.copy(alpha = glowAlpha * 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Logo ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark))),
                contentAlignment = Alignment.Center
            ) {
                Text("🚌", fontSize = 32.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Azores Bus Card",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "O seu passe digital nos Açores",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Card Principal ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BgCard)
                    .border(1.dp, InputBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    if (!isOtpStep) {
                        TabToggle(isLogin = isLogin, onToggle = {
                            isLogin = it
                            email = ""; password = ""; firstName = ""; lastName = ""
                        })
                        Spacer(Modifier.height(28.dp))
                    }

                    AnimatedContent(
                        targetState = isOtpStep,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "formTransition"
                    ) { otp ->
                        if (otp) {
                            OtpForm(
                                email    = email,
                                otpCode  = otpCode,
                                onOtpChange = { otpCode = it },
                                onVerify = { authViewModel.verifyOtp(email, otpCode) }
                            )
                        } else {
                            Column {
                                AnimatedVisibility(
                                    visible = !isLogin,
                                    enter   = fadeIn() + expandVertically(),
                                    exit    = fadeOut() + shrinkVertically()
                                ) {
                                    Column {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            StyledTextField(
                                                value         = firstName,
                                                onValueChange = { firstName = it },
                                                label         = "Primeiro Nome",
                                                leadingIcon   = Icons.Default.Person,
                                                modifier      = Modifier.weight(1f)
                                            )
                                            StyledTextField(
                                                value         = lastName,
                                                onValueChange = { lastName = it },
                                                label         = "Apelido",
                                                leadingIcon   = Icons.Default.Person,
                                                modifier      = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }

                                StyledTextField(
                                    value         = email,
                                    onValueChange = { email = it },
                                    label         = "Email",
                                    leadingIcon   = Icons.Default.Email,
                                    keyboardType  = KeyboardType.Email
                                )

                                Spacer(Modifier.height(12.dp))

                                StyledTextField(
                                    value               = password,
                                    onValueChange       = { password = it },
                                    label               = "Password",
                                    leadingIcon         = Icons.Default.Lock,
                                    keyboardType        = KeyboardType.Password,
                                    trailingIcon        = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector     = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint            = TextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                    else PasswordVisualTransformation()
                                )

                                if (isLogin) {
                                    Text(
                                        text     = "Esqueceu a password?",
                                        color    = GreenPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 8.dp)
                                            .clickable { }
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                GreenButton(
                                    text    = if (isLogin) "Entrar" else "Criar Conta",
                                    onClick = {
                                        if (isLogin) authViewModel.signInWithEmail(email, password)
                                        else         authViewModel.signUpWithEmail(email, password, firstName, lastName)
                                    }
                                )

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier.fillMaxWidth()
                                ) {
                                    Divider(modifier = Modifier.weight(1f), color = InputBorder)
                                    Text("  ou  ", color = TextSecondary, fontSize = 12.sp)
                                    Divider(modifier = Modifier.weight(1f), color = InputBorder)
                                }

                                Spacer(Modifier.height(16.dp))

                                GoogleButton(
                                    text    = if (isLogin) "Continuar com Google" else "Registar com Google",
                                    onClick = { authViewModel.signInWithGoogle(context) }
                                )
                            }
                        }
                    }

                    // Mensagem de estado
                    authState?.let { state ->
                        val isSuccess = state == "Login efetuado!" || state.contains("Verifique")
                        AnimatedVisibility(visible = true, enter = fadeIn()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isError) ErrorColor.copy(0.1f)
                                        else         GreenPrimary.copy(0.1f)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = if (isError) "⚠️" else "✅", fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text     = state,
                                    color    = if (isError) ErrorColor else GreenPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            if (!isOtpStep) {
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        text     = if (isLogin) "Ainda não tem conta? " else "Já tem conta? ",
                        color    = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text         = if (isLogin) "Registe-se" else "Entrar",
                        color        = GreenPrimary,
                        fontSize     = 14.sp,
                        fontWeight   = FontWeight.SemiBold,
                        modifier     = Modifier.clickable { isLogin = !isLogin }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Componentes ──────────────────────────────────────────────────────────────

@Composable
private fun TabToggle(isLogin: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .padding(4.dp)
    ) {
        Row {
            listOf("Entrar" to true, "Registar" to false).forEach { (label, value) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isLogin == value)
                                Brush.linearGradient(listOf(GreenPrimary, GreenDark))
                            else
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onToggle(value) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = if (isLogin == value) Color.White else TextSecondary,
                        fontWeight = if (isLogin == value) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize   = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label, fontSize = 13.sp) },
        leadingIcon         = {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        },
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        singleLine           = true,
        colors               = OutlinedTextFieldDefaults.colors(
            focusedTextColor       = TextPrimary,
            unfocusedTextColor     = TextPrimary,
            focusedContainerColor  = InputBg,
            unfocusedContainerColor= InputBg,
            focusedBorderColor     = GreenPrimary,
            unfocusedBorderColor   = InputBorder,
            focusedLabelColor      = GreenPrimary,
            unfocusedLabelColor    = TextSecondary,
            cursorColor            = GreenPrimary
        ),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun GreenButton(text: String, onClick: () -> Unit) {
    Button(
        onClick         = onClick,
        modifier        = Modifier.fillMaxWidth().height(52.dp),
        shape           = RoundedCornerShape(14.dp),
        colors          = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding  = PaddingValues(0.dp)
    ) {
        Box(
            modifier            = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GreenPrimary, GreenDark))),
            contentAlignment    = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun GoogleButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, InputBorder),
        colors   = ButtonDefaults.outlinedButtonColors(containerColor = InputBg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("G", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OtpForm(
    email: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✉️", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
        Text("Verifique o seu email", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text      = "Enviámos um código para\n$email",
            color     = TextSecondary,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        StyledTextField(
            value         = otpCode,
            onValueChange = onOtpChange,
            label         = "Código de verificação",
            leadingIcon   = Icons.Default.Lock,
            keyboardType  = KeyboardType.Number
        )
        Spacer(Modifier.height(20.dp))
        GreenButton(text = "Verificar e Entrar", onClick = onVerify)
    }
}