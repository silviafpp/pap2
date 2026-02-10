package com.example.buscardapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth

@Composable
fun HomeScreen(
    onCardClick: () -> Unit
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userCard by remember { mutableStateOf<UserCard?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val user = SupabaseClient.supabase.auth.currentUserOrNull()
            user?.id?.let { uid ->
                userProfile = SupabaseClient.supabase.postgrest["profiles"]
                    .select { filter { eq("id", uid) } }
                    .decodeSingleOrNull<UserProfile>()

                userCard = SupabaseClient.supabase.postgrest["user_cards"]
                    .select { filter { eq("user_id", uid) } }
                    .decodeSingleOrNull<UserCard>()
            }
        } catch (e: Exception) {
            println("Erro Home: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    val nomeExibicao = if (userProfile != null && !userProfile?.firstName.isNullOrBlank()) {
        "${userProfile?.firstName} ${userProfile?.lastName}"
    } else {
        "Utilizador"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Olá, $nomeExibicao!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
        )

        // CARTÃO COM NOME DO UTILIZADOR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { onCardClick() },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF006D4E), Color(0xFF00E676))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("BUS CARD", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text(
                                text = nomeExibicao.uppercase(), // NOME NO CARTÃO
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Nfc, null, tint = Color.White)
                    }

                    Column {
                        Text("Saldo Disponível", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(
                            text = "${userCard?.saldo ?: 0.0}€",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("Ações Rápidas", fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(15.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            // Apenas uma chamada para cada ação
            QuickActionButton("Carregar", Icons.Default.AddCard, Modifier.weight(1f))
            QuickActionButton("Histórico", Icons.Default.History, Modifier.weight(1f))
        }
    }
}

// GARANTE QUE ESTA FUNÇÃO SÓ APARECE UMA VEZ NO FICHEIRO
@Composable
fun QuickActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color(0xFF006D4E))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}