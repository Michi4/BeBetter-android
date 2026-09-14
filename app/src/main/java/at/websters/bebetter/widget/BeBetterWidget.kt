package at.websters.bebetter.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import at.websters.bebetter.MainActivity
import at.websters.bebetter.data.ApiClient
import java.time.LocalDate

class BeBetterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Best-effort cached fetch; widget never blocks on auth errors.
        val (due, total, streak) = try {
            val h = ApiClient.get().scheduledHabits(LocalDate.now().toString()).habits
            val s = ApiClient.get().statsOverview()
            Triple(h.count { it.completedToday != true }, h.size, s.activeStreak)
        } catch (_: Exception) { Triple(0, 0, 0) }
        provideContent {
            WidgetContent(due, total, streak)
        }
    }

    @Composable
    private fun WidgetContent(due: Int, total: Int, streak: Int) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color(0xFF0B0C0F)).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BeBetter", style = TextStyle(color = ColorProvider(Color(0xFF34D399))))
            Text("$due/$total due today", style = TextStyle(color = ColorProvider(Color.White)))
            Text("🔥 ${streak}d streak", style = TextStyle(color = ColorProvider(Color(0xFFA3ABB8))))
        }
    }
}

class BeBetterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BeBetterWidget()
}
