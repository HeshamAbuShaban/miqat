package app.miqat.ui

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.DstInfo
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.MiqatData
import app.miqat.core.Moments
import java.util.Calendar

@Composable
internal fun DayCell(
    cell: app.miqat.core.CalendarCell,
    primaryHijri: Boolean,
    emphasiseActs: Boolean,
    showPrimary: Boolean,
    showSecondary: Boolean,
    showEvents: Boolean,
    lens: String,
    fmt: Fmt,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // the lens dims UNMATCHED days; nothing disappears (the ledger stays whole)
    val relevant = when (lens) {
        "صيام" -> cell.isFast
        "مناسبات" -> cell.isRite
        else -> true
    }
    val cellAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (relevant) 1f else 0.32f, label = "lens",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.row))
            .background(if (cell.isToday) scheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .graphicsLayer { alpha = cellAlpha }
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (cell.turnsHere) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .height(1.dp)
                    .background(scheme.primary),
            )
        }
        if (cell.isToday) {
            Box(
                Modifier
                    .width(12.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(app.miqat.ui.Sky.sunMark),
            )
        }
        if (showPrimary) {
            Text(
                text = if (primaryHijri) fmt.n(cell.hijriDay) else fmt.n(cell.gregorianDay),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                color = if (cell.isToday) scheme.onPrimaryContainer else scheme.onSurface,
            )
        }
        if (showSecondary) {
            Text(
                text = if (primaryHijri) fmt.n(cell.gregorianDay) else fmt.n(cell.hijriDay),
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
        }
        if (cell.turnsHere && cell.turnsToLabel.isNotBlank()) {
            Text(
                text = cell.turnsToLabel,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                maxLines = 1,
            )
        }
        if (showEvents && (emphasiseActs || cell.isFast || cell.isRite)) {
            Spacer(Modifier.height(3.dp))
            // BADGES, not hairlines: SHAPE says the type, SIZE says the level
            // (the founder's card-type language, without a game score).
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Bottom,
            ) {
                // ONE type (a fast bar), FOUR cuts — rank is the ink inside
                // the same family, never a new colour, size-only signal or
                // game level. Same grammar in every screen that shows a fast.
                when (cell.fastKind) {
                    // R1 — nafl: raw cut stone, hollow, square corners
                    app.miqat.core.IslamicDays.FastDay.MON_THU -> Box(
                        Modifier.width(12.dp).height(6.dp)
                            .border(1.dp, scheme.secondary, RoundedCornerShape(0.dp)),
                    )
                    // R2 — sunnah: dressed once, a trace of ink inside
                    app.miqat.core.IslamicDays.FastDay.WHITE,
                    app.miqat.core.IslamicDays.FastDay.ASHURA,
                    app.miqat.core.IslamicDays.FastDay.SIX_SHAWWAL -> Box(
                        Modifier.width(16.dp).height(7.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(scheme.secondary.copy(alpha = 0.10f))
                            .border(1.5.dp, scheme.secondary, RoundedCornerShape(1.dp)),
                    )
                    // R3 — Arafah: double-ruled, the inner rule IS the mark
                    app.miqat.core.IslamicDays.FastDay.ARAFAH -> Box(
                        Modifier.width(20.dp).height(7.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .border(2.dp, scheme.secondary, RoundedCornerShape(1.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Box(
                            Modifier.width(14.dp).height(2.dp).background(scheme.secondary),
                        )
                    }
                    // R4 — Ramadan: filled, with ONE gold inlay at the start
                    app.miqat.core.IslamicDays.FastDay.RAMADAN -> Box(
                        Modifier.width(44.dp).height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(scheme.secondary),
                        contentAlignment = androidx.compose.ui.Alignment.CenterEnd,
                    ) {
                        Box(Modifier.width(6.dp).height(2.dp).background(scheme.primary))
                    }
                    else -> Unit // Eid and tashreeq veto the fast badge
                }
                when (cell.occasion) {
                    app.miqat.core.DayOccasion.ARAFAH -> Box(
                        Modifier.size(12.dp).graphicsLayer { rotationZ = 45f }
                            .clip(RoundedCornerShape(2.dp))
                            .background(scheme.onTertiaryContainer),
                    )
                    app.miqat.core.DayOccasion.EID -> Box(
                        Modifier.width(8.dp).height(13.dp)
                            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                            .background(scheme.onTertiaryContainer),
                    )
                    app.miqat.core.DayOccasion.HAJJ_RITE -> Box(
                        Modifier.width(8.dp).height(13.dp)
                            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                            .border(1.5.dp, scheme.onTertiaryContainer,
                                RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)),
                    )
                    app.miqat.core.DayOccasion.EVE_RAMADAN -> Box(
                        Modifier.width(12.dp).height(6.dp)
                            .border(1.5.dp, scheme.onTertiaryContainer, RoundedCornerShape(2.dp)),
                    )
                    else -> Unit
                }
            }
        }
    }
}

private val Color_Transparent = androidx.compose.ui.graphics.Color(0x00000000)
