package at.websters.bebetter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.websters.bebetter.data.ApiClient
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(onDetail: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var presets by remember { mutableStateOf<List<at.websters.bebetter.data.Preset>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { scope.launch { presets = runCatching { ApiClient.get().presets().presets }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Presets", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(q, { q = it }, label = { Text("Search") }, modifier = Modifier.weight(1f), singleLine = true)
            Button(onClick = { scope.launch { presets = runCatching { ApiClient.get().presets(q.ifBlank { null }, null).presets }.getOrDefault(emptyList()) } }) { Text("Go") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(presets) { p ->
                Card(onClick = { onDetail(p.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${p.emoji ?: "✨"} ${p.title}", style = MaterialTheme.typography.titleSmall)
                        p.description?.let { Text(it.take(120), style = MaterialTheme.typography.bodySmall) }
                        Text("❤ ${p.likesCount} • 🍴 ${p.forksCount} • ${p.category}", style = MaterialTheme.typography.labelSmall)
                    }
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
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        val preset = p ?: run { LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column }
        Text("${preset.emoji ?: "✨"} ${preset.title}", style = MaterialTheme.typography.headlineSmall)
        preset.description?.let { Text(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { msg = runCatching { ApiClient.get().usePreset(id); "Added to your habits!" }.getOrElse { it.message ?: "Failed" } } }) { Text("Use") }
            OutlinedButton(onClick = { scope.launch { msg = runCatching { ApiClient.get().likePreset(id); "Liked!" }.getOrElse { it.message ?: "Failed" } } }) { Text("♥ Like") }
            OutlinedButton(onClick = { scope.launch { msg = runCatching { ApiClient.get().forkPreset(id); "Forked!" }.getOrElse { it.message ?: "Failed" } } }) { Text("Fork") }
        }
        msg?.let { Text(it) }
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
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineSmall)
        entries.forEachIndexed { i, e ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${i + 1} @${e.username}")
                    Text("🔥 ${e.streak} • ✅ ${e.completions}")
                }
            }
        }
        if (entries.isEmpty()) Text("No rankings yet — complete habits to climb!")
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
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Notifications", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { scope.launch { runCatching { ApiClient.get().markRead() }; load() } }) { Text("Mark all read") }
        }
        notifs.forEach { n ->
            Card(Modifier.fillMaxWidth(), colors = if (!n.read) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()) {
                Column(Modifier.padding(12.dp)) {
                    Text(n.message, style = MaterialTheme.typography.bodyMedium)
                    Text("${n.type} • ${n.createdAt.take(16).replace("T", " ")}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (notifs.isEmpty()) Text("All caught up! 🎉")
    }
}
