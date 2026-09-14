package at.websters.bebetter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.data.Habit
import at.websters.bebetter.ui.components.*
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.SectionTitle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DashboardScreen(
    onHabit: (String) -> Unit,
    onSeeHabits: () -> Unit,
    onSeeTasks: () -> Unit,
    onChallenge: (String) -> Unit,
    onOpen: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<at.websters.bebetter.data.StatsOverview?>(null) }
    var scheduled by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<at.websters.bebetter.data.Task>>(emptyList()) }
    var grid by remember { mutableStateOf<Map<String, at.websters.bebetter.data.GridDay>>(emptyMap()) }
    var years by remember { mutableStateOf<List<Int>>(listOf(LocalDate.now().year)) }
    var year by remember { mutableStateOf(LocalDate.now().year) }
    var me by remember { mutableStateOf<at.websters.bebetter.data.User?>(null) }
    var vacation by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var quickTask by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf("task") }
    var clockTick by remember { mutableStateOf(0) }

    // reactive clock like web (30s) for Overdue/Now/Upcoming buckets
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            clockTick++
        }
    }

    fun load() {
        scope.launch {
            loading = true
            try {
                val api = ApiClient.get()
                me = api.me().user
                val ov = api.statsOverview()
                stats = ov
                vacation = ov.isOnVacation
                scheduled = api.scheduledHabits(LocalDate.now().toString()).habits
                tasks = api.tasks(LocalDate.now().toString()).tasks.filter { !it.isCompletedToday && it.isDueToday }
                years = api.gridYears().years.ifEmpty { listOf(LocalDate.now().year) }.sorted()
                grid = api.grid("$year-01-01", "$year-12-31").grid
            } catch (_: Exception) {}
            loading = false
        }
    }
    LaunchedEffect(year) {
        scope.launch {
            try { grid = ApiClient.get().grid("$year-01-01", "$year-12-31").grid } catch (_: Exception) {}
        }
    }
    LaunchedEffect(Unit) { load() }

    fun nowMinutes(): Int {
        val n = LocalTime.now()
        return n.hour * 60 + n.minute
    }
    fun slotMinutes(t: String?): Int {
        if (t.isNullOrBlank()) return -1
        return try {
            val (h, m) = t.split(":").map { it.toInt() }
            h * 60 + m
        } catch (_: Exception) { -1 }
    }

    val now = nowMinutes()
    // expand scheduled like web (per-slot entries already come expanded from backend)
    val overdue = scheduled.filter { h -> val t = h.scheduledTime; t != null && slotMinutes(t) < now - 30 && h.completedToday != true }
    val nowList = scheduled.filter { h -> val t = h.scheduledTime; t != null && slotMinutes(t) in (now - 30)..(now + 30) && h.completedToday != true }
    val upcoming = scheduled.filter { h -> val t = h.scheduledTime; t != null && slotMinutes(t) > now + 30 && h.completedToday != true }
    val unscheduled = scheduled.filter { it.scheduledTime == null }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { createMode = "task"; showCreate = true },
                containerColor = BeBetterTokens.AccentBtn,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(24.dp)) }
        }
    ) { pad ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BeBetterTokens.Accent)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Demo banner (web-exact)
            if (me?.isDemo == true) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BeBetterTokens.Accent.copy(alpha = 0.10f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BeBetterTokens.Accent.copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("You're in the demo account", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BeBetterTokens.Accent)
                                Text("Shared public account — data resets hourly. Sign up to save your own streaks.", fontSize = 12.sp, color = BeBetterTokens.Accent.copy(alpha = 0.8f))
                            }
                            TextButton(onClick = { onOpen("profile") }) { Text("Sign Up", color = BeBetterTokens.Accent, fontSize = 12.sp) }
                        }
                    }
                }
            }
            // Vacation banner
            if (vacation) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.10f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("🏖️ You're on vacation", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.ui.graphics.Color(0xFFFBBF24))
                                Text("No habits scheduled. Enjoy your break!", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color(0xFFFBBF24).copy(alpha = 0.7f))
                            }
                            TextButton(onClick = { scope.launch { runCatching { ApiClient.get().vacationEnd() }; load() } }) {
                                Text("End early", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color(0xFFFBBF24))
                            }
                        }
                    }
                }
            }
            // Stats row (web: grid-cols-2, card text-center py-3, text-xl bold emerald, label 10px gray)
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("${stats?.activeStreak ?: 0}", "Streak", Modifier.weight(1f))
                    StatCard("${stats?.consistency ?: 0}%", "Consistency", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("${stats?.totalLogs ?: 0}", "Total Done", Modifier.weight(1f))
                    StatCard("${stats?.todayLogs ?: 0}", "Today", Modifier.weight(1f))
                }
            }
            // Year in Review card with year nav
            item {
                BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Year in Review")
                        if (years.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (year > years.first()) year-- }, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.Filled.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("$year", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { if (year < years.last()) year++ }, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Text("$year", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ContributionGridView(grid = grid, year = year)
                }
            }
            // Quick create (tidied: always visible on Android, single row)
            item {
                BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Quick Create")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            quickTask, { quickTask = it }, placeholder = { Text("Add a quick task…", fontSize = 14.sp) },
                            singleLine = true, modifier = Modifier.weight(1f).heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        ApiClient.get().createTask(mapOf("title" to quickTask.trim()))
                                        quickTask = ""
                                        load()
                                    } catch (_: Exception) {}
                                }
                            },
                            enabled = quickTask.isNotBlank(),
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                        ) { Text("Add") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { createMode = "task"; showCreate = true }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("+ New Task", fontSize = 14.sp)
                        }
                        Button(onClick = { createMode = "habit"; showCreate = true }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) {
                            Text("◎ New Habit", fontSize = 14.sp)
                        }
                    }
                }
            }
            // Today's Tasks
            item { SectionTitle("Today's Tasks") }
            if (tasks.isEmpty()) {
                item { Text("No tasks for today", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
            } else {
                items(tasks.take(8)) { t -> TaskRow(task = t, onChanged = { load() }) }
            }
            // Overdue / Now / Upcoming / Today's Habits (web buckets)
            if (overdue.isNotEmpty()) {
                item { Text("OVERDUE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)) }
                items(overdue, key = { "o-${it.id}-${it.scheduledTime}" }) { h -> HabitRow(h, { onHabit(h.id) }, { load() }) }
            }
            if (nowList.isNotEmpty()) {
                item { SectionTitle("Now") }
                items(nowList, key = { "n-${it.id}-${it.scheduledTime}" }) { h -> HabitRow(h, { onHabit(h.id) }, { load() }) }
            }
            if (upcoming.isNotEmpty()) {
                item { SectionTitle("Upcoming") }
                items(upcoming, key = { "u-${it.id}-${it.scheduledTime}" }) { h -> HabitRow(h, { onHabit(h.id) }, { load() }) }
            }
            if (unscheduled.isNotEmpty()) {
                item { SectionTitle("Today's Habits") }
                items(unscheduled, key = { "t-${it.id}" }) { h -> HabitRow(h, { onHabit(h.id) }, { load() }) }
            }
            if (tasks.isEmpty() && scheduled.isEmpty()) {
                item {
                    BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("◎", fontSize = 28.sp, color = BeBetterTokens.Accent)
                            Text("Welcome to BeBetter", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Create your first habit or task to start tracking your streaks.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if (showCreate) {
        CreateSheet(initialMode = createMode, onDismiss = { showCreate = false }, onCreated = { showCreate = false; load() })
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BeBetterTokens.Accent)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}
