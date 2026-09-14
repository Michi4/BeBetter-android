package at.websters.bebetter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.theme.BeBetterTokens
import kotlinx.coroutines.launch

private val COMMON_EMOJIS = listOf("🌱","🔥","💧","🏃","📚","🧘","💪","🥗","😴","🎯","⭐","✨","💡","🎨","🎵","📝","🧠","❤️","🌙","☀️")
private val DAYS = listOf("Su","Mo","Tu","We","Th","Fr","Sa")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSheet(initialMode: String = "task", onDismiss: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(initialMode) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    // shared
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🌱") }
    var showEmoji by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    // task
    var dueDate by remember { mutableStateOf("") } // YYYY-MM-DD
    var schedTime by remember { mutableStateOf("") } // HH:MM
    var repeat by remember { mutableStateOf("once") }
    var repeatDays by remember { mutableStateOf(setOf(1,2,3,4,5)) }
    var reminders by remember { mutableStateOf(setOf(0)) }

    // habit
    var freq by remember { mutableStateOf("daily") }
    var verification by remember { mutableStateOf("honor") }
    var interval by remember { mutableStateOf("") }
    var habitTimes by remember { mutableStateOf(listOf("")) } // list of HH:MM
    var habitDays by remember { mutableStateOf(setOf(0,1,2,3,4,5,6)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "task", onClick = { mode = "task" }, label = { Text("+ New Task") })
                FilterChip(selected = mode == "habit", onClick = { mode = "habit" }, label = { Text("◎ New Habit") })
            }
            // emoji + title like web (emoji picker button + input)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showEmoji = !showEmoji }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(8.dp)) {
                    Text(emoji, fontSize = 22.sp)
                }
                OutlinedTextField(title, { title = it }, placeholder = { Text(if (mode == "task") "Task title…" else "Habit title…") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
            }
            if (showEmoji) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(COMMON_EMOJIS) { e ->
                        Text(e, fontSize = 24.sp, modifier = Modifier.clickable { emoji = e; showEmoji = false }.padding(6.dp))
                    }
                }
            }
            OutlinedTextField(desc, { desc = it }, placeholder = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), minLines = 1)

            if (mode == "task") {
                OutlinedTextField(dueDate, { dueDate = it }, placeholder = { Text("Due date YYYY-MM-DD (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(schedTime, { schedTime = it }, placeholder = { Text("Time HH:MM (optional, e.g. 09:30)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                Text("Repeat", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("once" to "Once", "daily" to "Daily", "weekly" to "Weekly").forEach { (v, l) ->
                        FilterChip(selected = repeat == v, onClick = { repeat = v }, label = { Text(l, fontSize = 12.sp) })
                    }
                }
                if (repeat == "weekly") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DAYS.forEachIndexed { i, d ->
                            FilterChip(selected = repeatDays.contains(i), onClick = {
                                repeatDays = if (repeatDays.contains(i)) repeatDays - i else repeatDays + i
                            }, label = { Text(d, fontSize = 11.sp) })
                        }
                    }
                }
                Text("Reminders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "At time", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m").forEach { (v, l) ->
                        FilterChip(selected = reminders.contains(v), onClick = {
                            reminders = if (reminders.contains(v)) reminders - v else reminders + v
                        }, label = { Text(l, fontSize = 11.sp) })
                    }
                }
            } else {
                // Habit recurrence like RecurrenceBuilder.vue
                Text("Schedule", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("daily" to "Daily", "weekly" to "Weekly", "interval" to "Every N days").forEach { (v, l) ->
                        FilterChip(selected = freq == v, onClick = { freq = v }, label = { Text(l, fontSize = 11.sp) })
                    }
                }
                if (freq == "weekly" || freq == "daily") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DAYS.forEachIndexed { i, d ->
                            FilterChip(selected = habitDays.contains(i), onClick = {
                                habitDays = if (habitDays.contains(i)) habitDays - i else habitDays + i
                            }, label = { Text(d, fontSize = 11.sp) })
                        }
                    }
                }
                if (freq == "interval") {
                    OutlinedTextField(interval, { interval = it.filter { c -> c.isDigit() }.take(3) }, placeholder = { Text("Every N days (2-365)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                }
                habitTimes.forEachIndexed { idx, t ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(t, { habitTimes = habitTimes.toMutableList().also { l -> l[idx] = it } }, placeholder = { Text("Time HH:MM (optional)") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                        if (habitTimes.size > 1) TextButton(onClick = { habitTimes = habitTimes.filterIndexed { i, _ -> i != idx } }) { Text("✕") }
                    }
                }
                TextButton(onClick = { habitTimes = habitTimes + "" }) { Text("+ Add time") }
                Text("Verification", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("honor" to "Honor", "photo" to "Photo", "be_better_cam" to "BeBetter Cam").forEach { (v, l) ->
                        FilterChip(selected = verification == v, onClick = { verification = v }, label = { Text(l, fontSize = 11.sp) })
                    }
                }
            }

            err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        busy = true; err = null
                        scope.launch {
                            try {
                                if (mode == "task") {
                                    val body = mutableMapOf<String, Any?>(
                                        "title" to title.trim(),
                                        "description" to desc.ifBlank { null },
                                        "emoji" to emoji
                                    )
                                    if (dueDate.isNotBlank()) body["dueDate"] = dueDate.trim()
                                    if (schedTime.isNotBlank()) body["scheduledTime"] = schedTime.trim()
                                    if (reminders.isNotEmpty()) body["reminderMinutes"] = reminders.sorted()
                                    if (repeat == "daily") body["isEveryday"] = true
                                    if (repeat == "weekly") body["scheduledDays"] = repeatDays.sorted()
                                    ApiClient.get().createTask(body)
                                } else {
                                    val schedules = habitTimes.mapNotNull { it.trim().takeIf { s -> s.matches(Regex("""([01]\d|2[0-3]):[0-5]\d""")) } }
                                        .map { mapOf("time" to it, "days" to habitDays.sorted()) }
                                    val body = mutableMapOf<String, Any?>(
                                        "title" to title.trim(),
                                        "description" to desc.ifBlank { null },
                                        "emoji" to emoji,
                                        "frequencyType" to freq,
                                        "verificationType" to verification
                                    )
                                    if (schedules.isNotEmpty()) body["schedules"] = schedules
                                    if (freq == "weekly") body["daysPerWeek"] = habitDays.sorted()
                                    if (freq == "interval" && interval.toIntOrNull() != null) body["intervalDays"] = interval.toInt()
                                    if (reminders.isNotEmpty()) body["reminderMinutes"] = reminders.sorted()
                                    ApiClient.get().createHabit(body)
                                }
                                onCreated()
                            } catch (e: Exception) {
                                err = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(300) ?: e.message
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && title.isNotBlank(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text(if (busy) "Saving…" else "Create") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
