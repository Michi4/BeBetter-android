package at.websters.bebetter.wear.tile

import android.content.Context
import android.graphics.Color
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

internal val BgAccent = Color.parseColor("#34D399")
internal val BgMuted = Color.parseColor("#9CA3AF")
internal val BgWhite = Color.parseColor("#FFFFFF")

internal fun tileText(ctx: Context, value: String, sizeSp: Float, color: Int = BgWhite, maxLines: Int = 2): LayoutElementBuilders.LayoutElement =
    LayoutElementBuilders.Text.Builder()
        .setText(value)
        .setMaxLines(maxLines)
        .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
        .setFontStyle(
            LayoutElementBuilders.FontStyle.Builder()
                .setSize(sp(sizeSp))
                .setColor(argb(color))
                .build()
        )
        .build()

/**
 * Root layout: full-screen Row (vertical center) → Column (horizontal center), tappable.
 * `assistant = true` launches the app straight into the chat screen.
 */
internal fun tileRoot(
    ctx: Context,
    assistant: Boolean,
    content: List<LayoutElementBuilders.LayoutElement>
): LayoutElementBuilders.LayoutElement {
    val col = LayoutElementBuilders.Column.Builder()
        .setWidth(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
    content.forEach { col.addContent(it) }
    val clickable = ModifiersBuilders.Clickable.Builder()
        .setId(if (assistant) "assistant" else "open")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(ctx.packageName)
                        .setClassName("at.websters.bebetter.wear.MainActivity")
                        .apply {
                            if (assistant) addKeyToExtraMapping("screen", ActionBuilders.stringExtra("assistant"))
                        }
                        .build()
                )
                .build()
        )
        .build()
    return LayoutElementBuilders.Row.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .addContent(col.setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable).build()).build())
        .build()
}

/**
 * BeBetter Today tile: due/total + streak, tap anywhere opens the app.
 */
class BeBetterTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CoroutineScope(Dispatchers.IO).future {
            val ctx = applicationContext
            val session = at.websters.bebetter.wear.WearSession(ctx)
            at.websters.bebetter.wear.WearClient.init(
                kotlinx.coroutines.runBlocking { session.base() }
            ) { runCatching { kotlinx.coroutines.runBlocking { session.token() } }.getOrNull() }
            val (due, total, streak) = try {
                val h = at.websters.bebetter.wear.WearClient.get().scheduled(java.time.LocalDate.now().toString()).habits
                val s = at.websters.bebetter.wear.WearClient.get().stats()
                Triple(h.count { it.completedToday != true }, h.size, s.activeStreak)
            } catch (_: Exception) { Triple(-1, -1, -1) }

            val root = tileRoot(
                ctx,
                assistant = false,
                content = listOf(
                    tileText(ctx, "BeBetter", 16f, BgAccent),
                    tileText(ctx, if (due < 0) "Tap to sign in" else "$due of $total due", 20f),
                    tileText(ctx, if (due < 0) "" else "🔥 $streak day streak", 14f, BgMuted)
                )
            )

            TileBuilders.Tile.Builder()
                .setResourcesVersion("3")
                .setFreshnessIntervalMillis(10 * 60 * 1000)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder().setLayout(
                            LayoutElementBuilders.Layout.Builder().setRoot(root).build()
                        ).build()
                    ).build()
                ).build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<androidx.wear.protolayout.ResourceBuilders.Resources> =
        CoroutineScope(Dispatchers.IO).future {
            androidx.wear.protolayout.ResourceBuilders.Resources.Builder().setVersion("3").build()
        }
}

/**
 * BeBetter Assistant tile: tap to jump straight into the AI chat.
 */
class AssistantTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CoroutineScope(Dispatchers.IO).future {
            val ctx = applicationContext
            val root = tileRoot(
                ctx,
                assistant = true,
                content = listOf(
                    tileText(ctx, "✨", 30f),
                    tileText(ctx, "Ask BeBetter", 18f, BgAccent),
                    tileText(ctx, "Tap to chat", 13f, BgMuted)
                )
            )
            TileBuilders.Tile.Builder()
                .setResourcesVersion("3")
                .setFreshnessIntervalMillis(60 * 60 * 1000)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder().setLayout(
                            LayoutElementBuilders.Layout.Builder().setRoot(root).build()
                        ).build()
                    ).build()
                ).build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<androidx.wear.protolayout.ResourceBuilders.Resources> =
        CoroutineScope(Dispatchers.IO).future {
            androidx.wear.protolayout.ResourceBuilders.Resources.Builder().setVersion("3").build()
        }
}
