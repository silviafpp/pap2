package com.example.buscardapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcPaymentOverlay(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Pronto a Validar",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Aqui podes colocar um ícone de NFC
            Text("(( 📱 ))", style = MaterialTheme.typography.displayLarge)

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Aproxime o telemóvel do leitor do autocarro",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}