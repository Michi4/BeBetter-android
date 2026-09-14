package at.websters.bebetter.wear.tile

import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import at.websters.bebetter.wear.WearClient
import at.websters.bebetter.wear.WearSession
import java.time.LocalDate

/**
 * Pixel Watch Tile: due habits + streak. Tap opens the Wear app.
 */
class BeBetterTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CoroutineScope(Dispatchers.IO).future {
            val session = WearSession(applicationContext)
            WearClient.init(session.base()) { runCatching { kotlinx.coroutines.runBlocking { session.token() } }.getOrNull() }
            val (due, total, streak) = try {
                val h = WearClient.get().scheduled(LocalDate.now().toString()).habits
                val s = WearClient.get().stats()
                Triple(h.count { it.completedToday != true }, h.size, s.activeStreak)
            } catch (_: Exception) { Triple(0, 0, 0) }

            val ctx = applicationContext
            val column = LayoutElementBuilders.Column.Builder()
                .addContent(
                    Text.Builder(ctx, "BeBetter")
                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                        .setColor(ColorBuilders.ColorProp.Builder().setArgb(0xFF34D399.toInt()).build())
                        .build()
                )
                .addContent(
                    Text.Builder(ctx, "$due/$total due")
                        .setTypography(Typography.TYPOGRAPHY_BODY1)
                        .build()
                )
                .addContent(
                    Text.Builder(ctx, "🔥 ${streak}d streak")
                        .setTypography(Typography.TYPOGRAPHY_BODY2)
                        .build()
                )
                .build()

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(10 * 60 * 1000)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder().setLayout(
                            LayoutElementBuilders.Layout.Builder().setRoot(column).build()
                        ).build()
                    ).build()
                ).build()
        }
}
