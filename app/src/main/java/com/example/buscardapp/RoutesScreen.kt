package com.example.buscardapp

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DirectionsBus
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
    var isLoading by remember { mutableStateOf(true) }

    // 1. Carregar todas as rotas da Base de Dados
    LaunchedEffect(Unit) {
        try {
            val results = SupabaseClient.supabase.postgrest["bus_routes"]
                .select().decodeList<BusRoute>()
            routesList = results
        } catch (e: Exception) {
            println("Erro ao carregar rotas: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 2. Lógica para extrair opções únicas para as caixas de seleção
    val origins = routesList.map { it.origin }.distinct().sorted()
    val destinations = routesList
        .filter { it.origin == originSelected }
        .map { it.destination }
        .distinct()
        .sorted()

    // 3. Filtragem da lista final baseada nas seleções
    val filteredRoutes = routesList.filter {
        (originSelected == "Selecionar Origem" || it.origin == originSelected) &&
                (destSelected == "Selecionar Destino" || it.destination == destSelected)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Explorar Rotas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- CAIXAS DE SELEÇÃO (DROPDOWNS) ---
        Card(
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Seleção de Origem
                RouteDropdownSelector(
                    label = "De onde parte?",
                    selectedOption = originSelected,
                    options = origins
                ) { selection ->
                    originSelected = selection
                    destSelected = "Selecionar Destino" // Reseta o destino ao mudar a origem
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seleção de Destino
                RouteDropdownSelector(
                    label = "Para onde vai?",
                    selectedOption = destSelected,
                    options = destinations,
                    enabled = originSelected != "Selecionar Origem"
                ) { selection ->
                    destSelected = selection
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- LISTA DE RESULTADOS FILTRADOS ---
        Text(
            text = "Rotas Disponíveis (${filteredRoutes.size})",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRoutes) { route ->
                    RouteResultItem(route)
                }
            }
        }
    }
}

@Composable
fun RouteDropdownSelector(
    label: String,
    selectedOption: String,
    options: List<String>,
    enabled: Boolean = true,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (enabled) Color.LightGray else Color(0xFFF00F0F0),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { expanded = true }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedOption,
                    color = if (enabled) Color.Black else Color.LightGray
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RouteResultItem(route: BusRoute) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${route.origin} → ${route.destination}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Linha ${route.route_number} • Duração estimada: ${route.duration}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "${String.format("%.2f", route.price)}€",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}