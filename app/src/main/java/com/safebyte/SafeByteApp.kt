package com.safebyte

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.safebyte.data.FirebaseUsersRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Dest("home", "Home", Icons.Filled.Home)
    data object Meals : Dest("meals", "Comidas", Icons.AutoMirrored.Filled.List)
    data object IANutri : Dest("ianutri", "IANutri", Icons.Filled.AutoAwesome)
    data object Scanner : Dest("scanner", "Scanner", Icons.Filled.CameraAlt)
    data object Settings : Dest("settings", "Config", Icons.Filled.Settings)
    data object Contact : Dest("contact", "Contacto", Icons.Filled.Mail)
}

@OptIn(ExperimentalMaterial3Api::class)
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

    // Cargar alérgenos desde Firestore cuando hay sesión (excepto guest)
    LaunchedEffect(isLoggedIn, loggedEmail) {
        val cachedLocal = prefs.allergens.first()
        if (isLoggedIn && loggedEmail.isNotBlank() && loggedEmail != "guest@local") {
            allergensLoaded = false
            try {
                userAllergens = usersRepo.getAllergensByEmail(loggedEmail)
                prefs.setAllergens(userAllergens) // cache local opcional
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

    val items = remember { listOf(Dest.Home, Dest.Meals, Dest.IANutri, Dest.Scanner, Dest.Settings, Dest.Contact) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTitle = when (currentRoute) {
        Dest.Home.route -> "Home"
        Dest.Meals.route -> "Comidas"
        Dest.IANutri.route -> "IANutri"
        Dest.Scanner.route -> "Scanner"
        Dest.Settings.route -> "Configuracion de Usuario"
        Dest.Contact.route -> "Contacta con Nosotros"
        else -> "SafeByte"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle) },
                actions = {
                    TextButton(onClick = { scope.launch { prefs.logout() } }) {
                        Text("Cerrar sesion")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Dest.Home.route) {
                HomeScreen()
            }
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
