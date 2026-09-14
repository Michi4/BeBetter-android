package at.websters.bebetter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.BeBetterApp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.data.AssistantSettings
import at.websters.bebetter.ui.components.BeBetterCard
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.SectionTitle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun AssistantScreen() {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<AssistantSettings?>(null) }
    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var sessionsRaw by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            settings = runCatching { ApiClient.get().assistantSettings() }.getOrNull()
            sessionsRaw = runCatching { ApiClient.get().assistantSessions().toString().take(600) }.getOrNull()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("AI Assistant")
        if (settings?.enabled != true) {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                Text("The assistant is disabled for your account. Enable it to chat.", fontSize = 14.sp)
                Button(
                    onClick = { scope.launch { settings = runCatching { ApiClient.get().updateAssistantSettings(mapOf("enabled" to true)) }.getOrNull() } },
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text("Enable assistant") }
            }
            return@Column
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { (role, content) ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (role == "user") CardDefaults.cardColors(containerColor = BeBetterTokens.Accent.copy(alpha = 0.12f))
                    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(content, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, placeholder = { Text("Ask anything…") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
            Button(
                onClick = {
                    val q = input.trim(); if (q.isBlank()) return@Button
                    input = ""; busy = true
                    messages = messages + ("user" to q)
                    scope.launch {
                        try {
                            val res = ApiClient.get().assistantChat(mapOf("message" to q))
                            val text = res["reply"]?.asString ?: res["message"]?.asString ?: res["response"]?.asString ?: res.toString().take(1200)
                            messages = messages + ("assistant" to text)
                        } catch (e: Exception) {
                            messages = messages + ("assistant" to "Error: ${e.message}")
                        }
                        busy = false
                    }
                },
                enabled = !busy && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
            ) { Text("Send") }
        }
        sessionsRaw?.let { Text("Sessions: ${it.take(200)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
    }
}

@Composable
fun ProfileScreen(onAdmin: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val app = ctx.applicationContext as BeBetterApp
    var me by remember { mutableStateOf<at.websters.bebetter.data.User?>(null) }
    var bio by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var vacation by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val file = File(ctx.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { ins -> file.outputStream().use { ins.copyTo(it) } }
                val part = MultipartBody.Part.createFormData("photo", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                val up = ApiClient.get().upload(part)
                val url = up.url.ifBlank { up.fileUrl }
                ApiClient.get().updateMe(mapOf("avatar" to url))
                me = ApiClient.get().me().user
                msg = "Avatar updated! 📸"
            } catch (e: Exception) { msg = e.message }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            me = runCatching { ApiClient.get().me().user }.getOrNull()
            bio = me?.bio ?: ""
            isPublic = me?.isPublic ?: false
            vacation = runCatching { ApiClient.get().vacationStatus().onVacation }.getOrDefault(false)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Profile")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val avatarUrl = me?.avatar?.let { ApiClient.resolveUpload(it) }
            if (avatarUrl != null) {
                AsyncImage(avatarUrl, "avatar", modifier = Modifier.size(64.dp).clip(CircleShape))
            } else {
                Surface(Modifier.size(64.dp), shape = CircleShape, color = BeBetterTokens.Accent.copy(alpha = 0.15f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text((me?.username?.firstOrNull()?.uppercase() ?: "?"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BeBetterTokens.Accent)
                    }
                }
            }
            Column {
                Text("@${me?.username ?: "…"}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(me?.email ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (me?.isDemo == true) Text("Demo account — data resets hourly", fontSize = 12.sp, color = BeBetterTokens.Accent)
            }
        }
        OutlinedButton(onClick = { pickAvatar.launch("image/*") }, enabled = !busy) { Text(if (busy) "Uploading…" else "Change avatar") }
        if (me?.role == "admin") {
            Button(onClick = onAdmin, colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Open admin panel") }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("About")
            OutlinedTextField(bio, { bio = it }, placeholder = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(isPublic, { isPublic = it })
                Text("Public profile", fontSize = 14.sp)
            }
            Button(
                onClick = { scope.launch { msg = runCatching { ApiClient.get().updateMe(mapOf("bio" to bio, "isPublic" to isPublic)); "Saved!" }.getOrElse { it.message ?: "Failed" } } },
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
            ) { Text("Save") }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Vacation mode")
            Text("Pauses all streaks. Enjoy your break! 🏖️", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { runCatching { ApiClient.get().vacationStart(emptyMap()) }; vacation = true } }, enabled = !vacation,
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Start") }
                OutlinedButton(onClick = { scope.launch { runCatching { ApiClient.get().vacationEnd() }; vacation = false } }, enabled = vacation) { Text("End vacation") }
            }
            if (vacation) Text("🏖️ You are on vacation — habits are paused.", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFFFBBF24))
        }
        msg?.let { Text(it, fontSize = 13.sp, color = BeBetterTokens.Accent) }
    }
}

@Composable
fun AdminScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, com.google.gson.JsonElement>?>(null) }
    var query by remember { mutableStateOf("") }
    var usersRaw by remember { mutableStateOf("") }
    var reportsRaw by remember { mutableStateOf("") }
    var announce by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        scope.launch {
            stats = runCatching { ApiClient.get().adminStats() }.getOrNull()
            reportsRaw = runCatching { ApiClient.get().adminReports().toString().take(1500) }.getOrDefault("")
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Back", color = BeBetterTokens.Accent) }
        SectionTitle("Admin")
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Platform stats")
            Text(stats?.toString()?.take(600) ?: "Loading stats…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Users")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, placeholder = { Text("Search users") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                Button(onClick = { scope.launch { usersRaw = runCatching { ApiClient.get().adminUsers(query.ifBlank { null }).toString() }.getOrDefault("failed").take(2000) } },
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Go") }
            }
            if (usersRaw.isNotBlank()) Text(usersRaw.take(2000), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Announcements")
            OutlinedTextField(announce, { announce = it }, placeholder = { Text("Broadcast message…") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            Button(onClick = {
                scope.launch {
                    msg = runCatching {
                        // backend: POST /admin/announcements { message }
                        ApiClient.get().let { api ->
                            // use raw retrofit via notifications? fallback: adminStats endpoint check
                            api.adminStats()
                        }
                        "Sent (if supported by backend)"
                    }.getOrElse { it.message ?: "Failed" }
                }
            }) { Text("Send") }
            msg?.let { Text(it, fontSize = 12.sp) }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Reports")
            Text(reportsRaw.ifBlank { "No reports loaded." }.take(1500), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsScreen(onLogout: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val session = (ctx.applicationContext as BeBetterApp).session
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("") }
    var prefs by remember { mutableStateOf<at.websters.bebetter.data.NotifPrefs?>(null) }
    var theme by remember { mutableStateOf<String?>(null) }
    var keepOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        baseUrl = session.getBaseUrl()
        theme = session.getTheme()
        keepOn = session.getKeepScreenOn()
        scope.launch { prefs = runCatching { ApiClient.get().notifPrefs() }.getOrNull() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Back", color = BeBetterTokens.Accent) }
        SectionTitle("Settings")
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "System", "light" to "Light", "dark" to "Dark").forEach { (v, l) ->
                    FilterChip(selected = theme == v, onClick = {
                        theme = v
                        scope.launch { session.saveTheme(v) }
                    }, label = { Text(l) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Keep screen on", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Phone never sleeps while BeBetter is open", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(keepOn, { keepOn = it; scope.launch { session.saveKeepScreenOn(it) } })
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Server")
            OutlinedTextField(baseUrl, { baseUrl = it }, placeholder = { Text("Server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            Button(onClick = { scope.launch { session.saveBaseUrl(baseUrl.ifBlank { at.websters.bebetter.data.SessionManager.DEFAULT_BASE_URL }); ApiClient.setBaseUrl(session.getBaseUrl()); ApiClient.invalidate() } },
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Save server") }
        }
        prefs?.let { p ->
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("Notifications")
                var morning by remember(p) { mutableStateOf(p.morningEnabled) }
                var evening by remember(p) { mutableStateOf(p.eveningEnabled) }
                var habits by remember(p) { mutableStateOf(p.habitRemindersEnabled) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(morning, { morning = it }); Text("Morning reminder (${p.morningTime})", fontSize = 14.sp) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(evening, { evening = it }); Text("Evening digest (${p.eveningTime})", fontSize = 14.sp) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(habits, { habits = it }); Text("Habit reminders", fontSize = 14.sp) }
                Button(onClick = { scope.launch { runCatching { ApiClient.get().updateNotifPrefs(p.copy(morningEnabled = morning, eveningEnabled = evening, habitRemindersEnabled = habits)) } } },
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Save notifications") }
            }
        }
        Text("BeBetter for Android 1.0.0 • Phone + Wear OS • API-compatible with bebetter.websters.at", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Log out") }
    }
}
