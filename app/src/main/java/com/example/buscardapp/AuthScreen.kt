package com.example.buscardapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Controla se o utilizador está na aba de Login ou Registo
    var isLoginMode by remember { mutableStateOf(true) }

    // Detecta se o e-mail foi disparado com sucesso (Troca para ecrã de código)
    val isWaitingForOtp = authState?.contains("Código enviado", ignoreCase = true) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título dinâmico conforme o estado
        Text(
            text = when {
                isWaitingForOtp -> "Confirmar E-mail"
                isLoginMode -> "Login"
                else -> "Criar Conta"
            },
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isWaitingForOtp) {
            // --- MODO DE VERIFICAÇÃO (OTP) ---
            Text(
                text = "Insira os 6 dígitos enviados para\n$email",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = { Text("Código de Verificação") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.verifyOtp(email, otpCode) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verificar Código")
            }

            TextButton(onClick = { viewModel.signUpWithEmail(email, password) }) {
                Text("Reenviar e-mail")
            }

        } else {
            // --- MODO ENTRAR / REGISTAR ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Palavra-passe") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isLoginMode) viewModel.signInWithEmail(email, password)
                    else viewModel.signUpWithEmail(email, password)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoginMode) "Entrar" else "Registar")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opção Google (Sempre visível fora do OTP)
            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar com Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                isLoginMode = !isLoginMode
                otpCode = "" // Limpa o código ao trocar de modo
            }) {
                Text(if (isLoginMode) "Não tem conta? Registe-se" else "Já tem conta? Faça Login")
            }
        }

        // Feedback de Status (Sucesso ou Erro)
        authState?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = if (it.contains("Erro", ignoreCase = true) || it.contains("inválido"))
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}