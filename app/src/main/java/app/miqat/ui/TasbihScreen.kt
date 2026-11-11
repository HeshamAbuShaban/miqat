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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.AdhkarCitations
import app.miqat.core.MiqatData

/**
 * التسبيح — the daily practice, at last a home in the app (the advisory
 * council's seventh finding: a caring Muslim's daily act had no home).
 *
 * The texts are the DONOR's own reminder texts (21), each with the citation
 * the council verified — a text with no verified number prints the
 * collection-only line rather than a number we cannot stand behind.
 */
@Composable
fun TasbihScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val texts = remember {
        (1..6).flatMap { MiqatData.prayerEvents(context, it) }
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    MiqatScaffold("التسبيح", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "اضغط على الذكر عدّاً له — والنصُّ مع مرجعه",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            texts.forEach { item ->
                var count by remember { mutableIntStateOf(0) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .cardSurface(RoundedCornerShape(Radius.card))
                        .padding(Space.md),
                ) {
                    Text(
                        item.messageAr,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        AdhkarCitations.lineFor(item.id)
                            ?: "رواه: الأذكار — رقم الحديث غير متحقق منه",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(scheme.primaryContainer)
                                .clickable { count++ },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${count}",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            "اضغط للتسبيح",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        if (count > 0) {
                            Text(
                                "تصفير",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.primary,
                                modifier = Modifier.clickable { count = 0 },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.sm))
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
