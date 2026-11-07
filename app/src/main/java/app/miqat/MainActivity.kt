package app.miqat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.City
import app.miqat.core.Cities
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.IslamicDays
import app.miqat.core.Location
import app.miqat.core.MethodTable
import app.miqat.core.PrayerTimes
import app.miqat.core.Qibla
import java.util.Calendar

/* Day-Arc palette (design-brief.md §2) — hardcoded v0 until the S5 theme system. */
private val Bg = Color(0xFF0B1020)
private val Card = Color(0xFF121830)
private val CardHi = Color(0xFF141B33)
private val Fg = Color(0xFFE9EDFB)
private val FgDim = Color(0xFF8A93B8)
private val Amber = Color(0xFFFFB74D)
private val Gold = Color(0xFFEAC964)
private val Cream = Color(0xFFF8E7AE)
private val Teal = Color(0xFF7FD0C8)
private val Blue = Color(0xFF5A74E8)
private val Sunset = Color(0xFFFF7A59)

private val PRAYER_AR = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

/**
 * S5 v0 — Day-Arc home: one hero (next-prayer countdown on the day arc),
 * contextual cards below (S9), city picker over the bundled 218k-city asset.
 * Offline-only; engine = S1 corpus-certified PrayerTimes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    Home()
                }
            }
        }
    }
}

@Composable
private fun Home() {
    val context = LocalContext.current
    val cities = remember { Cities.load(context) }
    var selected by remember {
        mutableStateOf(
            cities.firstOrNull { it.ar.startsWith("مكة") } ?: cities.first()
        )
    }
    var pickerOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Header(selected) { pickerOpen = true }
            Spacer(Modifier.height(14.dp))
            DayArcHero(selected)
            Spacer(Modifier.height(12.dp))
            ContextCards(selected)
            Spacer(Modifier.height(12.dp))
            TimesList(selected)
            Spacer(Modifier.height(20.dp))
        }
        if (pickerOpen) {
            CityPicker(cities, onSelect = {
                selected = it
                pickerOpen = false
            }, onDismiss = { pickerOpen = false })
        }
    }
}

@Composable
private fun Header(city: City, onOpenPicker: () -> Unit) {
    val islamic = remember { Hijri.today() }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الميقات", color = Gold, fontSize = 20.sp, fontWeight = FontWeight(600))
            Text(city.ar.ifBlank { city.en }, color = FgDim, fontSize = 13.sp,
                 modifier = Modifier.clickable { onOpenPicker() })
        }
        Spacer(Modifier.height(4.dp))
        Text(Hijri.label(islamic), color = Fg, fontSize = 14.sp)
    }
}

/** Compute the 6 times for a city on a specific date (local hours). */
private fun timesFor(city: City, year: Int, month: Int, day: Int): List<Double> {
    val t = PrayerTimes.compute(
        year, month, day,
        Location(city.lat, city.lon, city.utcOffset),
        MethodTable[city.calcMethod],
    )
    return listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
}

private fun minutesOf(hours: Double): Int {
    val total = (((hours + 0.5 / 60.0) % 24.0) + 24.0) % 24.0
    return (total * 60).toInt() % (24 * 60)
}

private fun hhmmAr(totalMin: Int): String {
    val h = totalMin / 60
    val m = totalMin % 60
    return "%d:%02d".format(h, m)
}

@Composable
private fun DayArcHero(city: City) {
    val cal = Calendar.getInstance()
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val times = timesFor(city, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    val mins = times.map { minutesOf(it) }

    // next prayer = first after now (wraps to fajr tomorrow)
    var nextIdx = mins.indexOfFirst { it > nowMin }
    var tomorrow = false
    if (nextIdx < 0) { nextIdx = 0; tomorrow = true }
    val remaining = if (tomorrow) mins[0] + 1440 - nowMin else mins[nextIdx] - nowMin

    // arc position of "now" between fajr and isha (0..1) for the dot
    val dayStart = mins[0].toDouble()
    val dayEnd = mins[5].toDouble() + if (mins[5] < mins[0]) 1440.0 else 0.0
    val nowF = if (nowMin < mins[0]) 0.0 else (nowMin - dayStart) / (dayEnd - dayStart)

    Column(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(CardHi, Card)), RoundedCornerShape(22.dp)
        ).padding(16.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val w = size.width
            val top = 8f
            val bottom = size.height - 12f
            // the arc: quadratic from left base to right base
            val p0 = Offset(w * 0.06f, bottom)
            val p2 = Offset(w * 0.94f, bottom)
            val p1 = Offset(w * 0.5f, top)
            fun pointAt(t: Float): Offset {
                val mt = 1 - t
                return Offset(
                    mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
                    mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y,
                )
            }
            // base arc (dim)
            val path = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
            }
            drawPath(path, Color(0xFF232B4A), style = Stroke(7f, cap = StrokeCap.Round))
            // elapsed sweep in the moment color
            val sweep = Path()
            val tEnd = (0.06 + 0.88 * nowF).toFloat().coerceIn(0f, 1f)
            if (tEnd > 0.02f) {
                val steps = 40
                for (i in 0..steps) {
                    val t = tEnd * i / steps
                    val pt = pointAt(t)
                    if (i == 0) sweep.moveTo(pt.x, pt.y) else sweep.lineTo(pt.x, pt.y)
                }
                drawPath(sweep, Amber, style = Stroke(7f, cap = StrokeCap.Round))
            }
            // prayer dots
            val fr = listOf(0f, 0.22f, 0.45f, 0.66f, 0.85f, 1f)
            fr.forEachIndexed { i, f ->
                val c = pointAt(f)
                drawCircle(if (i == nextIdx && !tomorrow) Amber else FgDim.copy(alpha = .6f), 7f, c)
            }
            // now marker
            val nowPt = pointAt(tEnd)
            drawCircle(Cream, 11f, nowPt)
        }
        Text(
            if (tomorrow) "المتبقي لميقات الفجر" else "المتبقي لميقات ${PRAYER_AR[nextIdx]}",
            color = FgDim, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "%d:%02d".format(remaining / 60, remaining % 60),
            color = Fg, fontSize = 40.sp, fontWeight = FontWeight(500),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "${PRAYER_AR[nextIdx]} ${hhmmAr(mins[nextIdx])}" + if (tomorrow) " (غداً)" else "",
            color = FgDim, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ContextCards(city: City) {
    val islamic = remember { Hijri.today() }
    val card = IslamicDays.todayCard(islamic)
    // three of the night: sunset → next fajr
    val cal = Calendar.getInstance()
    val t = timesFor(city, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    val tomorrow = timesFor(
        city,
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH) + 1,
    )
    val maghrib = minutesOf(t[4])
    val fajrNext = minutesOf(tomorrow[0])
    val nightLen = ((fajrNext - maghrib) + 1440) % 1440
    val third = (maghrib + nightLen * 2 / 3) % 1440
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val mins = t.map { minutesOf(it) }
    var nextIdx = mins.indexOfFirst { it > nowMin }
    var wraps = false
    if (nextIdx < 0) { nextIdx = 0; wraps = true }
    val nextLabel = "${PRAYER_AR[nextIdx]} ${hhmmAr(mins[nextIdx])}" + if (wraps) " (غداً)" else ""

    card?.let { (title, sub) ->
        Column(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF13203B), Card)), RoundedCornerShape(18.dp)
            ).padding(14.dp)
        ) {
            Text(title, color = Fg, fontSize = 14.sp, fontWeight = FontWeight(500))
            Text(sub, color = FgDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniCard("التالي", nextLabel, Modifier.weight(1f), accent = Amber)
        MiniCard("ثلث الليل الأخير", hhmmAr(third), Modifier.weight(1f), accent = Blue)
    }
    Spacer(Modifier.height(10.dp))
    val q = Qibla.bearing(city.lat, city.lon)
    MiniCard(
        "القبلة — ${"%.1f".format(q)}° من الشمال",
        "على بعد ${"%,.0f".format(Qibla.distanceKm(city.lat, city.lon))} كم",
        Modifier.fillMaxWidth(), accent = Gold,
    )
}

@Composable
private fun MiniCard(label: String, value: String, modifier: Modifier, accent: Color) {
    Column(modifier.background(Card, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, color = FgDim, fontSize = 11.sp)
        if (value.isNotBlank()) Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight(500))
    }
}

@Composable
private fun TimesList(city: City) {
    val cal = Calendar.getInstance()
    val times = timesFor(city, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(vertical = 6.dp)) {
        PRAYER_AR.forEachIndexed { i, name ->
            val m = minutesOf(times[i])
            val isNext = m > nowMin && minsUpTo(times, i).none { it > nowMin }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, color = if (isNext) Amber else Fg, fontSize = 15.sp)
                Text(hhmmAr(m), color = if (isNext) Amber else FgDim, fontSize = 15.sp)
            }
        }
    }
}

private fun minsUpTo(times: List<Double>, upto: Int): List<Int> =
    times.subList(0, upto).map { minutesOf(it) }

@Composable
private fun CityPicker(cities: List<City>, onSelect: (City) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Cities.search(cities, query) }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC050810)).clickable { onDismiss() }
    ) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(20.dp)
                .background(CardHi, RoundedCornerShape(20.dp)).clickable(enabled = false) {}
                .padding(14.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن مدينتك…", color = FgDim, fontSize = 14.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Fg, fontSize = 15.sp),
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                items(results) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(c) }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(c.ar.ifBlank { c.en }, color = Fg, fontSize = 15.sp)
                            if (c.ar.isNotBlank() && c.en.isNotBlank())
                                Text(c.en, color = FgDim, fontSize = 11.sp)
                        }
                        Text(c.countryCode, color = FgDim, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
