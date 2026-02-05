package com.example.buscardapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.buscardapp.ui.theme.BusCardAppTheme

class MainActivity : ComponentActivity() {

    // Instancia o ViewModel para ser usado na AuthScreen
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            BusCardAppTheme {
                AuthScreen(viewModel = authViewModel)
            }
        }
    }
}