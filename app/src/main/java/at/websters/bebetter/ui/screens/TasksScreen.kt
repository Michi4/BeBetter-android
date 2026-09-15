package at.websters.bebetter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.data.Task
import at.websters.bebetter.ui.components.TaskRow
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TasksScreen() {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            try { tasks = ApiClient.get().tasks(LocalDate.now().toString()).tasks } catch (e: Exception) { err = e.message }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    fun moveTask(t: Task, dir: Int) {
        val today = tasks.filter { it.isDueToday }
        val ids = today.map { it.id }
        val fi = ids.indexOf(t.id)
        if (fi < 0) return
        val ni = (fi + dir).coerceIn(0, ids.size - 1)
        if (ni == fi) return
        val newIds = ids.toMutableList().apply { removeAt(fi); add(ni, t.id) }
        val byId = tasks.associateBy { it.id }
        tasks = newIds.mapNotNull { byId[it] } + tasks.filter { !newIds.contains(it.id) }
        scope.launch {
            try { ApiClient.get().reorderTasks(mapOf("ids" to newIds)) } catch (_: Exception) { load() }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tasks", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Quick add task") }, modifier = Modifier.weight(1f), singleLine = true)
            Button(onClick = {
                scope.launch {
                    try { ApiClient.get().createTask(mapOf("title" to title.trim())); title = ""; load() } catch (e: Exception) { err = e.message }
                }
            }, enabled = title.isNotBlank()) { Icon(Icons.Filled.Add, null) }
        }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(tasks.filter { it.isDueToday }) { t ->
                TaskRow(task = t, onChanged = { load() }, onMove = { dir -> moveTask(t, dir) })
            }
            item {
                if (tasks.none { it.isDueToday }) Text("All clear! 🎉")
            }
            item {
                Text("Later / upcoming (${tasks.count { !it.isDueToday }})", style = MaterialTheme.typography.titleSmall)
            }
            items(tasks.filter { !it.isDueToday }) { t ->
                TaskRow(task = t, onChanged = { load() })
            }
        }
    }
}
