package at.websters.bebetter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.components.BeBetterCard
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(onDetail: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var presets by remember { mutableStateOf<List<at.websters.bebetter.data.Preset>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { scope.launch { presets = runCatching { ApiClient.get().presets().presets }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionTitle("Presets")
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
        msg?.let { Text(it, fontSize = 14.sp, color = BeBetterTokens.Accent) }
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
