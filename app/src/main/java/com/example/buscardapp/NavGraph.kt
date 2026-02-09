package com.example.buscardapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit // <--- 1. NOVO PARÂMETRO
) {
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    // Lógica de Autenticação
    if (authState != "Login efetuado!" && authState != "Bem-vindo!") {
        AuthScreen(authViewModel)
    } else {
        // Passamos o onCardClick para a MainScreen
        MainScreen(
            authViewModel = authViewModel,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            onCardClick = onCardClick // <--- 2. PASSAR PARA A MAIN SCREEN
        )
    }
}