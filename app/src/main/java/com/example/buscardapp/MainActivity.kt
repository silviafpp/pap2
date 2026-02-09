package com.example.buscardapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buscardapp.ui.theme.BusCardAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. Criar o estado do tema aqui
            var isDarkMode by remember { mutableStateOf(false) }

            // 2. Passar o estado para o Theme da App
            BusCardAppTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = viewModel()

                // 3. PASSAR O PARÂMETRO QUE ESTÁ A FALTAR
                NavGraph(
                    authViewModel = authViewModel,
                    isDarkMode = isDarkMode, // <--- Faltava isto
                    onThemeToggle = { isDarkMode = it } // <--- E isto
                )
            }
        }
    }
}