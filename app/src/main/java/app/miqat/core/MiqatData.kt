package app.miqat.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The companion data asset (`assets/miqat-data-v1.db`, built by
 * tools/build_miqat_assets.py from the donor DB): the Arabic country names, the
 * Islamic days worth knowing, the donor's own reminder texts (with their time
 * windows), and the timezone rules that make the UTC offset DST-correct for any
 * date — the S2 "own tz table" item.
 *
 * Opened read-only, copy-on-first-open, exactly like the city store.
 */
object MiqatData {

    private const val ASSET = "miqat-data-v1.db"

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
                    tmp.copyTo(file, overwrite = true)
                    tmp.delete()
                }
            }
            return SQLiteDatabase.openDatabase(
                file.path, null, SQLiteDatabase.OPEN_READONLY
            ).also { db = it }
        }
    }

    /** The Arabic name of a country, or its code when unknown. */
    fun countryNameAr(context: Context, code: String): String {
        if (code.isBlank()) return ""
        return try {
            open(context).rawQuery(
                "SELECT name_ar FROM countries WHERE code = ? LIMIT 1", arrayOf(code),
            ).use { c -> if (c.moveToFirst()) c.getString(0) else code }
        } catch (e: Exception) {
            code
        }
    }

    /** The Islamic days of a Hijri month — the calendar's events. */
    fun dayEvents(context: Context, hijriMonth: Int): List<DayEvent> = try {
        open(context).rawQuery(
            "SELECT id, month, start_day, end_day, name_ar FROM day_events"
                + " WHERE month = ? ORDER BY start_day",
            arrayOf(hijriMonth.toString()),
        ).use { c ->
            val out = ArrayList<DayEvent>()
            while (c.moveToNext()) {
                out.add(DayEvent(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getString(4)))
            }
            out
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** The donor's reminder texts for a prayer slot (0..5) — the content of
     *  the تذكيرية notification. */
    fun prayerEvents(context: Context, prayerIndex: Int): List<ReminderText> = try {
        open(context).rawQuery(
            "SELECT id, prayer_index, start_min, end_min, message_ar, priority"
                + " FROM prayer_events WHERE prayer_index = ? ORDER BY priority DESC, id",
            arrayOf(prayerIndex.toString()),
        ).use { c ->
            val out = ArrayList<ReminderText>()
            while (c.moveToNext()) {
                out.add(
                    ReminderText(
                        c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getString(4), c.getInt(5),
                    )
                )
            }
            out
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** The donor's integer key for an IANA zone name (e.g. Asia/Riyadh → 327). */
    fun zoneIdFor(context: Context, zoneName: String): Int? {
        if (zoneName.isBlank()) return null
        return try {
            open(context).rawQuery(
                "SELECT id FROM zones WHERE name = ? LIMIT 1", arrayOf(zoneName),
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The UTC offset (hours) in force for a zone at a given instant — DST
     * included. The donor's table stores one row per transition, with the
     * offset that applies FROM that moment.
     */
    fun utcOffsetHours(context: Context, zoneId: Int, epochMillis: Long): Double? = try {
        open(context).rawQuery(
            "SELECT gmt_offset FROM tz_rules WHERE zone_id = ? AND start_epoch <= ?"
                + " ORDER BY start_epoch DESC LIMIT 1",
            arrayOf(zoneId.toString(), epochMillis.toString()),
        ).use { c ->
            if (c.moveToFirst()) c.getInt(0) / 3600.0 else null
        }
    } catch (e: Exception) {
        null
    }

    /** Is daylight saving in force for that zone at that instant? */
    fun isDst(context: Context, zoneId: Int, epochMillis: Long): Boolean = try {
        open(context).rawQuery(
            "SELECT dst FROM tz_rules WHERE zone_id = ? AND start_epoch <= ?"
                + " ORDER BY start_epoch DESC LIMIT 1",
            arrayOf(zoneId.toString(), epochMillis.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) == 1 else false }
    } catch (e: Exception) {
        false
    }
}

/** An Islamic day worth knowing (from the donor's day_events). */
data class DayEvent(
    val id: Int,
    val month: Int,
    val startDay: Int,
    val endDay: Int,
    val nameAr: String,
)

/** One of the donor's reminder texts, with its window around the prayer. */
data class ReminderText(
    val id: Int,
    val prayerIndex: Int,
    /** Minutes relative to the prayer time (negative = before). */
    val startMinutes: Int,
    val endMinutes: Int,
    val messageAr: String,
    val priority: Int,
)
