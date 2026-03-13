package com.safebyte

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safebyte.data.FirebaseUsersRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF165F31)
private val UserChipGreen = Color(0xFF3A7B4B)
private val AccentLime = Color(0xFFA3DB57)
private val MenuItemGray = Color(0xFFD2D8D4)
private val MenuDangerBg = Color(0xFFEADCE0)
private val MenuDangerText = Color(0xFFC62828)

sealed class Dest(val route: String, val label: String) {
    data object Home : Dest("home", "Home")
    data object Scanner : Dest("scanner", "Escanear producto")
    data object Meals : Dest("meals", "Comidas")
    data object IANutri : Dest("ianutri", "IANutri")
    data object Settings : Dest("settings", "Gestionar alergenos")
    data object Contact : Dest("contact", "Soporte")
}

@Composable
fun SafeByteApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember(context) { UserPrefs(context) }
    val scope = rememberCoroutineScope()

    val isLoggedIn by prefs.isLoggedIn.collectAsState(initial = false)
    val loggedEmail by prefs.loggedEmail.collectAsState(initial = "")

    val usersRepo = remember { FirebaseUsersRepo() }
    var userAllergens by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allergensLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn, loggedEmail) {
        val cachedLocal = prefs.allergens.first()
        if (isLoggedIn && loggedEmail.isNotBlank() && loggedEmail != "guest@local") {
            allergensLoaded = false
            try {
                userAllergens = usersRepo.getAllergensByEmail(loggedEmail)
                prefs.setAllergens(userAllergens)
            } catch (_: Throwable) {
                userAllergens = normalizeAllergenSet(cachedLocal)
            } finally {
                allergensLoaded = true
            }
        } else {
            userAllergens = normalizeAllergenSet(cachedLocal)
            allergensLoaded = true
        }
    }

    if (!isLoggedIn) {
        AuthScreen(
            onLogin = { emailLower ->
                scope.launch { prefs.setLoggedIn(emailLower) }
            },
            onSkip = {
                scope.launch { prefs.setLoggedIn("guest@local") }
            }
        )
        return
    }

    if (!allergensLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val primaryItems = remember {
        listOf(Dest.Home, Dest.Scanner, Dest.Meals, Dest.IANutri)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            UserTopBar(
                loggedEmail = loggedEmail,
                onManageAllergens = { navigateToRoute(Dest.Settings.route) },
                onHistory = { navigateToRoute(Dest.IANutri.route) },
                onSupport = { navigateToRoute(Dest.Contact.route) },
                onLogout = { scope.launch { prefs.logout() } }
            )
        },
        bottomBar = {
            PrimaryBottomBar(
                items = primaryItems,
                currentRoute = currentRoute,
                onNavigate = { dest -> navigateToRoute(dest.route) }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.Meals.route) { MealsScreen(prefs = prefs) }
            composable(Dest.IANutri.route) {
                IANutriScreen(
                    email = loggedEmail,
                    allergens = userAllergens,
                    apiBaseUrl = BuildConfig.SAFEBYTE_API_BASE_URL
                )
            }
            composable(Dest.Scanner.route) { ScannerScreen(userAllergens = userAllergens) }
            composable(Dest.Settings.route) {
                SettingsScreen(
                    emailLower = loggedEmail,
                    currentAllergens = userAllergens,
                    onAllergensChanged = { newSet ->
                        userAllergens = normalizeAllergenSet(newSet)
                        scope.launch {
                            try {
                                prefs.setAllergens(userAllergens)
                                if (loggedEmail.isNotBlank() && loggedEmail != "guest@local") {
                                    usersRepo.setAllergensByEmail(loggedEmail, userAllergens)
                                }
                            } catch (_: Throwable) {
                                // Evita que una falla de red/capa remota cierre la app.
                            }
                        }
                    }
                )
            }
            composable(Dest.Contact.route) { ContactScreen() }
        }
    }
}

@Composable
private fun UserTopBar(
    loggedEmail: String,
    onManageAllergens: () -> Unit,
    onHistory: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit
) {
    val safeEmail = loggedEmail.ifBlank { "usuario@safebyte.com" }
    val isGuest = safeEmail == "guest@local"
    val displayName = if (isGuest) {
        "Invitado"
    } else {
        safeEmail.substringBefore("@").ifBlank { "Usuario" }
    }
    val displayEmail = if (isGuest) "guest@local" else safeEmail
    val initial = displayName.take(1).uppercase()

    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = BrandGreen,
        shadowElevation = 8.dp,
        modifier = Modifier.statusBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            val compact = maxWidth < 520.dp
            val menuWidth = (maxWidth - 8.dp).coerceIn(220.dp, 340.dp)

            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Food DNA",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        UserIdentityChip(
                            initial = initial,
                            displayName = displayName,
                            displayEmail = displayEmail,
                            expanded = expanded,
                            onToggleExpanded = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        )

                        UserDropdownMenu(
                            expanded = expanded,
                            menuWidth = menuWidth,
                            onDismiss = { expanded = false },
                            onManageAllergens = {
                                expanded = false
                                onManageAllergens()
                            },
                            onHistory = {
                                expanded = false
                                onHistory()
                            },
                            onSupport = {
                                expanded = false
                                onSupport()
                            },
                            onLogout = {
                                expanded = false
                                onLogout()
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Food DNA",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.weight(1f))

                    Box {
                        UserIdentityChip(
                            initial = initial,
                            displayName = displayName,
                            displayEmail = displayEmail,
                            expanded = expanded,
                            onToggleExpanded = { expanded = !expanded },
                            modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)
                        )

                        UserDropdownMenu(
                            expanded = expanded,
                            menuWidth = menuWidth,
                            onDismiss = { expanded = false },
                            onManageAllergens = {
                                expanded = false
                                onManageAllergens()
                            },
                            onHistory = {
                                expanded = false
                                onHistory()
                            },
                            onSupport = {
                                expanded = false
                                onSupport()
                            },
                            onLogout = {
                                expanded = false
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMenuAction(
    label: String,
    backgroundColor: Color,
    textColor: Color = BrandGreen,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PrimaryBottomBar(
    items: List<Dest>,
    currentRoute: String?,
    onNavigate: (Dest) -> Unit
) {
    Surface(
        color = BrandGreen,
        shadowElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 380.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = if (compact) Arrangement.spacedBy(4.dp) else Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { dest ->
                    val selected = currentRoute == dest.route
                    Text(
                        text = dest.label,
                        color = if (selected) AccentLime else Color.White,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onNavigate(dest) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserIdentityChip(
    initial: String,
    displayName: String,
    displayEmail: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UserChipGreen)
            .clickable { onToggleExpanded() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentLime),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = BrandGreen,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displayEmail,
                color = Color(0xFFE6EFE9),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = "Abrir menu de usuario",
            tint = Color.White
        )
    }
}

@Composable
private fun UserDropdownMenu(
    expanded: Boolean,
    menuWidth: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    onManageAllergens: () -> Unit,
    onHistory: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(menuWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UserMenuAction(
                label = "Gestionar alergenos",
                backgroundColor = MenuItemGray
            ) { onManageAllergens() }
            UserMenuAction(
                label = "Historial",
                backgroundColor = MenuItemGray
            ) { onHistory() }
            UserMenuAction(
                label = "Soporte",
                backgroundColor = MenuItemGray
            ) { onSupport() }
            UserMenuAction(
                label = "Cerrar sesion",
                backgroundColor = MenuDangerBg,
                textColor = MenuDangerText
            ) { onLogout() }
        }
    }
}
