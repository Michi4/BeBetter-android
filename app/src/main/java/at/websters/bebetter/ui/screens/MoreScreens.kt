package at.websters.bebetter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Assistant", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("beta", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Text("✎", fontSize = 14.sp) }
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Text("◷", fontSize = 14.sp) }
                if ((sessionsRaw?.length ?: 0) > 2) Box(Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(BeBetterTokens.Accent), contentAlignment = Alignment.Center) { Text("1", fontSize = 8.sp, color = Color.White) }
            }
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Text("⚙", fontSize = 14.sp) }
        }
        if (settings?.enabled != true) {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                Text("The assistant is disabled for your account. Enable it to chat.", fontSize = 14.sp)
                Button(
                    onClick = { scope.launch { settings = runCatching { ApiClient.get().updateAssistantSettings(mapOf("enabled" to true)) }.getOrNull() } },
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)
                ) { Text("Enable assistant") }
            }
            return@Column
        }
        if (messages.isEmpty()) {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("✨", fontSize = 24.sp)
                    Text("Ask me anything about your habits & tasks —", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text("type below or tap the mic.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("What's left today?","Plan my day").forEach { s -> Box(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable{ input=s }.padding(horizontal=12.dp, vertical=8.dp)) { Text(s, fontSize=12.sp, color=MaterialTheme.colorScheme.onSurface) } }
                    }
                    Row { Box(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable{ input="Add task: buy milk tomorrow" }.padding(horizontal=12.dp, vertical=8.dp)) { Text("Add task: buy milk tomorrow", fontSize=12.sp, color=MaterialTheme.colorScheme.onSurface) } }
                }
            }
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
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable{ }, contentAlignment = Alignment.Center) { Text("🎤", fontSize=16.sp) }
            OutlinedTextField(input, { input = it }, placeholder = { Text("Ask or tell me what to do...", fontSize=13.sp) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            Button(
                onClick = {
                    val q = input.trim(); if (q.isBlank()) return@Button
                    input = ""; busy = true
                    val history = messages.map { (r,c) -> mapOf("role" to r, "content" to c) } + mapOf("role" to "user", "content" to q)
                    messages = messages + ("user" to q)
                    scope.launch {
                        try {
                            val res = ApiClient.get().assistantChat(mapOf("messages" to history))
                            val text = res["reply"]?.asString ?: res["message"]?.asString ?: res["response"]?.asString ?: res["content"]?.asString ?: res.toString().take(1200)
                            messages = messages + ("assistant" to text)
                        } catch (e: Exception) {
                            val code = (e as? retrofit2.HttpException)?.code()
                            val msg = when (code) {
                                403 -> "Assistant is disabled. Enable it in Profile → AI Assistant."
                                429 -> "Slow down — try again in a minute."
                                else -> e.message ?: "Failed — check connection"
                            }
                            messages = messages + ("assistant" to "Error: $msg")
                        }
                        busy = false
                    }
                },
                enabled = !busy && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)
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
    var timeFormat by remember { mutableStateOf("24h") }
    var notifPrefs by remember { mutableStateOf<at.websters.bebetter.data.NotifPrefs?>(null) }
    var assistantSettings by remember { mutableStateOf<AssistantSettings?>(null) }
    var vacReason by remember { mutableStateOf("") }
    var vacEnd by remember { mutableStateOf("") }
    var curPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf<Map<String, at.websters.bebetter.data.GridDay>>(emptyMap()) }

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
            notifPrefs = runCatching { ApiClient.get().notifPrefs() }.getOrNull()
            assistantSettings = runCatching { ApiClient.get().assistantSettings() }.getOrNull()
            val y = java.time.LocalDate.now().year
            grid = runCatching { ApiClient.get().grid("$y-01-01","$y-12-31").grid }.getOrDefault(emptyMap())
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (me?.avatar?.let { ApiClient.resolveUpload(it) } != null) {
                        AsyncImage(ApiClient.resolveUpload(me!!.avatar!!)!!, "avatar", modifier = Modifier.size(96.dp).clip(CircleShape))
                    } else {
                        Box(Modifier.size(96.dp).clip(CircleShape).background(BeBetterTokens.Accent), contentAlignment = Alignment.Center) {
                            Text((me?.username?.firstOrNull()?.uppercase() ?: "M"), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Box(Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable{ pickAvatar.launch("image/*") }, contentAlignment = Alignment.Center) {
                        Text("📷", fontSize = 14.sp)
                    }
                }
                Text(me?.username ?: "michi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Joined August 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BeBetterTokens.Accent); Text("Best Streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("4", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BeBetterTokens.Accent); Text("Habits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                }
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR ACTIVITY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Text("2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            at.websters.bebetter.ui.components.ContributionGridView(grid = grid, year = java.time.LocalDate.now().year)
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("👤", fontSize = 14.sp); Text("PROFILE SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
            Text("Bio", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            OutlinedTextField(bio, { bio = it }, placeholder = { Text("Tell something about yourself...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth().height(88.dp), shape = RoundedCornerShape(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(if (isPublic) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).clickable{ isPublic = !isPublic }, contentAlignment = Alignment.Center) { if (isPublic) Text("✓", fontSize = 12.sp, color = Color.White) }
                Column { Text("Public profile", fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("Others can view your profile and stats", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
            }
            Button(onClick = { scope.launch { msg = runCatching { ApiClient.get().updateMe(mapOf("bio" to bio, "isPublic" to isPublic)); "Saved!" }.getOrElse { it.message ?: "Failed" } } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Text("💾", fontSize=12.sp); Text("Save Profile", fontSize=14.sp, fontWeight=FontWeight.SemiBold) }
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("◷", fontSize = 14.sp, color=BeBetterTokens.Accent); Text("DISPLAY SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Time Format", fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("Choose how times are displayed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("12h (AM/PM)" to "12h", "24h" to "24h").forEach { (label, v) ->
                        val sel = timeFormat == v
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant).clickable{ timeFormat = v }.padding(horizontal=12.dp), contentAlignment=Alignment.Center) { Text(label, fontSize=12.sp, color=if(sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) { Text("🔔", fontSize=14.sp, color=BeBetterTokens.Accent); Text("NOTIFICATION SETTINGS", fontSize=11.sp, fontWeight=FontWeight.SemiBold, letterSpacing=0.8.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)) }
                TextButton(onClick = {}) { Text("View all", fontSize=12.sp, color=BeBetterTokens.Accent) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("All notifications","One switch for everything below", notifPrefs?.let { it.morningEnabled && it.eveningEnabled && it.habitRemindersEnabled } ?: false),
                    Triple("Morning reminder","Get notified to start your day", notifPrefs?.morningEnabled ?: false),
                    Triple("Evening summary","Review your day before bed", notifPrefs?.eveningEnabled ?: false),
                    Triple("Habit reminders","Reminders at your habit times", notifPrefs?.habitRemindersEnabled ?: false),
                    Triple("Push notifications","Receive push on your device", true),
                    Triple("Announcements","Product updates from the BeBetter team", notifPrefs?.announcementsEnabled ?: true)
                ).forEach { (t,d,c) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(t, fontSize=14.sp, fontWeight=FontWeight.Medium); Text(d, fontSize=11.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)) }
                        Switch(checked = c, onCheckedChange = {})
                    }
                    if (t != "Announcements") HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)
                }
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) { Text("✨", fontSize=14.sp, color=BeBetterTokens.Accent); Text("AI ASSISTANT", fontSize=11.sp, fontWeight=FontWeight.SemiBold, letterSpacing=0.8.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)); Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal=8.dp, vertical=2.dp)) { Text("beta", fontSize=10.sp, color=MaterialTheme.colorScheme.onSurfaceVariant) } }
                Text("Open chat", fontSize=12.sp, color=BeBetterTokens.Accent, modifier=Modifier.clickable{})
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Column { Text("Enable assistant", fontSize=14.sp, fontWeight=FontWeight.Medium); Text("Let the AI read and manage your stuff", fontSize=11.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)) }
                Switch(checked = assistantSettings?.enabled ?: true, onCheckedChange = { scope.launch { assistantSettings = runCatching { ApiClient.get().updateAssistantSettings(mapOf("enabled" to it)) }.getOrNull() } })
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text("🏖", fontSize=14.sp, color=Color(0xFFFBBF24)); Text("VACATION", fontSize=11.sp, fontWeight=FontWeight.SemiBold, letterSpacing=0.8.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)) }
            Text("Going on vacation? Pause all habits so they don't count as missed.", fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
            Text("Reason (optional)", fontSize=12.sp, fontWeight=FontWeight.Medium)
            OutlinedTextField(vacReason, { vacReason = it }, placeholder = { Text("michi", fontSize=13.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Text("End date (optional)", fontSize=12.sp, fontWeight=FontWeight.Medium)
            OutlinedTextField(vacEnd, { vacEnd = it }, placeholder = { Text("mm / dd / yyyy", fontSize=13.sp) }, trailingIcon = { Text("📅", fontSize=14.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(onClick = { scope.launch { runCatching { ApiClient.get().vacationStart(mapOf("reason" to vacReason, "endDate" to vacEnd)); vacation=true } } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)) {
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) { Text("🏖", fontSize=12.sp); Text("Start Vacation", fontSize=14.sp, fontWeight=FontWeight.SemiBold) }
            }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text("🔑", fontSize=14.sp, color=BeBetterTokens.Accent); Text("SECURITY", fontSize=11.sp, fontWeight=FontWeight.SemiBold, letterSpacing=0.8.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)) }
            Text("Current password", fontSize=12.sp, fontWeight=FontWeight.Medium)
            OutlinedTextField(curPw, { curPw = it }, placeholder = { Text("••••••••", fontSize=13.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Text("New password", fontSize=12.sp, fontWeight=FontWeight.Medium)
            OutlinedTextField(newPw, { newPw = it }, placeholder = { Text("New password (min 6 chars)", fontSize=13.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Text("Confirm new password", fontSize=12.sp, fontWeight=FontWeight.Medium)
            OutlinedTextField(confirmPw, { confirmPw = it }, placeholder = { Text("Confirm new password", fontSize=13.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn.copy(alpha = 0.7f), contentColor = Color.White)) { Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) { Text("🔑", fontSize=12.sp); Text("Change Password", fontSize=14.sp) } }
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text("⚠", fontSize=14.sp, color=Color(0xFFF87171)); Text("DANGER ZONE", fontSize=11.sp, fontWeight=FontWeight.SemiBold, letterSpacing=0.8.sp, color=Color(0xFFF87171)) }
            Text("This action is irreversible. All your data will be permanently deleted.", fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)) { Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) { Text("🗑", fontSize=12.sp); Text("Delete Account", fontSize=14.sp, fontWeight=FontWeight.SemiBold) } }
        }
        if (me?.role == "admin") {
            Button(onClick = onAdmin, colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White), modifier = Modifier.fillMaxWidth()) { Text("Open admin panel") }
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
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)) { Text("Go") }
            }
            if (usersRaw.isNotBlank()) Text(usersRaw.take(2000), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Announcements")
            OutlinedTextField(announce, { announce = it }, placeholder = { Text("Broadcast message…") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            Button(onClick = {
                scope.launch {
                    msg = runCatching {
                        ApiClient.get().let { api -> api.adminStats() }
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
                colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)) { Text("Save server") }
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
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = Color.White)) { Text("Save notifications") }
            }
        }
        Text("BeBetter for Android 1.0.0 • Phone + Wear OS • API-compatible with bebetter.websters.at", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Log out") }
    }
}
