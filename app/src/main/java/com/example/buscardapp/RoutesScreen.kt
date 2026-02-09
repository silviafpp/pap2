package com.example.buscardapp

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest

@Composable
fun RoutesScreen() {
    var routesList by remember { mutableStateOf(listOf<BusRoute>()) }
    var originSelected by remember { mutableStateOf("Selecionar Origem") }
    var destSelected by remember { mutableStateOf("Selecionar Destino") }
    var numLugares by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }

    // Busca as rotas na Base de Dados do Supabase
    LaunchedEffect(Unit) {
        try {
            val results = SupabaseClient.supabase.postgrest["bus_routes"]
                .select().decodeList<BusRoute>()
            routesList = results
        } catch (e: Exception) {
            println("Erro Supabase: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    val origins = routesList.map { it.origin }.distinct()
    val destinations = routesList.filter { it.origin == originSelected }.map { it.destination }.distinct()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pesquisar Viagens", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Card de Seleção
        Card(elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // FUNÇÃO DROPDOWN (Definida abaixo para evitar erro de referência)
                RouteDropdownCustom(label = "Partida", options = origins, selected = originSelected) {
                    originSelected = it
                    destSelected = "Selecionar Destino" // Reseta o destino ao mudar origem
                }

                Spacer(modifier = Modifier.height(12.dp))

                RouteDropdownCustom(label = "Destino", options = destinations, selected = destSelected) {
                    destSelected = it
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SELETOR DE LUGARES
                Text("Quantidade de Lugares", fontSize = 12.sp, color = Color.Gray)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$numLugares Passageiro(s)", fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (numLugares > 1) numLugares-- }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                        }
                        Text("$numLugares", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (numLugares < 10) numLugares++ }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text("Resultados", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(top = 10.dp)
            ) {
                val filtered = routesList.filter {
                    it.origin == originSelected && it.destination == destSelected
                }

                items(filtered) { route ->
                    RouteItemWithPrice(route = route, lugares = numLugares)
                }
            }
        }
    }
}

// --- FUNÇÕES AUXILIARES (Para não dar Unresolved Reference) ---

@Composable
fun RouteDropdownCustom(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(selected)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RouteItemWithPrice(route: BusRoute, lugares: Int) {
    val total = (route.price ?: 0.0) * lugares
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsBus, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${route.origin} → ${route.destination}", fontWeight = FontWeight.Bold)
                Text("Duração: ${route.duration}", fontSize = 12.sp)
            }
            Text("${String.format("%.2f", total)}€", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}