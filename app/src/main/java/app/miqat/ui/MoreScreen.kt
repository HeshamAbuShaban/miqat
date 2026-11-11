package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.miqat.core.MomentCategory
import app.miqat.core.Qibla
import app.miqat.core.Moments

/**
 * «المUQ» — the index of everything else, with a LIVE preview on every tile.
 *
 * The settings tree is a tree, not a destination: it is opened once a week,
 * while the compass, the tasbih and the acts of worship are opened daily.
 * So the bar keeps five daily things, and this screen holds the rest — each
 * tile carrying its current state, so it reads before you tap it.
 */
@Composable
fun MoreScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
    onOpen: (MoreTarget) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val now = nowState.value
    val (y, m, d) = state.cityDate(now)
    val times = state.times(y, m, d)
    val (ty, tm, td) = state.cityTomorrow(now)
    val tomorrow = state.times(ty, tm, td)
    val nowMinutes = (state.cityHourOfDay(now) * 60).toInt()
    val timeline = rememberTimeline(state, now)

    // the next act of worship (of any kind) — the tasbih/acts preview
    val nextAct = timeline
        .filter { it.isEvent && it.category != MomentCategory.FASTING }
        .filter { it.minute > nowMinutes }
        .minByOrNull { it.minute }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))
        Text(
            "المزيد",
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onBackground,
        )
        Text(
            "كل ما في التطبيق — كل بطاقة تحمل حالتها الآن",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.md))

        // the tiles — two columns, each with a live preview
        val tiles = listOf(
            MoreTile(
                "القبلة",
                "${fmt.decimalUnit(Qibla.bearing(state.city.lat, state.city.lon), 1, "°")} · " +
                    "${fmt.n(Qibla.distanceKm(state.city.lat, state.city.lon).toInt())} كم",
                MoreTarget.QIBLA,
            ),
            MoreTile(
                "التسبيح",
                "أذكار اليوم مع مراجعها وعدّاد",
                MoreTarget.TASBIH,
            ),
            MoreTile(
                "مواقيت العبادات",
                nextAct?.let { "${it.labelAr} · ${fmt.hhmm(it.minuteOfDay)}" } ?: "—",
                MoreTarget.ACTS,
            ),
            MoreTile(
                "أوقات السنة",
                "النوم والسحور والقيلولة وقيام الليل",
                MoreTarget.SUNNAH,
            ),
            MoreTile(
                "البُعد عن ميقات",
                "كم مرّ منذ أيّ وقت",
                MoreTarget.DISTANCE,
            ),
            MoreTile(
                "المدد بين المواقيت",
                "كم يبقى بين كل موعد والذي يليه",
                MoreTarget.GAPS,
            ),
            MoreTile(
                "احسب بين ميقوتين",
                "أيّ ميقوتين كان — من أيّ نوع",
                MoreTarget.INTERVAL,
            ),
        )

        tiles.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                pair.forEach { tile ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.card))
                            .background(scheme.surface)
                            .clickable { onOpen(tile.target) }
                            .padding(Space.md),
                    ) {
                        Text(
                            tile.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                        )
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            tile.preview,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.sm))
        }

        Spacer(Modifier.height(Space.sm))

        // the settings — honestly labelled as a tree
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.row))
                .background(scheme.surfaceVariant.copy(alpha = .45f))
                .clickable { onOpen(MoreTarget.SETTINGS) }
                .padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(scheme.outline, CircleShape),
            )
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    "الإعدادات",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                )
                Text(
                    "الموقع والحساب والإشعارات والمؤشرات والمظهر",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

/** One tile of «المUQ». */
data class MoreTile(
    val title: String,
    val preview: String,
    val target: MoreTarget,
)

/** What a «المUQ» tile opens. */
enum class MoreTarget { QIBLA, TASBIH, ACTS, SUNNAH, DISTANCE, GAPS, INTERVAL, SETTINGS }
