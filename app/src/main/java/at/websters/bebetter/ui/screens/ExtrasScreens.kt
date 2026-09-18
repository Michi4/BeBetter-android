package at.websters.bebetter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.components.BeBetterCard
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(onDetail: (String) -> Unit, onCreate: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var presets by remember { mutableStateOf<List<at.websters.bebetter.data.Preset>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { scope.launch { presets = runCatching { ApiClient.get().presets().presets }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Presets")
            Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("+ New") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(q, { q = it }, placeholder = { Text("Search presets…", fontSize = 14.sp) }, modifier = Modifier.weight(1f).heightIn(min = 44.dp), singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
            Button(onClick = { scope.launch { presets = runCatching { ApiClient.get().presets(q.ifBlank { null }, null).presets }.getOrDefault(emptyList()) } },
                modifier = Modifier.height(44.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Go") }
        }
        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
            items(presets) { p ->
                BeBetterCard(modifier = Modifier.fillMaxWidth(), onClick = { onDetail(p.id) }) {
                    Text("${p.emoji.orEmpty()} ${p.title}".trim(), fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    p.description?.let { Text(it.take(140), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("${p.likesCount} likes • ${p.forksCount} forks • ${p.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun PresetDetailScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var p by remember { mutableStateOf<at.websters.bebetter.data.Preset?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { scope.launch { p = runCatching { ApiClient.get().presetDetail(id).preset }.getOrNull() } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("← Back", color = BeBetterTokens.Accent) }
        val preset = p ?: run { LinearProgressIndicator(Modifier.fillMaxWidth(), color = BeBetterTokens.Accent); return@Column }
        BeBetterCard(modifier = Modifier.fillMaxWidth()) {
            Text("${preset.emoji.orEmpty()} ${preset.title}".trim(), fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            preset.description?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("${preset.category} • ${preset.likesCount} likes • ${preset.forksCount} forks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { msg = runCatching { ApiClient.get().usePreset(id); "Added to your habits!" }.getOrElse { it.message ?: "Failed" } } },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { Text("Use") }
            OutlinedButton(onClick = { scope.launch { msg = runCatching { ApiClient.get().likePreset(id); "Liked!" }.getOrElse { it.message ?: "Failed" } } }, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { Text("♥ Like") }
            OutlinedButton(onClick = { scope.launch { msg = runCatching { ApiClient.get().forkPreset(id); "Forked!" }.getOrElse { it.message ?: "Failed" } } }, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { Text("Fork") }
        }
        var showReport by remember { mutableStateOf(false) }
        var reportReason by remember { mutableStateOf("") }
        OutlinedButton(onClick = { showReport = !showReport }, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { Text("Report") }
        if (showReport) {
            OutlinedTextField(reportReason, { reportReason = it }, placeholder = { Text("Why is this inappropriate?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            Button(onClick = { scope.launch {
                msg = runCatching {
                    ApiClient.get().reportPreset(id, mapOf("reason" to reportReason.trim()))
                    showReport = false
                    "Thanks — reported for review."
                }.getOrElse { it.message ?: "Failed" }
            } }, enabled = reportReason.isNotBlank()) { Text("Send report") }
        }
        msg?.let { Text(it, fontSize = 14.sp, color = BeBetterTokens.Accent) }
    }
}

@Composable
fun PresetCreateScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("New preset", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
        OutlinedTextField(desc, { desc = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
        Text("Category", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Fitness", "Health", "Learning", "Productivity", "Mindfulness", "Social", "Other").forEach { c ->
                val sel = category == c
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { category = c }.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(c, fontSize = 12.sp, color = if (sel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Button(onClick = {
            busy = true; err = null
            scope.launch {
                val r = runCatching { ApiClient.get().createPreset(mapOf("title" to title.trim(), "description" to desc.trim().ifBlank { null }, "category" to category)) }
                busy = false
                if (r.isSuccess) onDone() else err = "Failed to create preset"
            }
        }, enabled = !busy && title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Saving…" else "Create preset") }
    }
}

@Composable
fun LeaderboardScreen() {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<at.websters.bebetter.data.LeaderboardEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        scope.launch {
            val r = runCatching { ApiClient.get().globalLeaderboard() }.getOrNull()
            entries = r?.leaderboard?.ifEmpty { r.entries } ?: emptyList()
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Leaderboard")
        if (entries.isEmpty()) {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) { Text("No rankings yet — complete habits to climb!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            entries.forEachIndexed { i, e ->
                BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("#${i + 1} @${e.username}", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text("🔥 ${e.streak} • ✅ ${e.completions}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen() {
    val scope = rememberCoroutineScope()
    var notifs by remember { mutableStateOf<List<at.websters.bebetter.data.AppNotification>>(emptyList()) }
    fun load() {
        scope.launch {
            notifs = runCatching { ApiClient.get().notifications().notifications }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            SectionTitle("Notifications")
            TextButton(onClick = { scope.launch { runCatching { ApiClient.get().markRead() }; load() } }) { Text("Mark all read", color = BeBetterTokens.Accent, fontSize = 12.sp) }
        }
        if (notifs.isEmpty()) {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) { Text("All caught up! 🎉", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            notifs.forEach { n ->
                BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(n.message, fontSize = 14.sp, fontWeight = if (!n.read && !n.pushed) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal)
                        Text("${n.type} • ${n.createdAt.take(16).replace("T", " ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
