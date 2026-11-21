package app.miqat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ONE expanding mechanism for the whole app — no per-screen booleans with
 * different easing. The team asked for «التوسيع/الطي»; this is it.
 *
 * A single-open accordion is owned by the screen (openId: String?); the
 * body animates its height with a calm spring, and the chevron follows.
 */
@Composable
fun Expando(
    id: String,
    openId: String?,
    onToggle: (String?) -> Unit,
    title: String,
    value: String? = null,
    body: @Composable () -> Unit,
) {
    val open = openId == id
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "expando-chevron",
    )
    ColumnWithSpring {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggle(if (open) null else id) }
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Plex, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Kufi, fontFeatureSettings = TabularFigures,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "⌄",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Kufi),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation).padding(start = Space.sm),
            )
        }
        if (open) body()
    }
}

@Composable
private fun ColumnWithSpring(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(
        Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
        content = content,
    )
}
