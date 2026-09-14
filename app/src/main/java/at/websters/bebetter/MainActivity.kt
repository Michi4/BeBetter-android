package at.websters.bebetter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.components.BeBetterLogo
import at.websters.bebetter.ui.screens.*
import at.websters.bebetter.ui.theme.BeBetterTheme
import at.websters.bebetter.ui.theme.BeBetterTokens
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val DASHBOARD = "dashboard"
    const val HABITS = "habits"
    const val HABIT_DETAIL = "habit/{id}"
    const val TASKS = "tasks"
    const val GRID = "grid"
    const val FRIENDS = "friends"
    const val CHALLENGES = "challenges"
    const val CHALLENGE_DETAIL = "challenge/{id}"
    const val NEW_CHALLENGE = "challenge_new"
    const val PRESETS = "presets"
    const val PRESET_DETAIL = "preset/{id}"
    const val LEADERBOARD = "leaderboard"
    const val NOTIFICATIONS = "notifications"
    const val ASSISTANT = "assistant"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val SETTINGS = "settings"
    fun habit(id: String) = "habit/$id"
    fun challenge(id: String) = "challenge/$id"
    fun preset(id: String) = "preset/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Phone does not turn off while BeBetter is in the foreground.
        // Default ON (user request); toggle in Settings. Mirrors "never sleep during workouts".
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val app = applicationContext as BeBetterApp
            val keepOn by app.session.keepScreenOnFlow.collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(keepOn) {
                if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            BeBetterTheme {
                BeBetterNav()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeBetterNav() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as BeBetterApp
    val session = remember { app.session }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isAdmin by remember { mutableStateOf(false) }
    var isDemo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val t = session.getToken()
        val base = session.getBaseUrl()
        ApiClient.setBaseUrl(base)
        if (!t.isNullOrBlank()) {
            val me = runCatching { ApiClient.get().me().user }.getOrNull()
            if (me == null) {
                session.clearToken()
                ApiClient.invalidate()
                token = null
            } else {
                token = t
                isAdmin = me.role == "admin"
                isDemo = me.isDemo
            }
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BeBetterTokens.Accent)
        }
        return
    }

    val start = if (token.isNullOrBlank()) Routes.LOGIN else Routes.DASHBOARD

    data class NavItem(val route: String, val label: String, val icon: @Composable (Boolean) -> Unit)

    @Composable
    fun navIcon(vector: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean): @Composable () -> Unit = {
        Icon(vector, null, tint = if (active) BeBetterTokens.Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }

    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: ""
    fun isActive(to: String): Boolean = when (to) {
        Routes.DASHBOARD -> route == Routes.DASHBOARD
        Routes.HABITS -> route.startsWith("habit")
        Routes.FRIENDS -> route == Routes.FRIENDS || route.startsWith("challenge")
        Routes.PRESETS -> route.startsWith("preset")
        Routes.ADMIN -> route == Routes.ADMIN
        else -> route.startsWith(to)
    }

    // Web parity: desktop top nav = Home/Habits/Friends/Ranks/Presets(+Admin); mobile bottom = same 5.
    // Android bottom bar shows the same 5 (Today/Habits/Social/Ranks/Presets) to stay tidied up.
    val bottomRoutes = buildList {
        add(Triple(Routes.DASHBOARD, "Home", Icons.Filled.Home))
        add(Triple(Routes.HABITS, "Habits", Icons.AutoMirrored.Filled.List))
        add(Triple(Routes.FRIENDS, "Friends", Icons.Filled.People))
        add(Triple(Routes.LEADERBOARD, "Ranks", Icons.Filled.Star))
        add(Triple(Routes.PRESETS, "Presets", Icons.Filled.Book))
        if (isAdmin) add(Triple(Routes.ADMIN, "Admin", Icons.Filled.Shield))
    }

    val showChrome = route !in listOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showChrome && token != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    bottomRoutes.take(5).forEach { (r, label, icon) ->
                        val active = isActive(r)
                        // Demo lock like web: Friends/Ranks need real account
                        val locked = isDemo && (r == Routes.FRIENDS || r == Routes.LEADERBOARD)
                        NavigationBarItem(
                            selected = active,
                            onClick = {
                                if (locked) nav.navigate(Routes.PROFILE)
                                else nav.navigate(r) { launchSingleTop = true; popUpTo(Routes.DASHBOARD) }
                            },
                            icon = { Icon(icon, label, tint = if (active) BeBetterTokens.Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)) },
                            label = { Text(label, fontSize = 10.sp, color = if (active) BeBetterTokens.Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = BeBetterTokens.Accent.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        },
        topBar = {
            if (showChrome && token != null) {
                TopAppBar(
                    modifier = Modifier.height(48.dp),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.86f)),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BeBetterLogo(size = 28.dp)
                            Text(
                                "BeBetter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                style = LocalTextStyle.current.copy(
                                    brush = Brush.linearGradient(listOf(BeBetterTokens.Accent, Color(0xFF6EE7B7)))
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val cur = session.getTheme()
                                val isDarkNow = cur != "light"
                                session.saveTheme(if (isDarkNow) "light" else "dark")
                                // force recompose via token bump
                                token = token
                            }
                        }) { Icon(Icons.Filled.BrightnessMedium, "Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (!isDemo) {
                            IconButton(onClick = { nav.navigate(Routes.ASSISTANT) }) { Icon(Icons.Filled.AutoAwesome, "Assistant", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        IconButton(onClick = { nav.navigate(Routes.NOTIFICATIONS) }) { Icon(Icons.Filled.Notifications, "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { nav.navigate(Routes.PROFILE) }) { Icon(Icons.Filled.Person, "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                )
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = start, Modifier.padding(pad)) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = { t, admin, demo ->
                        token = t; isAdmin = admin; isDemo = demo
                        nav.navigate(Routes.DASHBOARD) { popUpTo(0) }
                    },
                    onRegister = { nav.navigate(Routes.REGISTER) },
                    onForgot = { nav.navigate(Routes.FORGOT) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(onDone = { t -> token = t; nav.navigate(Routes.DASHBOARD) { popUpTo(0) } }, onBack = { nav.popBackStack() })
            }
            composable(Routes.FORGOT) { ForgotScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onHabit = { nav.navigate(Routes.habit(it)) },
                    onSeeHabits = { nav.navigate(Routes.HABITS) },
                    onSeeTasks = { nav.navigate(Routes.TASKS) },
                    onChallenge = { nav.navigate(Routes.challenge(it)) },
                    onOpen = { nav.navigate(it) }
                )
            }
            composable(Routes.HABITS) { HabitsScreen(onDetail = { nav.navigate(Routes.habit(it)) }) }
            composable(Routes.HABIT_DETAIL, listOf(navArgument("id") { type = NavType.StringType })) {
                HabitDetailScreen(id = it.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
            }
            composable(Routes.TASKS) { TasksScreen() }
            composable(Routes.GRID) { GridScreen() }
            composable(Routes.FRIENDS) {
                FriendsScreen(onChallenge = { nav.navigate(Routes.challenge(it)) }, onNewChallenge = { nav.navigate(Routes.NEW_CHALLENGE) })
            }
            composable(Routes.CHALLENGES) {
                ChallengesScreen(onDetail = { nav.navigate(Routes.challenge(it)) }, onNew = { nav.navigate(Routes.NEW_CHALLENGE) })
            }
            composable(Routes.NEW_CHALLENGE) { NewChallengeScreen(onDone = { nav.popBackStack() }) }
            composable(Routes.CHALLENGE_DETAIL, listOf(navArgument("id") { type = NavType.StringType })) {
                ChallengeDetailScreen(id = it.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
            }
            composable(Routes.PRESETS) { PresetsScreen(onDetail = { nav.navigate(Routes.preset(it)) }) }
            composable(Routes.PRESET_DETAIL, listOf(navArgument("id") { type = NavType.StringType })) {
                PresetDetailScreen(id = it.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
            }
            composable(Routes.LEADERBOARD) { LeaderboardScreen() }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
            composable(Routes.ASSISTANT) { AssistantScreen() }
            composable(Routes.PROFILE) { ProfileScreen(onAdmin = { nav.navigate(Routes.ADMIN) }) }
            composable(Routes.ADMIN) { AdminScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLogout = { scope.launch { session.clearToken(); ApiClient.invalidate(); token = null; nav.navigate(Routes.LOGIN) { popUpTo(0) } } },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
