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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
            // left circular check button: empty ring when unchecked (web: border-2 rounded-full)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (done) BeBetterTokens.Accent else Color.Transparent)
                    .border(
                        2.dp,
                        if (done) BeBetterTokens.Accent else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                    .clickable(enabled = !busy && !done) {
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
                contentAlignment = Alignment.Center
            ) {
                when {
                    done -> Icon(Icons.Filled.Check, "Completed", tint = Color.White, modifier = Modifier.size(18.dp))
                    needsCam -> Icon(Icons.Filled.CameraAlt, "Complete with photo", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    // unchecked: intentionally empty
                }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // mobile move buttons (web TaskCard: ChevronUp/ChevronDown)
        if (onMove != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onMove(1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Move down", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
                    textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
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

// ContributionGridView — exact replica of ContributionGrid.vue
// Monday-start week columns, cell 10dp / gap 3dp / day-label gutter 24dp,
// absolute month labels at week-of-month, today ring, Less/More legend,
// auto-scroll so TODAY sits centered (web scrollToToday math).

@Composable
fun ContributionGridView(
    grid: Map<String, GridDay>,
    year: Int,
    dark: Boolean = isBeBetterDark(),
    vacationDays: Set<String> = emptySet(),
    onDayClick: ((String) -> Unit)? = null
) {
    val scroll = androidx.compose.foundation.rememberScrollState()
    val cellDp = 10
    val gapDp = 3
    val gutterDp = 24

    // ---- weeks: column-major, Monday first, exactly like web ----
    data class Cell(val date: String?, val day: GridDay?)
    val weeks: List<List<Cell>> = remember(grid, year) {
        val jan1 = java.time.LocalDate.of(year, 1, 1)
        val dec31 = java.time.LocalDate.of(year, 12, 31)
        val startDow = jan1.dayOfWeek.value - 1 // Mon=0
        val first = jan1.minusDays(startDow.toLong())
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(first, dec31).toInt() + 1
        val nWeeks = (totalDays + 6) / 7
        (0 until nWeeks).map { w ->
            (0 until 7).map { d ->
                val date = first.plusDays((w * 7 + d).toLong())
                if (date.year != year) Cell(null, null)
                else {
                    val ds = date.toString()
                    Cell(ds, grid[ds])
                }
            }
        }
    }

    // ---- month labels: floor((firstOfMonth - week0)/7) like web ----
    val monthLabels = remember(year, weeks.size) {
        val jan1 = java.time.LocalDate.of(year, 1, 1)
        val startDow = jan1.dayOfWeek.value - 1
        val week0 = jan1.minusDays(startDow.toLong())
        (0 until 12).mapNotNull { m ->
            val firstOfMonth = java.time.LocalDate.of(year, m + 1, 1)
            val wi = java.time.temporal.ChronoUnit.DAYS.between(week0, firstOfMonth).toInt() / 7
            if (wi < 0 || wi >= weeks.size) return@mapNotNull null
            val label = firstOfMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
            wi to label
        }
    }

    val totalWidthDp = gutterDp + gapDp + weeks.size * (cellDp + gapDp)

    // ---- auto-scroll to today centered ----
    val todayStr = LocalDate.now().toString()
    LaunchedEffect(year, grid) {
        val wi = weeks.indexOfFirst { w -> w.any { it.date == todayStr } }
        if (wi >= 0) {
            val x = (gutterDp + gapDp + wi * (cellDp + gapDp)).toFloat()
            val max = scroll.maxValue
            if (max > 0) {
                val target = (x - 170f).toInt().coerceIn(0, max)
                scroll.scrollTo(target)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .horizontalScroll(scroll)
                .width(totalWidthDp.dp)
        ) {
            // month labels row (absolute offsets)
            Box(Modifier.fillMaxWidth().height(18.dp)) {
                monthLabels.forEach { (wi, label) ->
                    Text(
                        label, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.offset(x = (gutterDp + gapDp + wi * (cellDp + gapDp)).dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp.dp)) {
                // day labels column: Mon..Sun, only Wed & Sat shown
                Column(
                    Modifier.width(gutterDp.dp),
                    verticalArrangement = Arrangement.spacedBy(gapDp.dp)
                ) {
                    val labels = listOf("Mon", "", "Wed", "", "", "Sat", "")
                    labels.forEach { lab ->
                        Box(Modifier.height(cellDp.dp), contentAlignment = Alignment.CenterStart) {
                            Text(lab, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                        }
                    }
                }
                // weeks
                Row(horizontalArrangement = Arrangement.spacedBy(gapDp.dp)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(gapDp.dp)) {
                            week.forEach { cell ->
                                val ds = cell.date
                                Box(
                                    Modifier
                                        .size(cellDp.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when {
                                                ds == null -> Color.Transparent
                                                else -> webCellColor(cell.day, ds in vacationDays)
                                            }
                                        )
                                        .then(
                                            if (ds == todayStr)
                                                Modifier.border(2.dp, Color(0x9934D399), RoundedCornerShape(2.dp))
                                            else Modifier
                                        )
                                        .then(
                                            if (ds != null && cell.day != null &&
                                                (cell.day.scheduled > 0 || cell.day.completed > 0 || cell.day.tasks > 0) &&
                                                onDayClick != null
                                            ) Modifier.clickable { onDayClick(ds) } else Modifier
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
        // legend (right aligned) — 6 swatches gray/800-40,800-80,e950,e700,e500,e400
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            listOf(
                Color(0x661F2937), Color(0xCC1F2937), Color(0xFF022C22),
                Color(0xFF047857), Color(0xFF10B981), Color(0xFF34D399)
            ).forEach {
                Box(Modifier.size(cellDp.dp).clip(RoundedCornerShape(2.dp)).background(it))
            }
            Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// exact getCellClass() from ContributionGrid.vue
@Composable
private fun webCellColor(day: GridDay?, isVacation: Boolean): Color {
    if (isVacation) return Color(0x2EF59E0B)
    if (day == null) return Color(0x661F2937) // gray-800/40 no activity
    val habits = day.habits
    val scheduled = day.scheduled
    val tasks = day.tasks
    if (scheduled == 0 && habits == 0 && tasks == 0 && day.completed == 0) return Color(0x661F2937)
    if (tasks > 0 && scheduled == 0 && habits == 0) return Color(0x99022C22) // emerald-950/60
    val ratio = if (scheduled > 0) habits.toFloat() / scheduled else if (habits > 0) 1f else null
    if (ratio == null || ratio == 0f) return Color(0xCC1F2937) // gray-800/80
    return when {
        ratio <= 0.33f -> Color(0xFF022C22)
        ratio <= 0.66f -> Color(0xFF047857)
        ratio < 1f -> Color(0xFF10B981)
        else -> Color(0xFF34D399)
    }
}
