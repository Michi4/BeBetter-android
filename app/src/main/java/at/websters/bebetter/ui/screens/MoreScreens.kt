@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import at.websters.bebetter.ui.components.WebChip
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.SectionTitle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate

@Composable
fun AssistantScreen() {
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var settings by remember { mutableStateOf<AssistantSettings?>(null) }
    var input by remember { mutableStateOf("") }
    // role: "user" | "assistant"; content string
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var history by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        settings = runCatching { ApiClient.get().assistantSettings() }.getOrNull()
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send(text: String) {
        val q = text.trim()
        if (q.isBlank() || busy) return
        busy = true
        err = null
        messages.add("user" to q)
        messages.add("assistant" to "")
        val userMsg = mapOf("role" to "user", "content" to q)
        val payload = history + userMsg
        ApiClient.chatStream(
            messages = payload,
            sessionId = sessionId,
            onDelta = { d ->
                val i = messages.size - 1
                messages[i] = "assistant" to (messages[i].second + d)
            },
            onDone = { full, sid ->
                val i = messages.size - 1
                if (i in messages.indices) messages[i] = "assistant" to full
                sessionId = sid ?: sessionId
                history = payload + mapOf("role" to "assistant", "content" to full)
                busy = false
            },
            onError = { e ->
                val i = messages.size - 1
                if (i in messages.indices && messages[i].second.isEmpty()) messages.removeAt(i)
                err = e
                busy = false
            }
        )
    }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AutoAwesome, null, tint = BeBetterTokens.Accent, modifier = Modifier.size(18.dp))
            Text("AI Assistant", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            WebChip("beta", "amber")
        }
        if (settings?.enabled != true) {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                Text("The assistant is disabled for your account. Enable it to chat.", fontSize = 14.sp)
                Button(
                    onClick = { scope.launch { settings = runCatching { ApiClient.get().updateAssistantSettings(mapOf("enabled" to true)) }.getOrNull() } },
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text("Enable assistant") }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = BeBetterTokens.Accent.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                        Text("Ask me to create habits, plan your day,\nor analyze your streaks.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("What should I focus on today?", "Suggest a new habit", "How is my streak?").forEach { s ->
                                OutlinedButton(onClick = { send(s) }, shape = RoundedCornerShape(20.dp)) { Text(s, fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
            items(messages, key = { "${it.first}-${messages.indexOf(it)}" }) { (role, content) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start) {
                    Card(
                        modifier = Modifier.widthIn(max = 300.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = if (role == "user") CardDefaults.cardColors(containerColor = BeBetterTokens.Accent.copy(alpha = 0.15f))
                        else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(content, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                    }
                }
            }
            if (busy) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = BeBetterTokens.Accent)
                        Text("Thinking…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            err?.let { e -> item { Text(e, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            OutlinedTextField(
                input, { input = it },
                placeholder = { Text("Ask anything…", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { send(input); input = "" })
            )
            FilledIconButton(
                onClick = { send(input); input = "" },
                enabled = !busy && input.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
        }
    }
}

@Composable
fun ProfileScreen(onAdmin: () -> Unit, onSettings: () -> Unit = {}) {
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
                msg = "Avatar updated"
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
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("Open Settings", fontSize = 13.sp) }
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
            Text("Pauses all streaks. Enjoy your break!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { runCatching { ApiClient.get().vacationStart(emptyMap()) }; vacation = true } }, enabled = !vacation,
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Start") }
                OutlinedButton(onClick = { scope.launch { runCatching { ApiClient.get().vacationEnd() }; vacation = false } }, enabled = vacation) { Text("End vacation") }
            }
            if (vacation) Text("You are on vacation — habits are paused.", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFFFBBF24))
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
            var announceTitle by remember { mutableStateOf("") }
            OutlinedTextField(announceTitle, { announceTitle = it }, placeholder = { Text("Title (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            Button(onClick = {
                scope.launch {
                    msg = runCatching {
                        ApiClient.get().sendAnnouncement(mapOf("title" to announceTitle.ifBlank { "Announcement" }, "message" to announce))
                        "Sent"
                    }.getOrElse {
                        (it as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(200) ?: (it.message ?: "Failed")
                    }
                }
            }, enabled = announce.isNotBlank()) { Text("Send") }
            msg?.let { Text(it, fontSize = 12.sp) }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Reports")
            Text(reportsRaw.ifBlank { "No reports loaded." }.take(1500), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WebSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .width(48.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) BeBetterTokens.AccentBtn else androidx.compose.ui.graphics.Color(0xFF374151))
            .clickable { onChange(!checked) }
    ) {
        Box(
            Modifier
                .padding(2.dp)
                .align(Alignment.CenterStart)
                .offset(x = if (checked) 24.dp else 0.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.White)
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, checked: Boolean, trailing: (@Composable () -> Unit)? = null, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
        WebSwitch(checked, onChecked)
    }
}

@Composable
fun SettingsScreen(onLogout: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val session = (ctx.applicationContext as BeBetterApp).session
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("") }
    var prefs by remember { mutableStateOf<at.websters.bebetter.data.NotifPrefs?>(null) }
    var ai by remember { mutableStateOf<AssistantSettings?>(null) }
    var theme by remember { mutableStateOf<String?>(null) }
    var keepOn by remember { mutableStateOf(true) }
    var vacation by remember { mutableStateOf(false) }
    var vacReason by remember { mutableStateOf("") }
    var vacEnd by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var pwCur by remember { mutableStateOf("") }
    var pwNew by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        baseUrl = session.getBaseUrl()
        theme = session.getTheme()
        keepOn = session.getKeepScreenOn()
        scope.launch {
            prefs = runCatching { ApiClient.get().notifPrefs() }.getOrNull()
            ai = runCatching { ApiClient.get().assistantSettings() }.getOrNull()
            vacation = runCatching { ApiClient.get().vacationStatus().onVacation }.getOrDefault(false)
        }
    }
    fun savePrefs(p: at.websters.bebetter.data.NotifPrefs) {
        prefs = p
        scope.launch { runCatching { ApiClient.get().updateNotifPrefs(p) } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack, modifier = Modifier) { Text("← Back", color = BeBetterTokens.Accent, fontSize = 14.sp) }
        SectionTitle("Settings")

        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Display Settings")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "System", "light" to "Light", "dark" to "Dark").forEach { (v, l) ->
                    FilterChip(
                        selected = theme == v,
                        onClick = { theme = v; scope.launch { session.saveTheme(v) } },
                        label = { Text(l, fontSize = 13.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BeBetterTokens.Accent.copy(alpha = 0.15f), selectedLabelColor = BeBetterTokens.Accent)
                    )
                }
            }
            SettingRow("Keep screen on", "Phone never sleeps while BeBetter is open", keepOn) { keepOn = it; scope.launch { session.saveKeepScreenOn(it) } }
        }

        prefs?.let { p ->
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                val all = p.morningEnabled || p.eveningEnabled || p.habitRemindersEnabled || p.announcementsEnabled
                SectionTitle("Notification Settings")
                SettingRow("All notifications", "Master switch", all) { v -> savePrefs(p.copy(morningEnabled = v, eveningEnabled = v, habitRemindersEnabled = v, announcementsEnabled = v)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                SettingRow("Morning reminder", "Start your day on track", p.morningEnabled, trailing = { Text(p.morningTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }) { savePrefs(p.copy(morningEnabled = it)) }
                SettingRow("Evening digest", "Reflect before bed", p.eveningEnabled, trailing = { Text(p.eveningTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }) { savePrefs(p.copy(eveningEnabled = it)) }
                SettingRow("Habit reminders", "Ping at each scheduled time", p.habitRemindersEnabled) { savePrefs(p.copy(habitRemindersEnabled = it)) }
                SettingRow("Announcements", "Product updates from the team", p.announcementsEnabled) { savePrefs(p.copy(announcementsEnabled = it)) }
            }
        }

        ai?.let { a ->
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("AI Assistant")
                SettingRow("Enable assistant", "Let the assistant read and shape your data", a.enabled) { v ->
                    ai = a.copy(enabled = v)
                    scope.launch { runCatching { ApiClient.get().updateAssistantSettings(mapOf("enabled" to v, "confirmBeforeExecute" to a.confirmBeforeExecute)) } }
                }
                SettingRow("Confirm before executing", "Ask before creating or editing things", a.confirmBeforeExecute) { v ->
                    ai = a.copy(confirmBeforeExecute = v)
                    scope.launch { runCatching { ApiClient.get().updateAssistantSettings(mapOf("enabled" to a.enabled, "confirmBeforeExecute" to v)) } }
                }
            }
        }

        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Vacation")
            if (vacation) {
                Text("You are on vacation — habits are paused.", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFFFBBF24))
            } else {
                Text("Going on vacation? Pause all habits so they don't count as missed.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(vacReason, { vacReason = it }, placeholder = { Text("e.g. Holiday, sick leave...", fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                OutlinedTextField(vacEnd, { vacEnd = it }, label = { Text("End date (YYYY-MM-DD)", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { scope.launch { runCatching { ApiClient.get().vacationStart(mapOf("reason" to vacReason.ifBlank { "Vacation" }, "endDate" to vacEnd)) }; vacation = true } },
                    enabled = !vacation,
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Start vacation", fontSize = 13.sp) }
                OutlinedButton(onClick = { scope.launch { runCatching { ApiClient.get().vacationEnd() }; vacation = false } }, enabled = vacation, shape = RoundedCornerShape(8.dp)) { Text("End now", fontSize = 13.sp) }
            }
        }

        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Security")
            OutlinedTextField(pwCur, { pwCur = it }, label = { Text("Current password", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(pwNew, { pwNew = it }, label = { Text("New password", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        msg = runCatching { ApiClient.get().changePassword(mapOf("currentPassword" to pwCur, "newPassword" to pwNew)); pwCur = ""; pwNew = ""; "Password changed" }.getOrElse { "Failed: ${it.message}" }
                    }
                },
                enabled = pwCur.isNotBlank() && pwNew.length >= 8,
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Change password", fontSize = 13.sp) }
        }

        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Server")
            OutlinedTextField(baseUrl, { baseUrl = it }, placeholder = { Text("Server URL", fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            Button(onClick = { scope.launch { session.saveBaseUrl(baseUrl.trimEnd('/').ifBlank { at.websters.bebetter.data.SessionManager.DEFAULT_BASE_URL }); ApiClient.setBaseUrl(session.getBaseUrl()); ApiClient.invalidate(); msg = "Server saved" } },
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(8.dp)) { Text("Save server", fontSize = 13.sp) }
        }

        msg?.let { Text(it, fontSize = 13.sp, color = BeBetterTokens.Accent) }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0x33EF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Danger Zone")
                OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete account permanently", fontSize = 13.sp) }
            }
        }
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("Log out", fontSize = 13.sp) }
        Text("BeBetter for Android 1.0.0 • Phone + Wear OS • bebetter.websters.at", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false; deleteConfirm = "" },
            title = { Text("Delete account?", color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This erases all habits, tasks, logs and streaks. Type DELETE_MY_ACCOUNT to confirm.", fontSize = 13.sp)
                    OutlinedTextField(deleteConfirm, { deleteConfirm = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deleteConfirm == "DELETE_MY_ACCOUNT",
                    onClick = {
                        showDelete = false
                        scope.launch {
                            runCatching { ApiClient.get().deleteAccount(mapOf("confirm" to "DELETE_MY_ACCOUNT")) }
                            runCatching { ApiClient.get().logout() }; session.clearToken(); ApiClient.invalidate(); onLogout()
                        }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false; deleteConfirm = "" }) { Text("Cancel") } }
        )
    }
}
