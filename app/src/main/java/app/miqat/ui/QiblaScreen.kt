package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.CompassSensor

/**
 * THE QIBLA COMPASS — the direction, not a number.
 *
 * Hesham: «اسلوب تحديد القبلة ليس فقط نص كما انت فاعله! بل … جيروسكوب وبوصلة
 * وهكذا تعقيدلان الحالي غريب لا يطلع على اتجاه وانما يعطي قيمة مبهمة».
 *
 * So this screen is drawn, not typed: the rose turns with the phone, the gold
 * needle points at the Kaaba, the sky inside the ring is the app's one rich
 * moment, and when the needle finds the Kaaba the ring closes and says so.
 */
@Composable
fun QiblaScreen(
    state: AppState,
    fmt: Fmt,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val bearing = remember(state.city.lat, state.city.lon) {
        CompassSensor.qiblaBearing(state.city.lat, state.city.lon)
    }
    val distanceKm = remember(state.city.lat, state.city.lon) {
        CompassSensor.qiblaDistanceKm(state.city.lat, state.city.lon)
    }

    val sensor = remember { CompassSensor(context).apply { lastFix = CompassSensor.Fix(state.city.lat, state.city.lon) } }
    var heading by remember { mutableFloatStateOf(0f) }
    DisposableEffect(sensor) {
        sensor.onHeading = { heading = it }
        sensor.start()
        onDispose { sensor.stop() }
    }

    val needle = CompassSensor.needleAngle(bearing, heading)
    val aligned = CompassSensor.aligned(needle)

    MiqatScaffold("القبلة", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Space.md))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CompassRose(
                    qiblaBearing = bearing,
                    heading = heading,
                    aligned = aligned,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // the numbers, now MEANINGFUL because the needle above points
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmt.decimal(bearing.toDouble(), 1),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TabularFigures),
                        color = if (aligned) scheme.primary else scheme.onSurface,
                    )
                    Text("درجة عن الشمال", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmt.n(distanceKm.toInt()),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TabularFigures),
                        color = scheme.onSurface,
                    )
                    Text("كم إلى الكعبة", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(Space.md))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (aligned) scheme.primaryContainer else scheme.surfaceVariant,
                        RoundedCornerShape(Radius.panel),
                    )
                    .padding(Space.md),
            ) {
                Column {
                    Text(
                        when {
                            aligned -> "أنت متجه إلى الكعبة"
                            sensor.available -> "أدِر الجهاز حتى يستقيم المؤشّر على الكعبة"
                            else -> "لا تحتوي هذا الجهاز على بوصلة — الزاوية والمسافة محسوبتان بدقّة"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (aligned) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    )
                    if (sensor.available) {
                        Text(
                            "اتجاهك الحالي: ${fmt.decimal(heading.toDouble(), 1)}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
        }
    }
}

/**
 * The rose: the sky inside, the degrees on the rim, the Kaaba's needle in
 * gold, and the phone's own heading marked at the top. Drawn with Canvas so it
 * stays light and sharp at any size.
 */
@Composable
private fun CompassRose(
    qiblaBearing: Float,
    heading: Float,
    aligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val needleAngle = CompassSensor.needleAngle(qiblaBearing, heading)

    Canvas(modifier.aspectRatio(1f)) {
        val side = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = side / 2f - 10f

        // the sky — the one rich moment inside the ring
        drawCircle(
            brush = Brush.verticalGradient(
                // the app's own night sky — the compass had a violet/plum
                // gradient, the family the palette bans (it muddies)
                Sky.bands(SkyPhase.NIGHT),
                startY = center.y - radius,
                endY = center.y + radius,
            ),
            radius = radius,
            center = center,
        )
        // a horizon arc, like the app's own arc
        drawArc(
            color = scheme.primary.copy(alpha = .28f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius * .78f, center.y - radius * .78f),
            size = Size(radius * 1.56f, radius * 1.56f),
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        // the rim + the degree ticks (the rose turns with the phone)
        rotate(-heading, center) {
            drawCircle(
                color = if (aligned) scheme.primary else scheme.outline,
                radius = radius,
                center = center,
                style = Stroke(width = if (aligned) 4f else 2f),
            )
            for (deg in 0 until 360 step 5) {
                val major = deg % 30 == 0
                val a = Math.toRadians((deg - 90).toDouble())
                val outer = radius
                val inner = radius - if (major) 16f else 7f
                val c = if (major) Color(0xFFE6E2D6) else Sky.tick
                drawLine(
                    color = c,
                    start = Offset(
                        center.x + (outer * cos(a)).toFloat(),
                        center.y + (outer * sin(a)).toFloat(),
                    ),
                    end = Offset(
                        center.x + (inner * cos(a)).toFloat(),
                        center.y + (inner * sin(a)).toFloat(),
                    ),
                    strokeWidth = if (major) 2.5f else 1.2f,
                    cap = StrokeCap.Round,
                )
            }
            // the four cardinal marks, in Arabic
            val labels = listOf("ش" to 0f, "ق" to 90f, "ج" to 180f, "غ" to 270f)
            labels.forEach { (letter, deg) ->
                val a = Math.toRadians((deg - 90).toDouble())
                val r = radius - 34f
                val p = Offset(
                    center.x + (r * cos(a)).toFloat(),
                    center.y + (r * sin(a)).toFloat(),
                )
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb(230, 230, 226, 214)
                        textSize = 26f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(letter, p.x, p.y + 8f, paint)
                }
            }
        }

        // the phone's heading, at the top of the dial
        val top = Offset(center.x, center.y - radius + 4f)
        drawPath(
            Path().apply {
                moveTo(top.x, top.y)
                lineTo(top.x - 9f, top.y - 15f)
                lineTo(top.x + 9f, top.y - 15f)
                close()
            },
            color = scheme.secondary,
        )

        // the Kaaba's needle — the gold that means something
        rotate(needleAngle, center) {
            val tip = Offset(center.x, center.y - radius * .72f)
            val tail = Offset(center.x, center.y + radius * .30f)
            drawLine(
                color = scheme.primary,
                start = tail,
                end = tip,
                strokeWidth = 7f,
                cap = StrokeCap.Round,
            )
            // the arrow head
            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - 13f, tip.y + 26f)
                    lineTo(tip.x + 13f, tip.y + 26f)
                    close()
                },
                color = scheme.primary,
            )
        }
        // the Kaaba itself, at the centre
        drawCircle(color = scheme.surface, radius = 13f, center = center)
        drawCircle(color = scheme.primary, radius = 13f, center = center, style = Stroke(width = 2.5f))
        drawCircle(color = scheme.primary, radius = 4f, center = center)
    }
}
