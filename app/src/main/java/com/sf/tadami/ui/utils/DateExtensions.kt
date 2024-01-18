package com.sf.tadami.ui.utils

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import java.text.DateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

private const val MILLISECONDS_IN_DAY = 86_400_000L

fun Date.toRelativeString(
    context: Context,
    range: Int = 7,
    dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()),
): String {
    if (range == 0) {
        return dateFormat.format(this)
    }
    val now = Date()
    val difference =
        now.timeWithOffset.floorNearest(MILLISECONDS_IN_DAY) - this.timeWithOffset.floorNearest(
            MILLISECONDS_IN_DAY
        )
    val days = difference.floorDiv(MILLISECONDS_IN_DAY).toInt()
    return when {
        difference < 0 -> context.getString(R.string.recently)
        difference < MILLISECONDS_IN_DAY -> context.getString(R.string.relative_time_today)
        difference < MILLISECONDS_IN_DAY.times(range) -> context.resources.getQuantityString(
            R.plurals.relative_time,
            days,
            days,
        )
        else -> dateFormat.format(this)
    }
}

private val Date.timeWithOffset: Long
    get() {
        return Calendar.getInstance().run {
            time = this@timeWithOffset
            val dstOffset = get(Calendar.DST_OFFSET)
            this@timeWithOffset.time + timeZone.rawOffset + dstOffset
        }
    }

fun Long.floorNearest(to: Long): Long {
    return this.floorDiv(to) * to
}

fun Long.toDateKey(): Date {
    val cal = Calendar.getInstance()
    cal.time = Date(this)
    cal[Calendar.HOUR_OF_DAY] = 0
    cal[Calendar.MINUTE] = 0
    cal[Calendar.SECOND] = 0
    cal[Calendar.MILLISECOND] = 0
    return cal.time
}

fun Date.toTimestampString(): String {
    val dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    return dateFormat.format(this)
}

@Composable
@ReadOnlyComposable
fun relativeTimeSpanString(epochMillis: Long): String {
    val now = Instant.now().toEpochMilli()
    return when {
        epochMillis <= 0L -> stringResource(R.string.relative_time_span_never)
        now - epochMillis < 1.minutes.inWholeMilliseconds -> stringResource(
            R.string.updates_last_update_info_just_now,
        )
        else -> DateUtils.getRelativeTimeSpanString(epochMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}