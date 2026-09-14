package at.websters.bebetter.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { WearApp() }
    }
}

@Composable
fun WearApp() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val session = remember { WearSession(ctx.applicationContext) }
    val nav = rememberSwipeDismissableNavController()
    var token by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val t = session.token()
        WearClient.init(session.base()) { runCatching { kotlinx.coroutines.runBlocking { session.token() } }.getOrNull() }
        if (!t.isNullOrBlank() && runCatching { WearClient.get().me() }.isSuccess) token = t
        else { session.clear(); WearClient.reset() }
        ready = true
    }
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0C0F))) {
            if (!ready) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (token == null) {
                WearLogin(session) { token = it }
            } else {
                WearNav(nav, session) { token = null }
            }
        }
    }
}

@Composable
fun WearLogin(session: WearSession, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    ScalingLazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text("BeBetter", fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
            Text("Sign in on your watch", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
        item {
            androidx.compose.material3.OutlinedTextField(id, { id = it }, label = { androidx.compose.material3.Text("Email/user", fontSize = 11.sp) }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.OutlinedTextField(pw, { pw = it }, label = { androidx.compose.material3.Text("Password", fontSize = 11.sp) }, singleLine = true)
        }
        item {
            err?.let { Text(it, color = Color(0xFFF87171)) }
            Button(onClick = {
                busy = true
                scope.launch {
                    try {
                        WearClient.init(session.base()) { null }
                        val r = WearClient.get().login(mapOf("email" to id.trim(), "password" to pw))
                        session.saveToken(r.token)
                        WearClient.init(session.base()) { runCatching { kotlinx.coroutines.runBlocking { session.token() } }.getOrNull() }
                        onDone(r.token)
                    } catch (e: Exception) { err = e.message; busy = false }
                }
            }, enabled = !busy && id.isNotBlank() && pw.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "…" else "Sign In")
            }
        }
    }
}

@Composable
fun WearNav(nav: NavHostController, session: WearSession, onLogout: () -> Unit) {
    SwipeDismissableNavHost(nav, startDestination = "today") {
        composable("today") { WearToday(nav, session, onLogout) }
        composable("assistant") { WearAssistant() }
        composable("stats") { WearStatsScreen() }
    }
}

@Composable
fun WearToday(nav: NavHostController, session: WearSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var habits by remember { mutableStateOf<List<WearHabit>>(emptyList()) }
    var stats by remember { mutableStateOf<WearStats?>(null) }
    fun load() {
        scope.launch {
            try {
                habits = WearClient.get().scheduled(LocalDate.now().toString()).habits
                stats = WearClient.get().stats()
            } catch (_: Exception) {}
        }
    }
    LaunchedEffect(Unit) { load() }
    ScalingLazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Today ${habits.count { it.completedToday == true }}/${habits.size}", fontWeight = FontWeight.Bold)
            stats?.let { Text("🔥 ${it.activeStreak}d • ${it.consistency}%", color = Color(0xFF34D399)) }
        }
        items(habits.take(20)) { h ->
            Chip(
                onClick = {
                    scope.launch {
                        try {
                            if (h.completedToday != true) WearClient.get().complete(mapOf("habitId" to h.id, "scheduledTime" to h.scheduledTime))
                            load()
                        } catch (_: Exception) {}
                    }
                },
                label = { Text("${h.emoji.ifBlank { "🌱" }} ${h.title}", maxLines = 2) },
                secondaryLabel = { h.scheduledTime?.let { Text(it) } },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (h.completedToday == true) Color(0xFF064E3B) else Color(0xFF14171D)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            CompactButton(onClick = { nav.navigate("assistant") }) { Text("✨ Assistant") }
            CompactButton(onClick = { nav.navigate("stats") }) { Text("📊 Stats") }
            CompactButton(onClick = {
                scope.launch { session.clear(); WearClient.reset(); onLogout() }
            }) { Text("Logout") }
        }
    }
}

@Composable
fun WearStatsScreen() {
    val scope = rememberCoroutineScope()
    var s by remember { mutableStateOf<WearStats?>(null) }
    LaunchedEffect(Unit) { scope.launch { s = runCatching { WearClient.get().stats() }.getOrNull() } }
    ScalingLazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text("Stats", fontWeight = FontWeight.Bold)
            val v = s
            if (v == null) CircularProgressIndicator()
            else {
                Text("🔥 ${v.activeStreak}-day streak")
                Text("✅ ${v.todayLogs} today")
                Text("📈 ${v.consistency}% consistent")
                Text("🏆 ${v.totalLogs} total")
            }
        }
    }
}

@Composable
fun WearAssistant() {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var log by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    ScalingLazyColumn(Modifier.fillMaxSize()) {
        item { Text("✨ Assistant", fontWeight = FontWeight.Bold) }
        items(log) { (_, t) ->
            Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text(t, modifier = Modifier.padding(8.dp))
            }
        }
        item {
            androidx.compose.material3.OutlinedTextField(q, { q = it }, label = { androidx.compose.material3.Text("Ask…") })
            Button(onClick = {
                val msg = q.trim(); if (msg.isBlank()) return@Button
                q = ""; busy = true
                log = log + ("user" to msg)
                scope.launch {
                    try {
                        val r = WearClient.get().chat(mapOf("message" to msg))
                        val text = r["reply"]?.asString ?: r["message"]?.asString ?: r.toString().take(400)
                        log = log + ("ai" to text)
                    } catch (e: Exception) { log = log + ("ai" to "Error: ${e.message}") }
                    busy = false
                }
            }, enabled = !busy && q.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Send")
            }
        }
    }
}
