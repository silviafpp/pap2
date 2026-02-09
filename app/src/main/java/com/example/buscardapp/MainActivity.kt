package com.example.buscardapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buscardapp.ui.theme.BusCardAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var showNfcSheet by remember { mutableStateOf(false) }

            BusCardAppTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = viewModel()

                Box {
                    NavGraph(
                        authViewModel = authViewModel,
                        isDarkMode = isDarkMode,
                        onThemeToggle = { isDarkMode = it },
                        // Esta função deve ser chamada quando clicares no cartão no teu Screen
                        onCardClick = { showNfcSheet = true }
                    )

                    if (showNfcSheet) {
                        NfcPaymentOverlay(onDismiss = { showNfcSheet = false })
                    }
                }
            }
        }
    }
}