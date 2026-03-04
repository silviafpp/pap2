package com.example.buscardapp

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// Usa a paleta global de HomeScreen.kt:
// BgDeep, BgCard, GreenPrimary, GreenDark, TextPrimary, TextSecondary,
// InputBg, SurfaceBorder, BluePrimary

@Composable
fun RoutesScreen() {
    var routesList     by remember { mutableStateOf(listOf<BusRoute>()) }
    var originSelected by remember { mutableStateOf("") }
    var destSelected   by remember { mutableStateOf("") }
    var isLoading      by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                routesList = SupabaseClient.supabase.postgrest["bus_routes"]
                    .select().decodeList<BusRoute>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val origins = routesList.map { it.origin }.distinct().sorted()
    val destinations = routesList
        .filter { originSelected == "" || it.origin == originSelected }
        .map { it.destination }.distinct().sorted()

    val filteredRoutes = routesList.filter {
        (originSelected == "" || it.origin == originSelected) &&
                (destSelected   == "" || it.destination == destSelected)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(52.dp))

        // ── Header ─────────────────────────────────────────────────────────────
        Text("Explorar Rotas", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Selecione o seu trajeto", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))

        Spacer(Modifier.height(24.dp))

        // ── Seletores de rota ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                RouteSelector(
                    label    = "Partida",
                    selected = if (originSelected == "") "Selecionar Origem" else originSelected,
                    options  = origins,
                    icon     = Icons.Default.TripOrigin,
                    accent   = GreenPrimary
                ) {
                    originSelected = it
                    destSelected   = ""
                }

                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color     = SurfaceBorder
                )

                RouteSelector(
                    label    = "Chegada",
                    selected = if (destSelected == "") "Selecionar Destino" else destSelected,
                    options  = destinations,
                    icon     = Icons.Default.LocationOn,
                    accent   = BluePrimary,
                    enabled  = originSelected != ""
                ) {
                    destSelected = it
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Contagem de resultados ─────────────────────────────────────────────
        if (!isLoading && filteredRoutes.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rotas disponíveis", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GreenPrimary.copy(0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${filteredRoutes.size}", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Lista de rotas ─────────────────────────────────────────────────────
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            }
            filteredRoutes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Nenhuma rota disponível", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding      = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredRoutes) { route ->
                        RouteCard(route)
                    }
                }
            }
        }
    }
}

// ── Selector de Rota ───────────────────────────────────────────────────────────
@Composable
private fun RouteSelector(
    label: String,
    selected: String,
    options: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    enabled: Boolean = true,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isPlaceholder = selected.contains("Selecionar")

    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) InputBg else InputBg.copy(0.5f))
                .border(1.dp, if (!isPlaceholder) accent.copy(0.4f) else SurfaceBorder, RoundedCornerShape(12.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, null,
                    tint     = if (enabled && !isPlaceholder) accent else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text     = selected,
                    color    = if (isPlaceholder) TextSecondary else TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
                expanded          = expanded,
                onDismissRequest  = { expanded = false },
                modifier          = Modifier.background(BgCard).border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text   = { Text(option, color = TextPrimary, fontSize = 14.sp) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        modifier = Modifier.background(BgCard)
                    )
                }
            }
        }
    }
}

// ── Route Card ─────────────────────────────────────────────────────────────────
@Composable
private fun RouteCard(route: BusRoute) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenPrimary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsBus, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Rota
                Text(
                    text       = "${route.origin} → ${route.destination}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                // Detalhes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(InputBg).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Linha ${route.route_number}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(route.duration, color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Preço
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "€${"%.2f".format(route.price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = GreenPrimary
                )
            }
        }
    }
}