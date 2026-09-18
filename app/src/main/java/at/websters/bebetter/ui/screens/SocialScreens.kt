package at.websters.bebetter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.ui.components.BeBetterCard
import at.websters.bebetter.data.ApiClient
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(onChallenge: (String) -> Unit, onNewChallenge: () -> Unit, onFriendProfile: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<at.websters.bebetter.data.Friend>>(emptyList()) }
    var requests by remember { mutableStateOf<List<at.websters.bebetter.data.FriendRequestItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<at.websters.bebetter.data.Friend>>(emptyList()) }
    var inviteToken by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            try {
                friends = runCatching { ApiClient.get().friendList().friends }.getOrElse { ApiClient.get().friendsAlt().friends }
                val r = ApiClient.get().friendRequests()
                requests = (r.requests.ifEmpty { r.incoming })
            } catch (_: Exception) {}
        }
    }
    LaunchedEffect(Unit) { load() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Friends", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search users") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = { scope.launch { try { val res = ApiClient.get().friendSearch(query); results = res.users.ifEmpty { res.results } } catch (_: Exception) {} } }) { Text("Go") }
            }
            results.forEach { u ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("@${u.username}")
                    TextButton(onClick = { scope.launch { runCatching { ApiClient.get().friendRequest(mapOf("userId" to u.id)) } } }) { Text("Add") }
                }
            }
            if (requests.isNotEmpty()) {
                Text("Requests (${requests.size})", style = MaterialTheme.typography.titleSmall)
            }
        }
        items(requests) { req ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(req.requester?.username ?: req.id.take(8))
                    Row {
                        TextButton(onClick = { scope.launch { runCatching { ApiClient.get().acceptRequest(req.id) }; load() } }) { Text("Accept") }
                        TextButton(onClick = { scope.launch { runCatching { ApiClient.get().declineRequest(req.id) }; load() } }) { Text("Decline") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch {
                    inviteToken = runCatching {
                        val r = ApiClient.get().createFriendLink()
                        r["token"] ?: r["link"]
                    }.getOrNull()
                } }) { Text("Invite link") }
                Button(onClick = onNewChallenge) { Text("New battle") }
            }
            inviteToken?.let { Text("Share: https://app.bebetter.websters.at/friend/accept/$it", style = MaterialTheme.typography.bodySmall) }
            Text("Your friends (${friends.size})", style = MaterialTheme.typography.titleSmall)
        }
        items(friends) { f ->
            Card(Modifier.fillMaxWidth().clickable { onFriendProfile(f.id) }) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("@${f.username}", style = MaterialTheme.typography.titleSmall); f.bio?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
                    TextButton(onClick = { scope.launch { runCatching { ApiClient.get().removeFriend(f.id) }; load() } }) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
fun ChallengesScreen(onDetail: (String) -> Unit, onNew: () -> Unit) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<at.websters.bebetter.data.Challenge>>(emptyList()) }
    LaunchedEffect(Unit) { scope.launch { list = runCatching { ApiClient.get().challenges().challenges }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Battles", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onNew) { Text("New") }
        }
        list.forEach { c ->
            Card(onClick = { onDetail(c.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${c.title.ifBlank { c.habit?.title ?: "Challenge" }}", style = MaterialTheme.typography.titleSmall)
                    Text("${c.creator?.username} vs ${c.opponent?.username} • ${c.status}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (list.isEmpty()) Text("No battles yet.")
    }
}

@Composable
fun NewChallengeScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var habits by remember { mutableStateOf<List<at.websters.bebetter.data.Habit>>(emptyList()) }
    var friends by remember { mutableStateOf<List<at.websters.bebetter.data.Friend>>(emptyList()) }
    var habitId by remember { mutableStateOf("") }
    var opponentId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        scope.launch {
            habits = runCatching { ApiClient.get().habits().habits }.getOrDefault(emptyList())
            friends = runCatching { ApiClient.get().friendList().friends }.getOrDefault(emptyList())
            habitId = habits.firstOrNull()?.id ?: ""
            opponentId = friends.firstOrNull()?.id ?: ""
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("New battle", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, { title = it }, label = { Text("Title (optional)") }, modifier = Modifier.fillMaxWidth())
        Text("Habit: ${habits.firstOrNull { it.id == habitId }?.title ?: habitId}")
        habits.take(20).forEach { h -> FilterChip(selected = h.id == habitId, onClick = { habitId = h.id }, label = { Text("${h.emoji} ${h.title}".take(24)) }) }
        Text("Opponent: ${friends.firstOrNull { it.id == opponentId }?.username ?: opponentId}")
        friends.take(20).forEach { f -> FilterChip(selected = f.id == opponentId, onClick = { opponentId = f.id }, label = { Text("@${f.username}") }) }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                try {
                    ApiClient.get().createChallenge(mapOf("habitId" to habitId, "opponentId" to opponentId, "title" to title))
                    onDone()
                } catch (e: Exception) { err = e.message }
            }
        }, enabled = habitId.isNotBlank() && opponentId.isNotBlank()) { Text("Challenge!") }
    }
}

@Composable
fun ChallengeDetailScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var c by remember { mutableStateOf<at.websters.bebetter.data.Challenge?>(null) }
    LaunchedEffect(id) { scope.launch { c = runCatching { ApiClient.get().challengeDetail(id).challenge }.getOrNull() } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        val ch = c ?: run { LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column }
        Text("${ch.title.ifBlank { ch.habit?.title ?: "Battle" }}", style = MaterialTheme.typography.headlineSmall)
        Text("${ch.creator?.username} vs ${ch.opponent?.username} • ${ch.status}")
        ch.stake?.let { Text("Stake: $it") }
        var actionMsg by remember { mutableStateOf<String?>(null) }
        if (ch.status == "pending") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch {
                    val r = runCatching { ApiClient.get().acceptChallenge(id) }
                    actionMsg = if (r.isSuccess) "Accepted — good luck!" else "Could not accept (maybe your own challenge)."
                    if (r.isSuccess) { c = runCatching { ApiClient.get().challengeDetail(id).challenge }.getOrNull() }
                } }) { Text("Accept") }
                OutlinedButton(onClick = { scope.launch { runCatching { ApiClient.get().declineChallenge(id) }; onBack() } }) { Text("Decline") }
            }
        }
        if (ch.status == "active") {
            Text("Declare winner", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch {
                    val r = runCatching { ApiClient.get().resolveChallenge(ch.id, mapOf("winnerId" to ch.creatorId)) }
                    actionMsg = if (r.isSuccess) "Winner declared!" else "Could not resolve."
                    if (r.isSuccess) { c = runCatching { ApiClient.get().challengeDetail(id).challenge }.getOrNull() }
                } }) { Text(ch.creator?.username?.takeIf { it.isNotBlank() }?.let { "$it won" } ?: "Creator won") }
                OutlinedButton(onClick = { scope.launch {
                    val r = runCatching { ApiClient.get().resolveChallenge(ch.id, mapOf("winnerId" to ch.opponentId)) }
                    actionMsg = if (r.isSuccess) "Winner declared!" else "Could not resolve."
                    if (r.isSuccess) { c = runCatching { ApiClient.get().challengeDetail(id).challenge }.getOrNull() }
                } }) { Text(ch.opponent?.username?.takeIf { it.isNotBlank() }?.let { "$it won" } ?: "Opponent won") }
            }
        }
        if (ch.status != "pending" && ch.status != "active") {
            Text("Status: ${ch.status}" + (ch.winnerId?.let { " • winner decided" } ?: ""), style = MaterialTheme.typography.bodyMedium)
        }
        actionMsg?.let { Text(it, color = BeBetterTokens.Accent, fontSize = 13.sp) }
    }
}

@Composable
fun FriendProfileScreen(userId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<at.websters.bebetter.data.FriendProfileResponse?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun load() {
        scope.launch {
            profile = runCatching { ApiClient.get().friendProfile(userId) }.getOrElse {
                err = "Could not load profile"; null
            }
        }
    }
    LaunchedEffect(userId) { load() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Back", color = BeBetterTokens.Accent) }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        val p = profile ?: run { LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column }
        val u = p.user
        if (u == null) {
            Text("User not found.", fontSize = 14.sp)
            return@Column
        }
        Text("@" + u.username, style = MaterialTheme.typography.headlineSmall)
        u.bio?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (!p.isFriend) {
            Button(onClick = {
                busy = true
                scope.launch {
                    val r = runCatching { ApiClient.get().friendRequest(mapOf("userId" to u.id)) }
                    busy = false
                    if (r.isSuccess) load()
                }
            }, enabled = !busy) { Text("Add friend") }
        } else {
            Text("You are friends", fontSize = 12.sp, color = BeBetterTokens.Accent)
        }
        Text("Habits (${p.habits?.size ?: 0})", style = MaterialTheme.typography.titleSmall)
        (p.habits ?: emptyList()).forEach { h ->
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                Text(h.title, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
        }
    }
}
