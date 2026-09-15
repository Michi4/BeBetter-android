package at.websters.bebetter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.data.Habit
import at.websters.bebetter.ui.components.BeBetterCard
import at.websters.bebetter.ui.components.HabitRow
import at.websters.bebetter.ui.components.SectionHeader
import at.websters.bebetter.ui.components.WebChip
import at.websters.bebetter.ui.components.formatTimeWeb
import at.websters.bebetter.ui.theme.BeBetterTokens
import at.websters.bebetter.ui.theme.SectionTitle
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun HabitsScreen(onDetail: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            try { habits = ApiClient.get().habits().habits } catch (e: Exception) { err = e.message }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = BeBetterTokens.AccentBtnHover,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) { Icon(Icons.Filled.Add, "Add", modifier = Modifier.size(24.dp)) }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                SectionTitle("Habits")
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = BeBetterTokens.Accent)
            }
            items(habits, key = { it.id }) { h ->
                HabitRow(habit = h, onOpen = { onDetail(h.id) }, onToggled = { load() })
            }
            if (!loading && habits.isEmpty()) {
                item { BeBetterCard(modifier = Modifier.fillMaxWidth()) { Text("No habits yet — create your first! 🌱", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            item { SectionTitle("History") }
            item {
                BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) { Icon(Icons.Filled.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("September 2026", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tuesday, Sep 15 · Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(18.dp)) }
                    }
                    // Mo-Su header
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Mo","Tu","We","Th","Fr","Sa","Su").forEach { d -> Text(d, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                    }
                    // premium mock grid 5 rows to approximate screenshot - September 2026 has 1 on Tuesday
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // row1: offset 1 blank, 1-6
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f).aspectRatio(1f))
                            for (d in 1..6) {
                                val isGreen = d in 1..5
                                val bg = if (isGreen) BeBetterTokens.AccentBtnHover else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                val isToday = d == 15
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg).then(if (isToday) Modifier.border(1.5.dp, BeBetterTokens.Accent, RoundedCornerShape(10.dp)) else Modifier), contentAlignment = Alignment.Center) {
                                    Text("$d", fontSize = 13.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, color = if (isGreen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                        // row2: 7-13
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (d in 7..13) {
                                val isGreen = d in 7..13
                                val bg = if (isGreen) BeBetterTokens.AccentBtnHover else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg).then(if (d==15) Modifier.border(1.5.dp, BeBetterTokens.Accent, RoundedCornerShape(10.dp)) else Modifier), contentAlignment = Alignment.Center) {
                                    Text("$d", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                            }
                        }
                        // row3: 14-20 with 15 selected ring
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (d in 14..20) {
                                val isGreen = d == 14
                                val isSelected = d == 15
                                val bg = when {
                                    isSelected -> MaterialTheme.colorScheme.surface
                                    isGreen -> BeBetterTokens.AccentBtnHover
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg).then(if (isSelected) Modifier.border(1.5.dp, BeBetterTokens.Accent, RoundedCornerShape(10.dp)) else Modifier), contentAlignment = Alignment.Center) {
                                    Text("$d", fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.onSurface else if (isGreen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                        // rows 4-5 blanks
                        repeat(2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(7) { idx ->
                                    val d = 21 + it*7 + idx
                                    if (d <= 30) Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Text("$d", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } else Box(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Today", fontSize = 13.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("0/6 done", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(0f).background(BeBetterTokens.Accent))
                        }
                    }
                }
            }
        }
    }
    if (showCreate) {
        CreateSheet(initialMode = "habit", onDismiss = { showCreate = false }, onCreated = { showCreate = false; load() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFormDialog(existing: Habit? = null, onDismiss: () -> Unit, onSaved: () -> Unit) {
    // Kept for quick edit; full create uses CreateSheet. Tidied to web tokens.
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🌱") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var freq by remember { mutableStateOf(existing?.frequencyType ?: "daily") }
    var verification by remember { mutableStateOf(existing?.verificationType ?: "honor") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "New habit" else "Edit habit") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, shape = RoundedCornerShape(8.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(freq, {}, readOnly = true, label = { Text("Frequency") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(8.dp))
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    listOf("daily", "weekly", "custom").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { freq = it; expanded = false })
                    }
                }
            }
            var vExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(vExpanded, { vExpanded = it }) {
                OutlinedTextField(verification, {}, readOnly = true, label = { Text("Verification") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vExpanded) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(8.dp))
                ExposedDropdownMenu(vExpanded, { vExpanded = false }) {
                    listOf("honor", "photo", "be_better_cam").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { verification = it; vExpanded = false })
                    }
                }
            }
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
    }, confirmButton = {
        Button(onClick = {
            busy = true
            scope.launch {
                try {
                    val body = mutableMapOf<String, Any?>("title" to title.trim(), "description" to desc.ifBlank { null }, "emoji" to emoji, "frequencyType" to freq, "verificationType" to verification)
                    if (existing == null) ApiClient.get().createHabit(body) else ApiClient.get().updateHabit(existing.id, body)
                    onSaved()
                } catch (e: Exception) { err = e.message; busy = false }
            }
        }, enabled = !busy && title.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = BeBetterTokens.Accent) } })
}

@Composable
fun HabitDetailScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var habit by remember { mutableStateOf<Habit?>(null) }
    var edit by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || habit == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val file = File(ctx.cacheDir, "proof_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { ins -> file.outputStream().use { ins.copyTo(it) } }
                val part = MultipartBody.Part.createFormData(
                    "photo", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val up = ApiClient.get().upload(part)
                val url = up.url.ifBlank { up.fileUrl }
                ApiClient.get().completeHabit(mapOf("habitId" to habit!!.id, "photo" to url, "status" to "completed"))
                msg = "Photo proof submitted! 📸"
                habit = ApiClient.get().habitDetail(id).habit
            } catch (e: Exception) {
                err = e.message
            }
            busy = false
        }
    }

    fun load() {
        scope.launch {
            try { habit = ApiClient.get().habitDetail(id).habit } catch (e: Exception) { err = e.message }
        }
    }
    LaunchedEffect(id) { load() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row { TextButton(onClick = onBack) { Text("← Back", color = BeBetterTokens.Accent) }; Spacer(Modifier.weight(1f)); habit?.let { TextButton(onClick = { edit = true }) { Text("Edit", color = BeBetterTokens.Accent) } } }
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            msg?.let { Text(it, color = BeBetterTokens.Accent, fontSize = 13.sp) }
        }
        val h = habit
        if (h == null) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = BeBetterTokens.Accent) }
            return@LazyColumn
        }
        item {
            BeBetterCard(modifier = Modifier.fillMaxWidth()) {
                Text("${h.emoji.ifBlank { "🌱" }} ${h.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                h.description?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WebChip(h.frequencyType)
                    WebChip("🔥 ${h.bestStreak}")
                    WebChip(h.verificationType, kind = if (h.verificationType == "honor") "gray" else "amber")
                    if (h.active == false) WebChip("paused", "amber")
                }
                val sched = h.parsedSchedules()
                if (sched.isNotEmpty()) {
                    Text("Schedules: " + sched.joinToString { "${formatTimeWeb(it.time)} [${it.days?.joinToString() ?: "all"}]" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                ApiClient.get().completeHabit(mapOf("habitId" to h.id, "status" to "completed"))
                                msg = "Habit completed! 🎉"
                                load()
                            } catch (e: Exception) { err = e.message }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BeBetterTokens.AccentBtn, contentColor = androidx.compose.ui.graphics.Color.White)
                ) { Text("Log completion") }
                if (h.verificationType == "photo" || h.verificationType == "be_better_cam") {
                    OutlinedButton(onClick = { pickPhoto.launch("image/*") }, enabled = !busy) { Text("📸 Proof") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { scope.launch { runCatching { ApiClient.get().breakStart(h.id) }; load() } }, modifier = Modifier.weight(1f)) { Text("Pause") }
                OutlinedButton(onClick = { scope.launch { runCatching { ApiClient.get().breakEnd(h.id) }; load() } }, modifier = Modifier.weight(1f)) { Text("Resume") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        try { ApiClient.get().finishHabit(h.id); msg = "Habit finished. 🎯"; load() }
                        catch (e: Exception) { err = e.message }
                    }
                }, modifier = Modifier.weight(1f)) { Text("Finish") }
                OutlinedButton(
                    onClick = { scope.launch { runCatching { ApiClient.get().deleteHabit(h.id) }; onBack() } },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) { Text("Delete") }
            }
        }
        item { SectionHeader("How it works") }
        item {
            Text("• Honor = one tap. Photo/BeBetter Cam = attach proof (gallery or camera).\n• Pausing freezes streaks (like vacation). Finishing archives the habit.\n• Reminders fire via local notifications (hourly sync).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (edit && habit != null) {
        HabitFormDialog(existing = habit, onDismiss = { edit = false }, onSaved = { edit = false; load() })
    }
}
