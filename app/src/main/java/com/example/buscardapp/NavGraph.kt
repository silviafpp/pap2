package com.example.buscardapp

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*

sealed class Screen(val route: String) {
    object Auth    : Screen("auth")
    object Home    : Screen("home")
    object Routes  : Screen("routes")
    object Profile : Screen("profile")
}

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.Home,    "Início", Icons.Default.Home),
    NavItem(Screen.Routes,  "Rotas",  Icons.Default.Map),
    NavItem(Screen.Profile, "Perfil", Icons.Default.Person),
)

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    physicalCardUid: String?,
    onPhysicalCardConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            "Login efetuado!" -> navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
            "Logout efetuado" -> navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute     = currentBackStack?.destination?.route
    val showBottomBar    = currentRoute in listOf(
        Screen.Home.route, Screen.Routes.route, Screen.Profile.route
    )

    Scaffold(
        containerColor = BgDeep,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                AppBottomBar(currentRoute, navController)
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Auth.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(authViewModel = authViewModel)
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    isDarkMode             = isDarkMode,
                    onCardClick            = onCardClick,
                    physicalCardUid        = physicalCardUid,
                    onPhysicalCardConsumed = onPhysicalCardConsumed
                )
            }
            composable(Screen.Routes.route) {
                RoutesScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    isDarkMode    = isDarkMode,
                    onThemeChange = onThemeToggle
                )
            }
        }
    }
}

// ── Navbar melhorada ──────────────────────────────────────────────────────────
@Composable
private fun AppBottomBar(
    currentRoute: String?,
    navController: androidx.navigation.NavHostController
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation    = 12.dp,
                    shape        = RoundedCornerShape(32.dp),
                    ambientColor = GreenPrimary.copy(0.08f),
                    spotColor    = GreenPrimary.copy(0.12f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(BgCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = currentRoute == item.screen.route

                // Largura animada — item selecionado expande
                val itemWeight by animateFloatAsState(
                    targetValue   = if (selected) 1.6f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "weight_${item.label}"
                )

                Box(
                    modifier = Modifier
                        .weight(itemWeight)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (selected)
                                Brush.linearGradient(
                                    listOf(GreenPrimary.copy(0.18f), GreenDark.copy(0.10f))
                                )
                            else
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState  = selected,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(150))
                        },
                        label = "content_${item.label}"
                    ) { isSelected ->
                        if (isSelected) {
                            // Item ativo: ícone com fundo circular + label em linha
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier              = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary.copy(0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector        = item.icon,
                                        contentDescription = item.label,
                                        tint               = GreenPrimary,
                                        modifier           = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text          = item.label,
                                    color         = GreenPrimary,
                                    fontSize      = 13.sp,
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                )
                            }
                        } else {
                            // Item inativo: apenas ícone
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = item.label,
                                tint               = TextSecondary,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}