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
                TaskRow(task = t, onChanged = { load() })
                // long-press actions simplified: delete via swipe replacement row
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
