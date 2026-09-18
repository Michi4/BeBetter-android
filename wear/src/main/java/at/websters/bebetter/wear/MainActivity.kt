package at.websters.bebetter.wear

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Bg = Color(0xFF0B0C0F)
private val Accent = Color(0xFF34D399)
private val CardBg = Color(0xFF14171D)
private val Muted = Color(0xFF9CA3AF)
private val ErrColor = Color(0xFFF87171)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val start = intent?.getStringExtra("screen") ?: "today"
        setContent { WearApp(startScreen = start) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

@Composable
fun WearApp(startScreen: String = "today") {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val session = remember { WearSession(ctx.applicationContext) }
    val nav = rememberSwipeDismissableNavController()
    var token by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { session.ensureMigrated() }
        WearClient.init(session.base()) { session.cachedToken }
        val t = session.token()
        if (!t.isNullOrBlank() && runCatching { WearClient.get().me() }.isSuccess) token = t
        else { session.clear(); WearClient.reset() }
        ready = true
    }
    MaterialTheme(
        colors = Colors(
            primary = Accent,
            primaryVariant = Color(0xFF059669),
            onPrimary = Color.Black,
            secondary = Accent,
            secondaryVariant = Accent,
            onSecondary = Color.Black,
            background = Bg,
            surface = CardBg,
            onSurface = Color.White,
            onBackground = Color.White,
            error = ErrColor,
            onError = Color.Black
        )
    ) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            when {
                !ready -> androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center), color = Accent)
                token == null -> WearLogin(session) { token = it }
                else -> WearNav(nav, session, startScreen) { token = null }
            }
        }
    }
}

@Composable
fun WearLogin(session: WearSession, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun submit() {
        if (busy || id.isBlank() || pw.isBlank()) return
        busy = true; err = null
        keyboard?.hide()
        scope.launch {
            try {
                WearClient.init(session.base()) { null }
                val r = WearClient.get().login(mapOf("email" to id.trim(), "password" to pw))
                session.saveToken(r.token)
                WearClient.init(session.base()) { runCatching { kotlinx.coroutines.runBlocking { session.token() } }.getOrNull() }
                onDone(r.token)
            } catch (e: Exception) {
                err = when {
                    e.message?.contains("Unable to resolve host") == true -> "No internet"
                    e.message?.contains("401") == true -> "Wrong email or password"
                    else -> e.message?.take(60) ?: "Login failed"
                }
                busy = false
            }
        }
    }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BeBetter", fontWeight = FontWeight.Bold, color = Accent, fontSize = 20.sp)
                Text("Sign in on your watch", fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
            }
        }
        item {
            WearInput(value = id, onValueChange = { id = it }, label = "Email", placeholder = "you@example.com")
        }
        item {
            WearInput(value = pw, onValueChange = { pw = it }, label = "Password", password = true, imeAction = ImeAction.Done, onSubmit = { submit() })
        }
        item {
            err?.let {
                Text(it, color = ErrColor, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Button(
                onClick = { submit() },
                enabled = !busy && id.isNotBlank() && pw.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Accent, disabledBackgroundColor = CardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (busy) "Signing in…" else "Sign in",
                    color = if (busy) Muted else Color.Black,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            Text(sessionInfoText(), fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun sessionInfoText(): String = "Server: app.bebetter.websters.at"

@Composable
fun WearNav(nav: NavHostController, session: WearSession, startScreen: String, onLogout: () -> Unit) {
    SwipeDismissableNavHost(nav, startDestination = if (startScreen == "assistant") "assistant" else "today") {
        composable("today") { WearToday(nav, session, onLogout) }
        composable("assistant") { WearAssistant(session) }
        composable("stats") { WearStatsScreen() }
    }
}

@Composable
fun WearToday(nav: NavHostController, session: WearSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var habits by remember { mutableStateOf<List<WearHabit>>(emptyList()) }
    var stats by remember { mutableStateOf<WearStats?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    fun load() {
        scope.launch {
            try {
                habits = WearClient.get().scheduled(LocalDate.now().toString()).habits
                stats = WearClient.get().stats()
                err = null
            } catch (e: Exception) {
                err = e.message?.take(40)
            }
            loaded = true
        }
    }
    LaunchedEffect(Unit) { load() }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${habits.count { it.completedToday == true }}/${habits.size} done  •  🔥 ${stats?.activeStreak ?: 0}d",
                    fontSize = 12.sp, color = Accent
                )
                err?.let { Text(it, color = ErrColor, fontSize = 10.sp, textAlign = TextAlign.Center) }
            }
        }
        if (!loaded && habits.isEmpty()) {
            item { androidx.compose.material3.CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp)) }
        }
        if (loaded && habits.isEmpty() && err == null) {
            item { Text("Nothing scheduled today 🎉", fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center) }
        }
        items(habits.take(25)) { h ->
            val done = h.completedToday == true
            WearHabitRow(
                done = done,
                title = h.title,
                time = h.scheduledTime,
                onClick = {
                    scope.launch {
                        try {
                            if (done) WearClient.get().undo(h.id, h.scheduledTime, LocalDate.now().toString())
                            else WearClient.get().complete(mapOf("habitId" to h.id, "scheduledTime" to h.scheduledTime))
                            habits = habits.map {
                                if (it.id == h.id && it.scheduledTime == h.scheduledTime) it.copy(completedToday = !done) else it
                            }
                            stats = runCatching { WearClient.get().stats() }.getOrNull()
                        } catch (e: Exception) { err = e.message?.take(40) }
                    }
                }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactButton(onClick = { nav.navigate("assistant") }) {
                    Text("✨", fontSize = 15.sp)
                }
                CompactButton(onClick = { nav.navigate("stats") }) {
                    Text("📊", fontSize = 15.sp)
                }
                CompactButton(onClick = { load() }) {
                    Text("↻", fontSize = 15.sp)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { scope.launch { session.clear(); WearClient.reset(); onLogout() } },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log out", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WearStatsScreen() {
    val scope = rememberCoroutineScope()
    var s by remember { mutableStateOf<WearStats?>(null) }
    LaunchedEffect(Unit) { scope.launch { s = runCatching { WearClient.get().stats() }.getOrNull() } }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Text("Stats", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        if (s == null) {
            item { androidx.compose.material3.CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp)) }
        } else {
            val v = s!!
            item { StatLine("🔥", "${v.activeStreak}-day streak") }
            item { StatLine("✅", "${v.todayLogs} done today") }
            item { StatLine("📈", "${v.consistency}% consistent") }
            item { StatLine("🏆", "${v.totalLogs} total check-ins") }
        }
    }
}

@Composable
fun StatLine(emoji: String, text: String) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Text(text, fontSize = 13.sp, color = if (text.contains("streak")) Accent else Color.White)
        }
    }
}

@Composable
fun WearAssistant(session: WearSession) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var q by remember { mutableStateOf("") }
    var log by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val listState = rememberScalingLazyListState()
    fun send(text: String) {
        val msg = text.trim()
        if (msg.isBlank() || busy) return
        q = ""
        busy = true; err = null
        keyboard?.hide()
        log = log + ("user" to msg) + ("ai" to "")
        val history = log.dropLast(1).map { (r, t) -> (if (r == "user") "user" else "assistant") to t }
        scope.launch {
            var partial = ""
            WearClient.chatStream(
                messages = history,
                sessionId = session.sessionId(),
                onDelta = { d ->
                    partial += d
                    log = log.dropLast(1) + ("ai" to partial)
                },
                onDone = { full, sid ->
                    log = log.dropLast(1) + ("ai" to full)
                    sid?.let { id -> scope.launch { session.saveSessionId(id) } }
                    busy = false
                },
                onError = { e ->
                    log = log.dropLast(1)
                    err = e
                    busy = false
                }
            )
        }
    }
    LaunchedEffect(log.size) {
        runCatching { listState.animateScrollToItem(if (log.isEmpty()) 0 else log.size + 2) }
    }
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✨ Assistant", fontWeight = FontWeight.Bold, color = Accent, fontSize = 15.sp)
                if (log.isEmpty()) Text("Ask about your habits, get advice.", fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center)
            }
        }
        if (log.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickChip("What should I do now?") { send("What should I do now?") }
                    QuickChip("Status update please") { send("Give me a quick status update.") }
                }
            }
        }
        items(log) { (role, text) ->
            if (role == "user") {
                Text(
                    text, fontSize = 12.sp, color = Color.Black, textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        .background(Accent, RoundedCornerShape(10.dp)).padding(8.dp)
                )
            } else {
                Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text.ifBlank { "…" }, fontSize = 12.sp, color = Color.White,
                        modifier = Modifier.padding(8.dp).let { if (busy) it.alpha(0.85f) else it }
                    )
                }
            }
        }
        item {
            err?.let { Text(it, color = ErrColor, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
        item {
            WearInput(value = q, onValueChange = { q = it }, label = if (busy) "Thinking…" else "Ask…", enabled = !busy, imeAction = ImeAction.Send, onSubmit = { send(q) })
        }
        item {
            Button(
                onClick = { send(q) },
                enabled = !busy && q.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Accent, disabledBackgroundColor = CardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "…" else "Send", color = if (busy) Muted else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 10.sp, maxLines = 2, color = Accent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun WearHabitRow(done: Boolean, title: String, time: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (done) Color(0xFF064E3B) else CardBg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (done) Text("✓", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            else Box(Modifier.size(15.dp).border(2.dp, Color(0xFF4B5563), CircleShape))
        }
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 2, fontSize = 13.sp, color = if (done) Muted else Color.White)
            time?.let { Text(it, fontSize = 11.sp, color = Muted) }
        }
    }
}

@Composable
fun WearInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onSubmit: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 10.sp, color = Muted, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(CardBg, RoundedCornerShape(8.dp))
                .border(1.dp, if (enabled) Color(0xFF374151) else Color(0xFF1F2937), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) Text(placeholder, fontSize = 12.sp, color = Muted)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }, onSend = { onSubmit?.invoke() }),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
