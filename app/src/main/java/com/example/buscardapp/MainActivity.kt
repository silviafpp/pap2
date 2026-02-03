package com.example.buscardapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.buscardapp.ui.theme.BusCardAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Inicializa a Splash Screen (se configurada no manifest)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            // 2. Aplica o tema da sua aplicação
            BusCardAppTheme {
                // 3. Chama a tela de registo que criámos
                // O ViewModel é instanciado automaticamente aqui
                AuthScreen()
            }
        }
    }
}