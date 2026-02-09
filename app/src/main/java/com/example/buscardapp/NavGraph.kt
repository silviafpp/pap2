package com.example.buscardapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit // Agora aceita o Boolean do Switch
) {
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    // Lógica de Autenticação: Se não estiver logado, mostra AuthScreen
    if (authState != "Login efetuado!" && authState != "Bem-vindo!") {
        AuthScreen(authViewModel)
    } else {
        // Se estiver logado, usamos o Scaffold da MainScreen que contém a navegação
        MainScreen(
            authViewModel = authViewModel,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle
        )
    }
}