package app.salatukplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salatukplus.core.Location
import app.salatukplus.core.MethodTable
import app.salatukplus.core.PrayerTimes
import java.util.Calendar
import java.util.TimeZone

/** S0 skeleton: renders live engine output. Real UI lands in S5 (ROADMAP). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimesSheet()
                }
            }
        }
    }
}

@Composable
private fun TimesSheet() {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val tzOffset = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3_600_000.0
    val location = Location(latitude = 31.7683, longitude = 35.2137, utcOffsetHours = tzOffset)
    val method = MethodTable["makkah"] // per-city default lands in S2
    val times = PrayerTimes.compute(
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
        location, method,
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SalatukPlus", fontSize = 28.sp)
        Text("engine demo — ${method.id} method", fontSize = 14.sp)
        listOf(
            "Fajr" to times.fajr, "Sunrise" to times.sunrise, "Dhuhr" to times.dhuhr,
            "Asr" to times.asr, "Maghrib" to times.maghrib, "Isha" to times.isha,
        ).forEach { (label, value) ->
            Text("$label  ${PrayerTimes.format(value)}", fontSize = 20.sp)
        }
    }
}
