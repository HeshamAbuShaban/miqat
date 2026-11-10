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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.miqat.core.City
import app.miqat.core.CityStore
import app.miqat.core.Qibla

/**
 * The city picker as a MODAL — a task deserves its own surface, not a card
 * floating over the home. Search is DB-backed and live (S2), and every result
 * row is INFORMATIVE (R2): local time now, distance from the current city,
 * country, and the city's own default calculation method.
 */
@Composable
fun CityPickerSheet(
    fromCity: City,
    arabicIndic: Boolean,
    onSelect: (City) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    LaunchedEffect(query) {
        results = if (query.isNotBlank()) CityStore.search(context, query) else emptyList()
    }
    val now = remember { System.currentTimeMillis() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .55f))) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Space.lg, vertical = Space.lg)
        ) {
            Text(
                "اختر مدينتك",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.md))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("ابحث بالعربية أو الإنجليزية…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(Space.sm))
            if (results.isEmpty()) {
                Text(
                    if (query.isBlank()) "اكتب اسم المدينة" else "لا نتائج",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Space.lg),
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(results) { c ->
                    CityResultRow(c, fromCity, now, arabicIndic) { onSelect(c) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.fillMaxWidth().clickable { onDismiss() },
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("إغلاق", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun CityResultRow(c: City, from: City, now: Long, arabicIndic: Boolean, onClick: () -> Unit) {
    val localMinute = (((utcHourOfDay(now) + c.utcOffset) % 24.0 + 24.0) % 24.0 * 60).toInt()
    val km = Qibla.distanceKm(from.lat, from.lon, c.lat, c.lon)
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                c.ar.ifBlank { c.en },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                buildString {
                    if (c.ar.isNotBlank() && c.en.isNotBlank()) append("${c.en} · ")
                    append(c.countryCode)
                    append(" · التوقيت الآن ${app.miqat.core.NumFmt.hhmm(localMinute, arabicIndic, false)}")
                    append(" · ")
                    append(if (km < 1.0) "أقل من ١ كم" else "${app.miqat.core.NumFmt.n(km.toInt(), arabicIndic)} كم")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "الطريقة الافتراضية: ${c.calcMethod}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** UTC hour-of-day of an epoch instant (the city offset is added by the caller). */
private fun utcHourOfDay(now: Long): Double {
    val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = now
    return utc.get(java.util.Calendar.HOUR_OF_DAY) + utc.get(java.util.Calendar.MINUTE) / 60.0 +
        utc.get(java.util.Calendar.SECOND) / 3600.0
}

