package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.ActsCatalog
import app.miqat.core.ClosingList
import app.miqat.core.CloseRole
import app.miqat.core.DayLine
import app.miqat.core.Hijri
import app.miqat.core.Moments
import app.miqat.core.Moment
import app.miqat.core.Proximity
import app.miqat.core.TimerKind

@Composable
internal fun ActRow(item: ActsCatalog.Live, fmt: Fmt, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val passed = item.state == ActsCatalog.ActState.PASSED
    val upcoming = item.state == ActsCatalog.ActState.UPCOMING
    val estimated = item.act.source.isNullOrBlank()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Space.md, vertical = 13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(if (passed) scheme.outline else scheme.primary),
        )
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.act.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (passed) scheme.onSurfaceVariant else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (upcoming) {
                    Spacer(Modifier.width(Space.xs))
                    Text(
                        "قادم",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = scheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(scheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                item.act.what,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (item.act.isWindow || item.act.isBoundary)
                        fmt.range(item.startMinute, item.endMinute)
                    else fmt.hhmm(item.startMinute),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Plex, fontFeatureSettings = TabularFigures,
                    ),
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    when {
                        upcoming -> "بعد ${fmt.duration(item.minutesAway)}"
                        passed -> "مضى"
                        else -> "جارٍ"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Plex, fontFeatureSettings = TabularFigures,
                    ),
                    color = if (upcoming || item.state == ActsCatalog.ActState.RUNNING)
                        scheme.secondary else scheme.onSurfaceVariant,
                )
            }
            // An estimated window must say so AND still say whether it passed.
            // Folding the two into one string used to hide the state.
            if (estimated) {
                Text(
                    "تقديري بحسب الاصطلاح",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                    color = scheme.tertiary,
                )
            }
        }
    }
}

@Composable
internal fun CtaTile(
    title: String,
    subtitle: String,
    glyph: Glyph,
    onClick: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(scheme.surface)
            .border(1.dp, scheme.outline, RoundedCornerShape(Radius.card))
            .clickable { onClick() }
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                .background(scheme.secondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            BrandGlyph(glyph, active = true, size = 18.dp)
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

internal fun closeRoleText(role: CloseRole): String = when (role) {
    CloseRole.ANCHOR -> "جارٍ"
    CloseRole.CLOSES_AT -> "ينتهي"
    CloseRole.INSIDE -> "قادم"
}

