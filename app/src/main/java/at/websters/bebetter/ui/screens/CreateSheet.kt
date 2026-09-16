package at.websters.bebetter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.theme.BeBetterTokens
import kotlinx.coroutines.launch

private val COMMON_EMOJIS = listOf("🌱","🔥","💧","🏃","📚","🧘","💪","🥗","😴","🎯","⭐","✨","💡","🎨","🎵","📝","🧠","❤️","🌙","☀️")
private val DAYS = listOf("Su","Mo","Tu","We","Th","Fr","Sa")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSheet(initialMode: String = "task", initialTitle: String = "", initialDescription: String = "", onDismiss: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(initialMode) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDescription) }
    var emoji by remember { mutableStateOf("🌱") }
    var showEmoji by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    var dueDate by remember { mutableStateOf("") }
    var schedTime by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("once") }
    var repeatDays by remember { mutableStateOf(setOf<Int>()) }
    var reminders by remember { mutableStateOf(setOf<Int>()) }
    var customReminder by remember { mutableStateOf("") }

    // habit specific
    var schedulePreset by remember { mutableStateOf("Daily") }
    var verification by remember { mutableStateOf("honor") }
    var habitTimes by remember { mutableStateOf(listOf<String>()) }
    var currentTime by remember { mutableStateOf("") }
    var anyTime by remember { mutableStateOf(true) }
    var habitDays by remember { mutableStateOf(setOf(0,1,2,3,4,5,6)) }
    var publish by remember { mutableStateOf(false) }

    fun applyPreset(preset: String) {
        schedulePreset = preset
        habitDays = when (preset) {
            "Daily" -> setOf(0,1,2,3,4,5,6)
            "Weekdays" -> setOf(1,2,3,4,5)
            "Weekends" -> setOf(0,6)
            "Every 2 days" -> setOf(0,1,2,3,4,5,6)
            "Every 3 days" -> setOf(0,1,2,3,4,5,6)
            "Every week" -> setOf(0,1,2,3,4,5,6)
            else -> habitDays
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Create New", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "Close", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Mode Toggle - like screenshot: Task/Habit pills in light gray container
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Task
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (mode == "task") BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f))
                        .clickable { mode = "task" }
                        .border(if (mode == "task") 0.dp else 0.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("☰", fontSize = 12.sp, color = if (mode == "task") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Task", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (mode == "task") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (mode == "habit") BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f))
                        .clickable { mode = "habit" },
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("◎", fontSize = 12.sp, color = if (mode == "habit") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Habit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (mode == "habit") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (mode == "task") {
                Text("What do you need to do?", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(title, { title = it }, placeholder = { Text("Task title", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 15.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                )
                Text("Description (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(desc, { desc = it }, placeholder = { Text("Add details...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth().height(88.dp), shape = RoundedCornerShape(12.dp), minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
                Text("Due date (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(dueDate, { dueDate = it }, placeholder = { Text("mm / dd / yyyy", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Text("📅", fontSize = 16.sp) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                    // Time button like screenshot
                    Box(Modifier.height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable {}.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("◷", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Time", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("◷", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Set a time", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // quick time input if set
                if (schedTime.isNotBlank() || true) {
                    // hidden until user taps "Set a time" - for simplicity show field
                }

                err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                Button(
                    onClick = {
                        busy = true; err = null
                        scope.launch {
                            try {
                                val body = mutableMapOf<String, Any?>("title" to title.trim(), "description" to desc.ifBlank { null }, "emoji" to emoji)
                                if (dueDate.isNotBlank()) body["dueDate"] = dueDate.trim()
                                if (schedTime.isNotBlank()) body["scheduledTime"] = schedTime.trim()
                                if (reminders.isNotEmpty()) body["reminderMinutes"] = reminders.sorted()
                                ApiClient.get().createTask(body); onCreated()
                            } catch (e: Exception) { err = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(300) ?: e.message; busy = false }
                        }
                    },
                    enabled = !busy && title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White, disabledContainerColor = BeBetterTokens.AccentBtn.copy(alpha = 0.4f))
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)); Text(if (busy) "Saving…" else "Create Task", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

            } else {
                // HABIT
                Text("What habit do you want to build?", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(title, { title = it }, placeholder = { Text("Habit title", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 15.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
                Text("Description (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(desc, { desc = it }, placeholder = { Text("Why is this important?", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth().height(88.dp), shape = RoundedCornerShape(12.dp), minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeBetterTokens.Accent, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
                Text("Emoji", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).clickable { showEmoji = !showEmoji }, contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 22.sp)
                    }
                    Text("Pick an emoji to represent this habit", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                if (showEmoji) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(COMMON_EMOJIS) { e ->
                            Text(e, fontSize = 22.sp, modifier = Modifier.clickable { emoji = e; showEmoji = false }.padding(8.dp))
                        }
                    }
                }
                Text("Schedule", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Schedule chips like screenshot: Daily Weekdays Weekends Every 2 days Every 3 days Every week
                val chipModifier = Modifier.height(36.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Daily","Weekdays","Weekends","Every 2 days").forEach { label ->
                            val sel = schedulePreset == label
                            Box(Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { applyPreset(label) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = if (sel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Every 3 days","Every week").forEach { label ->
                            val sel = schedulePreset == label
                            Box(Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { applyPreset(label) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = if (sel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                // Time section like screenshot: Anytime + day pills
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)).padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("◷", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("◷", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (anyTime) "Anytime" else currentTime.ifBlank { "Anytime" }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DAYS.forEachIndexed { i, d ->
                                val sel = habitDays.contains(i)
                                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).clickable {
                                    habitDays = if (sel) habitDays - i else habitDays + i
                                }, contentAlignment = Alignment.Center) {
                                    Text(d, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (sel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                // Add another time dashed
                Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, BeBetterTokens.Accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).background(BeBetterTokens.Accent.copy(alpha = 0.04f)).clickable {
                    val t = currentTime.trim()
                    if (t.matches(Regex("""([01]\d|2[0-3]):[0-5]\d""")) && !habitTimes.contains(t)) { habitTimes = habitTimes + t; currentTime = ""; anyTime = false }
                }, contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp), tint = BeBetterTokens.Accent); Text("Add another time", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = BeBetterTokens.Accent)
                    }
                }
                if (habitTimes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        habitTimes.forEachIndexed { idx, t ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface); TextButton(onClick = {
                                    val kept = habitTimes.filterIndexed { i, _ -> i != idx }
                                    habitTimes = kept
                                    if (kept.isEmpty() && currentTime.isBlank()) anyTime = true
                                }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                // Time quick input (always visible so "Add another time" is usable)
                OutlinedTextField(currentTime, { currentTime = it; anyTime = it.isBlank() && habitTimes.isEmpty() }, placeholder = { Text("HH:MM e.g. 07:00", fontSize = 13.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (showAdvanced) "⌃" else "⌄", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (showAdvanced) "Hide advanced" else "Show advanced", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Demo: photo verification, reminders, publishing, buddies and challenges require an account.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Text("Verification", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("honor" to "Honor", "photo" to "Photo").forEach { (v,l) ->
                                val sel = verification == v
                                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { verification = v }, contentAlignment = Alignment.Center) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (v=="honor") "🛡" else "📷", fontSize = 12.sp); Text(l, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = if (sel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Text("Reminders", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "At time", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m").forEach { (v,l) ->
                                val sel = reminders.contains(v)
                                Box(Modifier.height(32.dp).clip(RoundedCornerShape(10.dp)).background(if (sel) BeBetterTokens.AccentBtn else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable {
                                    reminders = if (sel) reminders - v else reminders + v
                                }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(l, fontSize = 12.sp, color = if (sel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(customReminder, { customReminder = it.filter { c->c.isDigit()}.take(3) }, placeholder = { Text("Custom", fontSize = 13.sp) }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                                customReminder.toIntOrNull()?.let { if (it in 1..1440) reminders = reminders + it; customReminder="" }
                            }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)) }
                        }
                        Text("Add multiple reminders. Last custom value is remembered.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        // Publish
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).background(if (publish) BeBetterTokens.Accent else MaterialTheme.colorScheme.surface).clickable { publish = !publish }, contentAlignment = Alignment.Center) {
                                    if (publish) Text("✓", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White)
                                }
                                Column {
                                    Text("Publish as public preset", fontSize = 13.sp, fontWeight = FontWeight.Medium); Text("Others can discover and use this habit template", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                            Text("🌐", fontSize = 16.sp)
                        }
                        Text("Accountability Buddies", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField("", {}, placeholder = { Text("Search friends to add as buddies...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        Text("Challenge Friends", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField("", {}, placeholder = { Text("Search friends to challenge...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    }
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                Button(
                    onClick = {
                        busy = true; err = null
                        scope.launch {
                            try {
                                val schedules = habitTimes.mapNotNull { it.trim().takeIf { s->s.matches(Regex("""([01]\d|2[0-3]):[0-5]\d""")) } }.map { mapOf("time" to it, "days" to habitDays.sorted()) }.ifEmpty {
                                    if (!anyTime && currentTime.matches(Regex("""([01]\d|2[0-3]):[0-5]\d"""))) listOf(mapOf("time" to currentTime.trim(), "days" to habitDays.sorted())) else emptyList()
                                }
                                val body = mutableMapOf<String, Any?>("title" to title.trim(), "description" to desc.ifBlank { null }, "emoji" to emoji, "verificationType" to verification, "makePublic" to publish)
                                if (schedules.isNotEmpty()) body["schedules"] = schedules else {
                                    // use preset days as interval mapping
                                    when (schedulePreset) {
                                        "Daily" -> body["schedules"] = listOf(mapOf("time" to null, "days" to listOf(0,1,2,3,4,5,6)))
                                        "Weekdays" -> body["schedules"] = listOf(mapOf("days" to listOf(1,2,3,4,5)))
                                        "Weekends" -> body["schedules"] = listOf(mapOf("days" to listOf(0,6)))
                                        "Every 2 days" -> body["intervalDays"] = 2
                                        "Every 3 days" -> body["intervalDays"] = 3
                                        "Every week" -> body["intervalDays"] = 7
                                    }
                                }
                                if (reminders.isNotEmpty()) body["reminderMinutes"] = reminders.sorted()
                                ApiClient.get().createHabit(body); onCreated()
                            } catch (e: Exception) { err = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.take(300) ?: e.message; busy=false }
                        }
                    },
                    enabled = !busy && title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White, disabledContainerColor = BeBetterTokens.AccentBtn.copy(alpha = 0.4f))
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("◎", fontSize = 14.sp); Text(if (busy) "Saving…" else "Create Habit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
