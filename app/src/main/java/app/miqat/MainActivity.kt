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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.Appointment
import app.miqat.core.City
import app.miqat.core.CityStore
import app.miqat.core.DayClock
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.IslamicDays
import app.miqat.core.Location
import app.miqat.core.MethodTable
import app.miqat.core.PrayerTimes
import app.miqat.core.Qibla
import app.miqat.core.WorshipWindows
import app.miqat.core.formatHms
import java.util.Calendar
import java.util.TimeZone

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
private val PRAYER_KEY = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

/** Fragment of الذاريات ١٨ — the ONLY sharia text on the الأسحار card until
 *  Muslim 2702 is verified (guardrail, research-agenda.md §Guardrail). */
private const val AYAH_ASHAR = "﴿…وَبِٱلْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ﴾ — الذاريات ١٨"
private const val TAQDIRI = "تقديري بحسب الاصطلاح"

/**
 * S5 v1 — Day-Arc home (R1/R5/R3 v0 of research-agenda.md):
 * one hero with the R1 dual clocks (remaining primary, elapsed secondary,
 * seconds ticking), worship-window cards (الضحى / الأسحار / الليل), a
 * "why this time" provenance sheet per prayer row (R3), rich city rows (R2).
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

/** R1 ticker: one aligned 1 Hz state at Home scope; LEAVES read [State.value]
 *  so recomposition is scoped to the ticking texts, not the whole Home. */
@Composable
private fun rememberNowState(): State<Long> {
    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val n = System.currentTimeMillis()
            now.value = n
            kotlinx.coroutines.delay(1000 - n % 1000)
        }
    }
    return now
}

/** City wall-clock "now" — the device tz is never trusted for a picked city. */
private fun cityCalendar(city: City, epochMillis: Long): Calendar {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = epochMillis + (city.utcOffset * 3600_000.0).toLong()
    return cal // its UTC fields are the city's wall clock
}

private fun cityHourOfDay(city: City, epochMillis: Long): Double {
    val c = cityCalendar(city, epochMillis)
    return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0 +
        c.get(Calendar.SECOND) / 3600.0 + c.get(Calendar.MILLISECOND) / 3.6e6
}

private fun cityDate(city: City, epochMillis: Long): Triple<Int, Int, Int> {
    val c = cityCalendar(city, epochMillis)
    return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

/** (y, m, d) of the day AFTER the given city date. */
private fun cityTomorrow(city: City, epochMillis: Long): Triple<Int, Int, Int> {
    val c = cityCalendar(city, epochMillis)
    c.add(Calendar.DAY_OF_MONTH, 1)
    return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

@Composable
private fun Home() {
    val context = LocalContext.current
    var picked by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(CityStore.MAKKAH) }
    // First frame never blocks: render on the Makkah constant, refine from the
    // store async; the 218k-row DB is never loaded into memory (CityStore).
    LaunchedEffect(Unit) {
        CityStore.makkah(context)?.let { if (!picked) selected = it }
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var detailIndex by remember { mutableStateOf<Int?>(null) }
    val nowState = rememberNowState()

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars).padding(16.dp)
        ) {
            Header(selected) { pickerOpen = true }
            Spacer(Modifier.height(14.dp))
            DayArcHero(selected, nowState)
            Spacer(Modifier.height(12.dp))
            ContextCards(selected, nowState)
            Spacer(Modifier.height(12.dp))
            TimesList(selected, nowState) { detailIndex = it }
            Spacer(Modifier.height(20.dp))
        }
        if (pickerOpen) {
            CityPicker(fromCity = selected, onSelect = {
                picked = true
                selected = it
                pickerOpen = false
            }, onDismiss = { pickerOpen = false })
        }
        detailIndex?.let { idx ->
            TimeDetailSheet(selected, idx) { detailIndex = null }
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

private fun dayAppointments(times: List<Double>): List<Appointment> =
    PRAYER_KEY.mapIndexed { i, k -> Appointment(k, PRAYER_AR[i], times[i]) }

@Composable
private fun DayArcHero(city: City, nowState: State<Long>) {
    val now = nowState.value
    val (y, m, d) = cityDate(city, now)
    val times = timesFor(city, y, m, d)
    val nowHour = cityHourOfDay(city, now)

    val seg = DayClock.position(dayAppointments(times), nowHour)
    val mins = times.map { minutesOf(it) }

    // arc position of "now" between fajr and isha (0..1) for the dot
    val dayStart = mins[0].toDouble()
    val dayEnd = mins[5].toDouble() + if (mins[5] < mins[0]) 1440.0 else 0.0
    val nowF = if (nowHour * 60 < mins[0]) 0.0 else ((nowHour * 60 - dayStart) / (dayEnd - dayStart)).coerceIn(0.0, 1.0)

    Column(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(CardHi, Card)), RoundedCornerShape(22.dp)
        ).padding(16.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val w = size.width
            val top = 8f
            val bottom = size.height - 12f
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
            val path = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
            }
            drawPath(path, Color(0xFF232B4A), style = Stroke(7f, cap = StrokeCap.Round))
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
            // prayer dots; the NEXT miqat is highlighted (amber)
            val fr = listOf(0f, 0.22f, 0.45f, 0.66f, 0.85f, 1f)
            fr.forEachIndexed { i, f ->
                val c = pointAt(f)
                val isNextDot = seg != null && seg.next.key == PRAYER_KEY[i] &&
                    !seg.nextIsTomorrow
                drawCircle(if (isNextDot) Amber else FgDim.copy(alpha = .6f), 7f, c)
            }
            val nowPt = pointAt(tEnd)
            drawCircle(Cream, 11f, nowPt)
        }
        if (seg != null) {
            Text(
                "المتبقي لميقات ${seg.next.labelAr}" + if (seg.nextIsTomorrow) " (غداً)" else "",
                color = FgDim, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            // PRIMARY clock: remaining, HH:MM:SS ticking
            Text(
                "%d:%02d:%02d".format(
                    seg.remainingSeconds / 3600, seg.remainingSeconds % 3600 / 60, seg.remainingSeconds % 60),
                color = Fg, fontSize = 40.sp, fontWeight = FontWeight(500),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            // SECONDARY clock: elapsed since the current miqat began
            Text(
                "منذ ${seg.current.labelAr} %d:%02d:%02d".format(
                    seg.elapsedSeconds / 3600, seg.elapsedSeconds % 3600 / 60, seg.elapsedSeconds % 60),
                color = FgDim, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun ContextCards(city: City, nowState: State<Long>) {
    val islamic = remember { Hijri.today() }
    val card = IslamicDays.todayCard(islamic)

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

    val now = nowState.value
    val (y, m, d) = cityDate(city, now)
    val (ty, tm, td) = cityTomorrow(city, now)
    val t = timesFor(city, y, m, d)
    val tomorrow = timesFor(city, ty, tm, td)

    // night portions over Maghrib → next Fajr (our stated convention — R5)
    val night = WorshipWindows.night(maghrib = t[4], fajrNext = tomorrow[0])
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniCard("منتصف الليل", hhmmAr(minutesOf(night.midnightHour)), Modifier.weight(1f),
            accent = Blue, sub = TAQDIRI)
        MiniCard("الثلث الأخير", hhmmAr(minutesOf(night.lastThirdStartHour)), Modifier.weight(1f),
            accent = Blue, sub = TAQDIRI)
    }
    Spacer(Modifier.height(10.dp))

    WorshipCard(
        city = city, nowState = nowState,
        title = "وقت الضحى", accent = Teal,
        start = WorshipWindows.duha(sunrise = t[1], dhuhr = t[2]).let { it.startHour to it.endHour },
        sub = "من بعد الشروق إلى ما قبل الظهر · $TAQDIRI",
    )
    Spacer(Modifier.height(10.dp))
    WorshipCard(
        city = city, nowState = nowState,
        title = "وقت الأسحار", accent = Gold,
        start = WorshipWindows.sahar(maghrib = t[4], fajrNext = tomorrow[0]).let { it.startHour to it.endHour },
        sub = "$AYAH_ASHAR · $TAQDIRI",
    )
    Spacer(Modifier.height(10.dp))

    val q = Qibla.bearing(city.lat, city.lon)
    MiniCard(
        "القبلة — ${"%.1f".format(q)}° من الشمال",
        "على بعد ${kmLabel(Qibla.distanceKm(city.lat, city.lon))}",
        Modifier.fillMaxWidth(), accent = Gold,
    )
}

/** One worship window: window bounds as value, ticking «جارٍ الآن» while inside. */
@Composable
private fun WorshipCard(
    city: City, nowState: State<Long>,
    title: String, accent: Color,
    start: Pair<Double, Double>,  // (startHour, endHour)
    sub: String,
) {
    val nowHour = cityHourOfDay(city, nowState.value)
    val (s, e) = start
    val inWindow = !s.isNaN() && !e.isNaN() && nowHour >= s && nowHour < e
    val value = if (s.isNaN() || e.isNaN()) "--:-- --:--"
    else "${hhmmAr(minutesOf(s))} — ${hhmmAr(minutesOf(e))}"
    Column(
        Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(title, color = FgDim, fontSize = 11.sp)
        Text(
            (if (inWindow) "جارٍ الآن · " else "") + value,
            color = accent, fontSize = 15.sp, fontWeight = FontWeight(500),
        )
        Text(sub, color = FgDim, fontSize = 11.sp)
    }
}

@Composable
private fun MiniCard(label: String, value: String, modifier: Modifier, accent: Color, sub: String? = null) {
    Column(modifier.background(Card, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, color = FgDim, fontSize = 11.sp)
        if (value.isNotBlank()) Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight(500))
        if (sub != null) Text(sub, color = FgDim, fontSize = 10.sp)
    }
}

private fun kmLabel(km: Double): String =
    if (km < 1.0) "أقل من ١ كم" else "${"%,.0f".format(km)} كم"

private fun minLabel(v: Double): String =
    if (v == v.toInt().toDouble()) "${v.toInt()}" else "$v"

@Composable
private fun TimesList(city: City, nowState: State<Long>, onRowTap: (Int) -> Unit) {
    val now = nowState.value
    val (y, m, d) = cityDate(city, now)
    val times = timesFor(city, y, m, d)
    val nowHour = cityHourOfDay(city, now)
    val seg = DayClock.position(dayAppointments(times), nowHour)
    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(vertical = 6.dp)) {
        PRAYER_AR.forEachIndexed { i, name ->
            val key = PRAYER_KEY[i]
            val isNext = seg?.next?.key == key
            val isCurrent = seg?.current?.key == key
            Row(
                Modifier.fillMaxWidth().clickable { onRowTap(i) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(name, color = if (isNext) Amber else Fg, fontSize = 15.sp)
                    when {
                        // SECONDARY ticking clocks on the two live rows only
                        isNext -> Text(
                            "بعد %d:%02d:%02d".format(
                                seg.remainingSeconds / 3600, seg.remainingSeconds % 3600 / 60, seg.remainingSeconds % 60),
                            color = Amber.copy(alpha = .7f), fontSize = 11.sp,
                        )
                        isCurrent -> Text(
                            "جارٍ منذ %d:%02d:%02d".format(
                                seg.elapsedSeconds / 3600, seg.elapsedSeconds % 3600 / 60, seg.elapsedSeconds % 60),
                            color = FgDim, fontSize = 11.sp,
                        )
                    }
                }
                Text(hhmmAr(minutesOf(times[i])), color = if (isNext) Amber else FgDim, fontSize = 15.sp)
            }
        }
    }
}

/** R3 v1 — «لماذا هذا الوقت؟»: honest provenance for one prayer (decisions 2026-11-10 (4)). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDetailSheet(city: City, index: Int, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val now = remember { System.currentTimeMillis() }
    val (y, m, d) = cityDate(city, now)
    val times = timesFor(city, y, m, d)
    val method = MethodTable[city.calcMethod]
    val p = method.withSeasonalCorrections(m * 100 + d)
    val seasonal = method.seasonal.firstOrNull { it.fromMMdd <= m * 100 + d && m * 100 + d <= it.toMMdd }
    val margins = listOfNotNull(
        ("الفجر" to p.margins.fajr), ("الشروق" to p.margins.sunrise), ("الظهر" to p.margins.dhuhr),
        ("العصر" to p.margins.asr), ("المغرب" to p.margins.maghrib), ("العشاء" to p.margins.isha),
    ).filter { it.second != 0.0 }.joinToString("، ") { "${it.first} +${it.second.toInt()}" }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardHi) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp)
        ) {
            Text(PRAYER_AR[index], color = Gold, fontSize = 18.sp, fontWeight = FontWeight(600))
            Spacer(Modifier.height(8.dp))
            Text("المعروض ${hhmmAr(minutesOf(times[index]))}", color = Fg, fontSize = 24.sp, fontWeight = FontWeight(500))
            Text(
                "المحسوب ${formatHms(times[index])} — العرض مقرّب للدقيقة نصفاً للأعلى",
                color = FgDim, fontSize = 12.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text("من أين جاء هذا الوقت؟", color = Fg, fontSize = 14.sp, fontWeight = FontWeight(500))
            Spacer(Modifier.height(6.dp))
            Detail("الطريقة", city.calcMethod)
            Detail("زاوية الفجر", "${p.fajrAngle}°")
            Detail("المغرب", if (p.maghribMinutesMode) "بعد الغروب بـ ${minLabel(p.maghribValue)} دقيقة" else "زاوية ${p.maghribValue}°")
            Detail("العشاء", if (p.ishaMinutesMode) "بعد المغرب بـ ${minLabel(p.ishaValue)} دقيقة" else "زاوية ${p.ishaValue}°")
            Detail("العصر", "بظل ${p.asrShadow}" + if (p.asrShadow == 2) " (حنفي)" else " (شافعي)")
            if (margins.isNotBlank()) Detail("احتياط الحساب", margins)
            if (seasonal != null)
                Detail("نافذة تعديل موسمي", "من ${seasonal.fromMMdd / 100}/${seasonal.fromMMdd % 100} إلى ${seasonal.toMMdd / 100}/${seasonal.toMMdd % 100}")
            Detail("قاعدة الاتساع العالي", when (method.highLatRule) {
                app.miqat.core.HighLatitudeRule.NONE -> "لا تُطبق"
                app.miqat.core.HighLatitudeRule.MID_NIGHT -> "منتصف الليل"
                app.miqat.core.HighLatitudeRule.ONE_SEVENTH_NIGHT -> "سُبع الليل"
                app.miqat.core.HighLatitudeRule.ANGLE_NIGHT -> "نسبة من الليل بالزاوية"
            })
            Detail("المنطقة الزمنية", "UTC+${city.utcOffset}")
            // R3b register (توخي وحذر) — named cause, per council copy review
            if (kotlin.math.abs(city.lat) >= 45.0 && method.highLatRule != app.miqat.core.HighLatitudeRule.NONE) {
                Spacer(Modifier.height(8.dp))
                Text("حساب تقريبي في هذه المنطقة", color = Sunset, fontSize = 13.sp, fontWeight = FontWeight(500))
                Text(
                    "في خطوط العرض العالية تُقدَّر أوقات الفجر والعشاء بنسب الليل، وقد يتقدّم الموعد أو يتأخر دقائق.",
                    color = FgDim, fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "حتى لا يلبس على الناس: هذه القيم من حساب التطبيق؛ في الحالات الخاصة فتاوى ومواعيد بلدك هي المرجع.",
                color = FgDim, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FgDim, fontSize = 13.sp)
        Text(value, color = Fg, fontSize = 13.sp)
    }
}

@Composable
private fun CityPicker(fromCity: City, onSelect: (City) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    // DB-backed search; LaunchedEffect cancels the previous keystroke's query.
    LaunchedEffect(query) {
        if (query.isNotBlank()) results = CityStore.search(context, query)
        else results = emptyList()
    }
    // snapshot for the rich rows (R2): local time now + distance from the picked city
    val now = remember { System.currentTimeMillis() }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC050810)).clickable { onDismiss() }
    ) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(20.dp)
                .background(CardHi, RoundedCornerShape(20.dp)).clickable(enabled = false) {}
                .padding(14.dp)
        ) {
            OutlinedTextField(
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
                        Column(Modifier.weight(1f)) {
                            Text(c.ar.ifBlank { c.en }, color = Fg, fontSize = 15.sp)
                            val localTime = hhmmAr(
                                ((cityHourOfDay(c, now) * 60).toInt()) % (24 * 60))
                            val km = Qibla.distanceKm(fromCity.lat, fromCity.lon, c.lat, c.lon)
                            Text(
                                "التوقيت الآن $localTime · على بعد ${kmLabel(km)}",
                                color = FgDim, fontSize = 11.sp,
                            )
                        }
                        Text(c.countryCode, color = FgDim, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
