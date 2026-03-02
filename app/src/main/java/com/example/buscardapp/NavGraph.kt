package com.example.buscardapp

import androidx.compose.runtime.*
import androidx.navigation.compose.*

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    physicalCardUid: String? = null,          // ← NOVO
    onPhysicalCardConsumed: () -> Unit = {}   // ← NOVO
) {
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    if (authState != "Login efetuado!" && authState != "Bem-vindo!") {
        AuthScreen(authViewModel)
    } else {
        NavHost(navController = navController, startDestination = "main_screen") {
            composable("main_screen") {
                MainScreen(
                    authViewModel = authViewModel,
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    onCardClick = onCardClick,
                    physicalCardUid = physicalCardUid,
                    onPhysicalCardConsumed = onPhysicalCardConsumed
                )
            }
        }
    }
}