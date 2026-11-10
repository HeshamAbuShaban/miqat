package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.IslamicDays
import java.util.Calendar

/**
 * CALENDAR (R4) — the month view, computed ON DEMAND («وضع المدار»): only the
 * visible month is computed, and only when the screen is open. No year-long
 * precomputation, no battery drain — the engine is µs/day, the discipline is
 * about *when* we ask for it.
 *
 * Each cell carries the Gregorian day and the Hijri day; tapping a day opens
 * THAT day's times.
 */
@Composable
fun CalendarScreen(
    state: AppState,
    fmt: Fmt,
    onOpenDay: (Int, Int, Int) -> Unit,
) {
    val now = System.currentTimeMillis()
    val (cy, cm, _) = state.cityDate(now)
    var year by remember { mutableIntStateOf(cy) }
    var month by remember { mutableIntStateOf(cm) }

    // the visible month's grid — computed lazily, only this month
    val cells = remember(year, month, state.city.lat, state.city.lon, state.utcOffset) {
        monthGrid(year, month, state.display.hijriOffsetDays)
    }
    val hijriLabel = cells.firstOrNull { it != null }?.let { Hijri.MONTHS_AR[(it.hijri.hMonth - 1).coerceIn(0, 11)] }

    Column(Modifier.fillMaxSize().padding(horizontal = Space.lg)) {
        Spacer(Modifier.height(Space.md))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق") {
                month--; if (month < 1) { month = 12; year-- }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${fmt.n(month)} / ${fmt.n(year)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (hijriLabel != null) {
                    Text(
                        "$hijriLabel ${fmt.n(cells.firstNotNullOfOrNull { it?.hijri?.hYear } ?: 0)}هـ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي") {
                month++; if (month > 12) { month = 1; year++ }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(Modifier.fillMaxWidth()) {
            ARABIC_WEEKDAYS.forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(cells.size) { index ->
                val cell = cells[index]
                if (cell == null) {
                    Box(Modifier.size(52.dp))
                } else {
                    DayCell(
                        day = cell.gregorianDay,
                        hijri = cell.hijri,
                        isToday = cell.isToday,
                        event = cell.event,
                        fmt = fmt,
                        onClick = { onOpenDay(year, month, cell.gregorianDay) },
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.md))
    }
}

@Composable
private fun IconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DayCell(
    day: Int,
    hijri: IslamicDay,
    isToday: Boolean,
    event: String?,
    fmt: Fmt,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            fmt.n(day),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            fmt.n(hijri.hDay),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (event != null) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

/** The visible month's cells (Gregorian day + its Hijri day + event marker). */
private data class Cell(
    val gregorianDay: Int,
    val hijri: IslamicDay,
    val isToday: Boolean,
    val event: String?,
)

private fun monthGrid(year: Int, month: Int, hijriOffset: Int): List<Cell?> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
    }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) % 7 // Saturday = 0, Sunday = 1 …
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val cells = mutableListOf<Cell?>()
    repeat(firstWeekday) { cells.add(null) }
    for (d in 1..days) {
        val hijri = Hijri.at(year, month, d)
        val event = when {
            IslamicDays.isEidAlFitr(hijri) -> "عيد"
            IslamicDays.isEidAlAdha(hijri) -> "عيد"
            IslamicDays.isDayOfArafah(hijri) -> "عرفة"
            IslamicDays.isAshura(hijri) -> "عاشوراء"
            IslamicDays.isWhiteDays(hijri) -> "أيام البيض"
            IslamicDays.isRamadan(hijri) -> "رمضان"
            else -> null
        }
        cells.add(
            Cell(
                gregorianDay = d,
                hijri = hijri,
                isToday = today.get(Calendar.YEAR) == year &&
                    today.get(Calendar.MONTH) + 1 == month &&
                    today.get(Calendar.DAY_OF_MONTH) == d,
                event = event,
            ),
        )
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private val ARABIC_WEEKDAYS = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

/** A single day: its times, computed on demand for that date. */
@Composable
fun DayDetailScreen(state: AppState, fmt: Fmt, year: Int, month: Int, day: Int, onBack: () -> Unit) {
    val times = state.times(year, month, day)
    val hijri = Hijri.at(year, month, day)
    MiqatScaffold("${fmt.n(day)} / ${fmt.n(month)} / ${fmt.n(year)}", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                Hijri.label(hijri, fmt.ar),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            PRAYER_NAMES.forEachIndexed { i, name ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Space.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        fmt.hhmm(minutesOf(times.list[i])),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Divider()
            }
            Spacer(Modifier.height(Space.md))
            Text(
                "محسوبة عند الطلب — بلا جدولة مسبقة لكل أيام السنة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
