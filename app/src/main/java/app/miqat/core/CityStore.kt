package app.miqat.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * City row (decisions.md 2026-11-07 (3) naming: donor-adjacent domain names).
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

/**
 * Indexed city storage over the bundled asset `assets/cities-v2.db`
 * (218,032 rows, built by tools/build_miqat_assets.py from the donor cities DB).
 *
 * S2 decision (decisions.md 2026-11-10 (2)): framework SQLiteDatabase, no Room —
 * no ksp/kapt, F-Droid-clean tree. Copy-on-first-open to filesDir; the asset
 * VERSION lives in the FILENAME, so bumping it replaces stale copies for free.
 * Queries are indexed SQL — the 218k rows never live in memory, and the first
 * UI frame never blocks on I/O (home renders on [MAKKAH], refined async).
 */
object CityStore {

    /** Default home city — rendered instantly before the store opens. */
    val MAKKAH = City("Makkah", "مكة", "SA", 21.42664, 39.82563, 3.0, "makkah")

    private const val ASSET = "cities-v2.db"

    @Volatile
    private var db: SQLiteDatabase? = null

    fun open(context: Context): SQLiteDatabase {
        db?.let { return it }
        synchronized(this) {
            db?.let { return it }
            val file: File = context.getDatabasePath(ASSET)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                val tmp = File(file.parentFile, "$ASSET.tmp")
                context.assets.open(ASSET).use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                if (!tmp.renameTo(file)) {
                    // cross-device rename fallback
                    tmp.copyTo(file, overwrite = true)
                    tmp.delete()
                }
            }
            return SQLiteDatabase.openDatabase(
                file.path, null, SQLiteDatabase.OPEN_READONLY
            ).also { db = it }
        }
    }

    private const val COLS = "en, ar, country, lat, lon, tz, method"

    private fun read(row: android.database.Cursor): City = City(
        row.getString(0), row.getString(1), row.getString(2),
        row.getDouble(3), row.getDouble(4), row.getDouble(5), row.getString(6),
    )

    /**
     * Search-as-you-type: EN prefix / EN contains / AR contains, prefix hits
     * ranked first, then shortest name (v0 behaviour, now in SQL).
     * Leading-wildcard branches scan (~50-200 ms cold, fast warm) — the caller
     * is a suspend fun, so per-keystroke cancellation gives free debouncing.
     */
    suspend fun search(context: Context, query: String, limit: Int = 30): List<City> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext emptyList()
            val qLower = q.lowercase()
            val d = open(context)
            val out = ArrayList<City>(limit)
            d.rawQuery(
                """SELECT $COLS FROM cities
                   WHERE en_lower LIKE ?||'%' OR en_lower LIKE '%'||?||'%'
                      OR ar LIKE '%'||?||'%'
                   ORDER BY (en_lower LIKE ?||'%') DESC, LENGTH(en), en
                   LIMIT ?""",
                arrayOf(qLower, qLower, q, qLower, limit.toString())
            ).use { c ->
                while (c.moveToNext()) out.add(read(c))
            }
            out
        }

    /** The canonical Makkah row from the store (city #1 of the DB order). */
    suspend fun makkah(context: Context): City? = withContext(Dispatchers.IO) {
        open(context).rawQuery(
            "SELECT $COLS FROM cities WHERE en_lower = 'makkah' AND country = 'SA' LIMIT 1",
            null
        ).use { c -> if (c.moveToFirst()) read(c) else null }
    }
}
