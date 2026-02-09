package com.example.buscardapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onCardClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val user = SupabaseClient.supabase.auth.currentUserOrNull()

    val firstName = user?.userMetadata?.get("first_name")?.toString()?.removeSurrounding("\"") ?: "Utilizador"
    val lastName = user?.userMetadata?.get("last_name")?.toString()?.removeSurrounding("\"") ?: ""

    var activeCard by remember { mutableStateOf<UserCard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showInitialDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            activeCard = SupabaseClient.supabase.postgrest["user_cards"]
                .select().decodeSingleOrNull<UserCard>()
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Olá, $firstName", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (activeCard == null) {
            // Chamada da função que estava a dar erro
            CreateCardPlaceholder { showInitialDialog = true }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { onCardClick() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(Icons.Default.CreditCard, null, tint = Color.White)
                        Text(activeCard!!.card_type.uppercase(), color = Color.Cyan, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Lógica de visualização por tipo de passe
                    when (activeCard?.card_type) {
                        "Semanal" -> Text("${activeCard?.trips_left}/10 Viagens", color = Color.White, fontSize = 18.sp)
                        "Mensal" -> Text("Viagens Ilimitadas", color = Color.White, fontSize = 18.sp)
                        else -> Text("Passe Diário", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$firstName $lastName".uppercase(), color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (showInitialDialog) {
        AlertDialog(
            onDismissRequest = { showInitialDialog = false },
            title = { Text("Novo Cartão") },
            text = { Text("Deseja criar um cartão digital?") },
            confirmButton = { Button(onClick = { showInitialDialog = false; showTypeDialog = true }) { Text("Sim") } }
        )
    }

    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("Escolha o Passe") },
            confirmButton = {},
            text = {
                Column {
                    listOf("Diário", "Semanal", "Mensal").forEach { tipo ->
                        ListItem(
                            headlineContent = { Text(tipo) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val newCard = UserCard(
                                        user_id = user?.id ?: "",
                                        card_type = tipo,
                                        trips_left = if (tipo == "Semanal") 10 else 0
                                    )
                                    SupabaseClient.supabase.postgrest["user_cards"].insert(newCard)
                                    activeCard = newCard
                                    showTypeDialog = false
                                }
                            }
                        )
                    }
                }
            }
        )
    }
}

// --- ESSA FUNÇÃO DEVE FICAR AQUI, FORA DA HOMESCREEN ---
@Composable
fun CreateCardPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEEEEEE))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AddCard, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Adicionar Cartão", color = Color.Gray)
        }
    }
}