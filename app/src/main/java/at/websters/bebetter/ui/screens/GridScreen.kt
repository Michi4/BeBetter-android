package at.websters.bebetter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.websters.bebetter.data.ApiClient
import at.websters.bebetter.ui.components.ContributionGridView
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun GridScreen() {
    val scope = rememberCoroutineScope()
    var year by remember { mutableStateOf(LocalDate.now().year) }
    var years by remember { mutableStateOf<List<Int>>(listOf(year)) }
    var grid by remember { mutableStateOf<Map<String, at.websters.bebetter.data.GridDay>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            loading = true
            try {
                years = ApiClient.get().gridYears().years.ifEmpty { listOf(LocalDate.now().year) }
                grid = ApiClient.get().grid("$year-01-01", "$year-12-31").grid
            } catch (_: Exception) {}
            loading = false
        }
    }
    LaunchedEffect(year) { load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Year in Review", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            years.sorted().forEach { y ->
                FilterChip(selected = y == year, onClick = { year = y }, label = { Text("$y") })
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        ContributionGridView(grid = grid, year = year)
        val total = grid.values.sumOf { it.completed }
        Text("$total completions in $year", style = MaterialTheme.typography.bodyMedium)
    }
}
