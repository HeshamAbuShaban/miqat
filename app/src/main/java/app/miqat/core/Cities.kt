package app.miqat.core

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * City rows from the bundled asset `assets/cities.tsv.gz`
 * (built by tools/build_miqat_assets.py from the donor cities DB, 218k rows).
 *
 * v0 loader: whole-file lazy load + linear scan search (~10 ms/query).
 * S2 replaces this with the indexed Room DB; the row contract stays.
 */
data class City(
    val en: String,
    val ar: String,
    val countryCode: String,
    val lat: Double,
    val lon: Double,
    val utcOffset: Double,
    val calcMethod: String,
)

object Cities {

    @Volatile
    private var cached: List<City>? = null

    fun load(context: Context): List<City> {
        cached?.let { return it }
        val rows = ArrayList<City>(220_000)
        context.assets.open("cities.tsv.gz").use { raw ->
            BufferedReader(InputStreamReader(GZIPInputStream(raw), Charsets.UTF_8)).use { r ->
                r.readLine() // header
                while (true) {
                    val line = r.readLine() ?: break
                    val c = line.split('|')
                    if (c.size < 7) continue
                    val lat = c[3].toDoubleOrNull() ?: continue
                    val lon = c[4].toDoubleOrNull() ?: continue
                    val tz = c[5].toDoubleOrNull() ?: continue
                    rows.add(City(c[0], c[1], c[2], lat, lon, tz, c[6]))
                }
            }
        }
        cached = rows
        return rows
    }

    /** Search-as-you-type over EN prefix/contains + AR contains; top [limit]. */
    fun search(cities: List<City>, query: String, limit: Int = 30): List<City> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()
        val out = ArrayList<City>(limit)
        for (c in cities) {
            val en = c.en
            val hit = when {
                en.startsWith(qLower, ignoreCase = true) -> 0
                en.contains(qLower, ignoreCase = true) -> 1
                c.ar.contains(q) -> 2
                else -> -1
            }
            if (hit >= 0) out.add(c)
            if (out.size >= limit * 3) break
        }
        out.sortWith(compareBy({ it.en.length }, { it.en }))
        return out.take(limit)
    }
}
