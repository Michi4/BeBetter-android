package at.websters.bebetter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.*
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.isBeBetterDark
import at.websters.bebetter.ui.theme.levelColor
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------- Web-exact primitives ----------

@Composable
fun BeBetterCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun WebChip(text: String, kind: String = "emerald") {
    val (bg, fg) = when (kind) {
        "amber" -> Color(0xFFF59E0B).copy(alpha = 0.10f) to Color(0xFFFBBF24)
        "red" -> Color(0xFFEF4444).copy(alpha = 0.10f) to Color(0xFFF87171)
        "gray" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> BeBetterTokens.Accent.copy(alpha = 0.10f) to BeBetterTokens.Accent
    }
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1)
    }
}

fun formatTimeWeb(t: String?): String {
    if (t.isNullOrBlank()) return ""
    return try {
        val (h, m) = t.split(":").map { it.toInt() }
        val ampm = if (h >= 12) "PM" else "AM"
        val h12 = when (h % 12) { 0 -> 12 else -> h % 12 }
        "$h12:${m.toString().padStart(2, '0')} $ampm"
    } catch (_: Exception) { t }
}

// ---------- HabitCard: exact web replica ----------

@Composable
fun HabitRow(habit: Habit, onOpen: () -> Unit, onToggled: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val done = habit.completedToday == true
    val needsCam = habit.verificationType == "be_better_cam" || habit.verificationType == "photo"
    val time = habit.scheduledTime ?: habit.parsedSchedules().firstOrNull { it.time != null }?.time

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // left circular check / camera button (w-11 h-11)
            FilledIconButton(
                onClick = {
                    if (done || busy) return@FilledIconButton
                    busy = true
                    scope.launch {
                        try {
                            val api = ApiClient.get()
                            api.completeHabit(mapOf("habitId" to habit.id, "scheduledTime" to habit.scheduledTime, "status" to "completed"))
                            onToggled()
                        } catch (_: Exception) {}
                        busy = false
                    }
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (done) BeBetterTokens.Accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (done) BeBetterTokens.Accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    when {
                        needsCam && !done -> Icons.Filled.CameraAlt
                        else -> Icons.Filled.CheckCircle
                    },
                    contentDescription = if (done) "Completed" else "Complete",
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(habit.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (!time.isNullOrBlank()) WebChip(formatTimeWeb(time))
                    if (!habit.challengeId.isNullOrBlank()) WebChip("challenge", "amber")
                    if (habit.hasBreak()) WebChip("pause", "amber")
                }
                if (!habit.description.isNullOrBlank()) {
                    Text(habit.description!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Done", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = BeBetterTokens.Accent)
                    IconButton(onClick = {
                        busy = true
                        scope.launch {
                            try {
                                ApiClient.get().undoHabit(habit.id, habit.scheduledTime, LocalDate.now().toString())
                                onToggled()
                            } catch (_: Exception) {}
                            busy = false
                        }
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun Habit.hasBreak(): Boolean = try {
    breaks?.any { it.endDate == null } == true
} catch (_: Exception) { false }

// ---------- TaskCard: exact web replica ----------

@Composable
fun TaskRow(task: Task, onChanged: () -> Unit, onMove: ((Int) -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val done = task.isCompletedToday
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (onMove != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(20.dp)) {
                    Text("∧", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                }
                IconButton(onClick = { onMove(1) }, modifier = Modifier.size(20.dp)) {
                    Text("∨", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                }
            }
        }
        // checkbox 44dp rounded-lg border-2
        Box(
            Modifier.size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    2.dp,
                    if (done) BeBetterTokens.AccentStrong else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
                .background(if (done) BeBetterTokens.AccentStrong else Color.Transparent)
                .clickable(enabled = !busy) {
                    busy = true
                    scope.launch {
                        try {
                            if (!done) ApiClient.get().completeTask(task.id) else ApiClient.get().uncompleteTask(task.id, LocalDate.now().toString())
                            onChanged()
                        } catch (_: Exception) {}
                        busy = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!task.scheduledTime.isNullOrBlank()) WebChip(formatTimeWeb(task.scheduledTime))
                if (!task.dueDate.isNullOrBlank()) {
                    val label = task.dueDate!!.take(10)
                    WebChip(label, kind = if (label < LocalDate.now().toString()) "red" else "gray")
                }
            }
            if (!task.description.isNullOrBlank() && !done) {
                Text(task.description!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ChallengeRow(c: Challenge, onOpen: () -> Unit) {
    BeBetterCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Text("⚔️ ${c.title.ifBlank { c.habit?.title ?: "Challenge" }}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("${c.creator?.username ?: "?"} vs ${c.opponent?.username ?: "?"} • ${c.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        if (onSeeAll != null) TextButton(onClick = onSeeAll) {
            Text("See all", color = BeBetterTokens.Accent, fontSize = 12.sp)
        }
    }
}

@Composable
fun ContributionGridView(
    grid: Map<String, GridDay>,
    year: Int,
    dark: Boolean = isBeBetterDark(),
    vacationDays: Set<String> = emptySet(),
    onDayClick: ((String) -> Unit)? = null
) {
    val scroll = androidx.compose.foundation.rememberScrollState()
    val start = LocalDate.of(year, 1, 1)
    var first = start
    // web: week starts Sunday (0), align Jan 1 to Sunday
    while (first.dayOfWeek.value % 7 != 0) first = first.minusDays(1)
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    // auto-scroll to September/October like screenshot (center ~ today)
    LaunchedEffect(year, grid) {
        // scroll to ~ 2/3 year to mimic web where today is visible
        kotlinx.coroutines.delay(100)
        scroll.animateScrollTo((scroll.maxValue * 0.55).toInt().coerceAtLeast(0))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(scroll)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(start = 24.dp)) {
            // month headers anchored to week start — web does exact math, we approximate with spacer
            months.forEachIndexed { i, label ->
                val monthStart = LocalDate.of(year, i+1, 1)
                val weekIndex = java.time.temporal.ChronoUnit.WEEKS.between(first, monthStart.minusDays((monthStart.dayOfWeek.value % 7).toLong())).toInt()
                val offset = (weekIndex * 15) // 12 + 3
                Box(Modifier.width(30.dp)) { Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)) }
            }
        }
        for (row in 0 until 7) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (row) { 2 -> "Wed"; 5 -> "Sat"; else -> "" },
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.width(20.dp)
                )
                for (w in 0 until 53) {
                    val day = first.plusDays((w * 7 + row).toLong())
                    if (day.year != year && (w > 0 && day.year > year)) {
                        Box(Modifier.size(10.dp))
                    } else if (day.year != year) {
                        Box(Modifier.size(10.dp))
                    } else {
                        val dateStr = day.toString()
                        val g = grid[dateStr]
                        val isVac = dateStr in vacationDays
                        val intensity = when {
                            isVac -> 0.0
                            g == null || (g.completed == 0 && g.scheduled == 0 && g.tasks == 0) -> 0.0
                            g.scheduled == 0 && g.tasks > 0 -> 0.25
                            g.scheduled == 0 -> 1.0
                            else -> (g.completed.toDouble() / g.scheduled.coerceAtLeast(1)).coerceIn(0.0, 1.0).let { if (it == 0.0 && g.completed > 0) 0.5 else it }
                        }
                        val today = day == LocalDate.now()
                        val base = if (isVac) Color(0xFFF59E0B).copy(alpha = 0.18f) else levelColor(intensity, dark)
                        val cellModifier = Modifier.size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(base)
                            .then(if (today) Modifier.border(1.5.dp, BeBetterTokens.Accent, RoundedCornerShape(2.dp)) else Modifier)
                            .clickable { onDayClick?.invoke(dateStr) }
                        Box(cellModifier)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            listOf(0.0, 0.2, 0.4, 0.7, 1.0).forEach {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(levelColor(it, dark)))
            }
            Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
