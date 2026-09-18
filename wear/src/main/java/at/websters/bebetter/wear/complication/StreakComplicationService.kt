package at.websters.bebetter.wear.complication

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import at.websters.bebetter.wear.WearClient
import at.websters.bebetter.wear.WearSession

/**
 * Watch-face complication: current streak (SHORT_TEXT + RANGED_VALUE).
 * Updates every 10 min via manifest UPDATE_PERIOD_SECONDS.
 */
class StreakComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("8").build(),
                ComplicationText.EMPTY
            ).setTitle(PlainComplicationText.Builder("BeBetter").build()).build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 8f, min = 0f, max = 30f,
                contentDescription = PlainComplicationText.Builder("BeBetter streak").build()
            ).setText(PlainComplicationText.Builder("8").build()).build()
            else -> null
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val session = WearSession(applicationContext)
        WearClient.init(session.base()) { session.cachedToken }
        val streak = runCatching { WearClient.get().stats().activeStreak }.getOrDefault(0)
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("$streak").build(),
                ComplicationText.EMPTY
            ).setTitle(PlainComplicationText.Builder("BeBetter").build()).build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = streak.toFloat().coerceIn(0f, 100f), min = 0f, max = 30f,
                contentDescription = PlainComplicationText.Builder("BeBetter $streak day streak").build()
            ).setText(PlainComplicationText.Builder("$streak").build()).build()
            else -> null
        }
    }
}
