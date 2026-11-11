package com.waha.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import com.waha.app.domain.SavedLocation

private val Context.store by preferencesDataStore("waha_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class PrayerSettings(
    val location: SavedLocation?, val method: String, val madhab: String,
    val notifyEnabled: Boolean, val notifyPrayers: Set<String>,
)
data class QuranPos(val surah: Int, val ayah: Int)
data class TasbihEntry(val time: Long, val label: String, val count: Int, val goal: Int)

class Prefs(private val ctx: Context) {
    private object K {
        val THEME = stringPreferencesKey("theme")
        val FONT = floatPreferencesKey("quran_font_scale")
        val NIGHT = booleanPreferencesKey("quran_night")
        val LAST_S = intPreferencesKey("last_surah")
        val LAST_A = intPreferencesKey("last_ayah")
        val BOOKMARKS = stringSetPreferencesKey("quran_bookmarks")
        val T_COUNT = intPreferencesKey("tasbih_count")
        val T_GOAL = intPreferencesKey("tasbih_goal")
        val T_LABEL = stringPreferencesKey("tasbih_label")
        val T_HISTORY = stringPreferencesKey("tasbih_history")
        val LOC_NAME = stringPreferencesKey("loc_name")
        val LOC_LAT = doublePreferencesKey("loc_lat")
        val LOC_LON = doublePreferencesKey("loc_lon")
        val LOC_TZ = stringPreferencesKey("loc_tz")
        val LOC_GPS = booleanPreferencesKey("loc_gps")
        val P_METHOD = stringPreferencesKey("prayer_method")
        val P_MADHAB = stringPreferencesKey("prayer_madhab")
        val NOTIFY = booleanPreferencesKey("notify_enabled")
        val NOTIFY_SET = stringSetPreferencesKey("notify_prayers")
        val FAV_ADHKAR = stringSetPreferencesKey("fav_adhkar")
        val FAV_DUAS = stringSetPreferencesKey("fav_duas")
    }

    val theme: Flow<ThemeMode> = ctx.store.data.map { p -> runCatching { ThemeMode.valueOf(p[K.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM) }
    val quranFont: Flow<Float> = ctx.store.data.map { it[K.FONT] ?: 1f }
    val quranNight: Flow<Boolean> = ctx.store.data.map { it[K.NIGHT] ?: false }
    val lastPos: Flow<QuranPos?> = ctx.store.data.map { p ->
        val s = p[K.LAST_S]; val a = p[K.LAST_A]
        if (s != null && a != null) QuranPos(s, a) else null
    }
    val bookmarks: Flow<List<QuranPos>> = ctx.store.data.map { p ->
        (p[K.BOOKMARKS] ?: emptySet()).mapNotNull { v ->
            val x = v.split(':'); if (x.size == 2) QuranPos(x[0].toIntOrNull() ?: return@mapNotNull null, x[1].toIntOrNull() ?: return@mapNotNull null) else null
        }.sortedWith(compareBy({ it.surah }, { it.ayah }))
    }

    suspend fun setTheme(m: ThemeMode) { ctx.store.edit { it[K.THEME] = m.name } }
    suspend fun setQuranFont(v: Float) { ctx.store.edit { it[K.FONT] = v.coerceIn(0.8f, 2.2f) } }
    suspend fun setQuranNight(v: Boolean) { ctx.store.edit { it[K.NIGHT] = v } }
    suspend fun saveLastPos(s: Int, a: Int) { ctx.store.edit { it[K.LAST_S] = s; it[K.LAST_A] = a } }
    suspend fun toggleBookmark(s: Int, a: Int) {
        ctx.store.edit { p ->
            val cur = (p[K.BOOKMARKS] ?: emptySet()).toMutableSet()
            val key = "$s:$a"
            if (!cur.remove(key)) cur.add(key)
            p[K.BOOKMARKS] = cur
        }
    }

    val favAdhkar: Flow<Set<Int>> = ctx.store.data.map { p -> (p[K.FAV_ADHKAR] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet() }
    val favDuas: Flow<Set<Int>> = ctx.store.data.map { p -> (p[K.FAV_DUAS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet() }
    suspend fun toggleFavAdhkar(id: Int) = toggleSet(K.FAV_ADHKAR, id)
    suspend fun toggleFavDua(id: Int) = toggleSet(K.FAV_DUAS, id)
    private suspend fun toggleSet(key: Preferences.Key<Set<String>>, id: Int) {
        ctx.store.edit { p ->
            val cur = (p[key] ?: emptySet()).toMutableSet()
            if (!cur.remove(id.toString())) cur.add(id.toString())
            p[key] = cur
        }
    }

    // ---- المسبحة ----
    val tasbihCount: Flow<Int> = ctx.store.data.map { it[K.T_COUNT] ?: 0 }
    val tasbihGoal: Flow<Int> = ctx.store.data.map { it[K.T_GOAL] ?: 33 }
    val tasbihLabel: Flow<String> = ctx.store.data.map { it[K.T_LABEL] ?: "سبحان الله" }
    val tasbihHistory: Flow<List<TasbihEntry>> = ctx.store.data.map { p ->
        (p[K.T_HISTORY] ?: "").lines().mapNotNull { l ->
            val x = l.split('|')
            if (x.size == 4) TasbihEntry(x[0].toLongOrNull() ?: return@mapNotNull null, x[1], x[2].toIntOrNull() ?: return@mapNotNull null, x[3].toIntOrNull() ?: 0) else null
        }
    }
    suspend fun setTasbihCount(n: Int) { ctx.store.edit { it[K.T_COUNT] = n.coerceAtLeast(0) } }
    suspend fun setTasbihGoal(n: Int) { ctx.store.edit { it[K.T_GOAL] = n.coerceIn(1, 100000) } }
    suspend fun setTasbihLabel(l: String) { ctx.store.edit { it[K.T_LABEL] = l.replace('|', ' ').replace('\n', ' ').trim().ifEmpty { "سبحان الله" } } }
    /** يسجّل الجلسة الحالية في السجل (الأحدث أولاً، بحد أقصى 500 سجل) ثم يصفّر العدّاد. */
    suspend fun logAndResetTasbih() {
        ctx.store.edit { p ->
            val n = p[K.T_COUNT] ?: 0
            if (n > 0) {
                val line = "${System.currentTimeMillis()}|${(p[K.T_LABEL] ?: "سبحان الله").replace('|', ' ')}|$n|${p[K.T_GOAL] ?: 33}"
                p[K.T_HISTORY] = (listOf(line) + (p[K.T_HISTORY] ?: "").lines().filter { it.isNotBlank() }).take(500).joinToString("\n")
            }
            p[K.T_COUNT] = 0
        }
    }
    suspend fun clearTasbihHistory() { ctx.store.edit { it.remove(K.T_HISTORY) } }

    // ---- مواقيت الصلاة ----
    private val allFive = setOf("fajr", "dhuhr", "asr", "maghrib", "isha")
    val prayerSettings: Flow<PrayerSettings> = ctx.store.data.map { p ->
        val lat = p[K.LOC_LAT]; val lon = p[K.LOC_LON]; val tz = p[K.LOC_TZ]
        PrayerSettings(
            location = if (lat != null && lon != null && tz != null) SavedLocation(p[K.LOC_NAME] ?: "", lat, lon, tz, p[K.LOC_GPS] ?: false) else null,
            method = p[K.P_METHOD] ?: "MUSLIM_WORLD_LEAGUE", madhab = p[K.P_MADHAB] ?: "SHAFI",
            notifyEnabled = p[K.NOTIFY] ?: false, notifyPrayers = p[K.NOTIFY_SET] ?: allFive,
        )
    }
    suspend fun setLocation(l: SavedLocation, method: String? = null) {
        ctx.store.edit { it[K.LOC_NAME] = l.name; it[K.LOC_LAT] = l.lat; it[K.LOC_LON] = l.lon; it[K.LOC_TZ] = l.tz; it[K.LOC_GPS] = l.gps; if (method != null) it[K.P_METHOD] = method }
    }
    suspend fun setMethod(m: String) { ctx.store.edit { it[K.P_METHOD] = m } }
    suspend fun setMadhab(m: String) { ctx.store.edit { it[K.P_MADHAB] = m } }
    suspend fun setNotifyEnabled(v: Boolean) { ctx.store.edit { it[K.NOTIFY] = v } }
    suspend fun toggleNotifyPrayer(key: String) {
        ctx.store.edit { p -> val c = (p[K.NOTIFY_SET] ?: allFive).toMutableSet(); if (!c.remove(key)) c.add(key); p[K.NOTIFY_SET] = c }
    }

    // ---- إدارة المفضلة ----
    suspend fun clearFavAdhkar() { ctx.store.edit { it.remove(K.FAV_ADHKAR) } }
    suspend fun clearFavDuas() { ctx.store.edit { it.remove(K.FAV_DUAS) } }
    suspend fun clearBookmarks() { ctx.store.edit { it.remove(K.BOOKMARKS) } }

    // ---- نسخ احتياطي واستعادة ----
    private enum class T { S, I, F, D, B, SS }
    private val schema: Map<String, T> = mapOf(
        "theme" to T.S, "quran_font_scale" to T.F, "quran_night" to T.B, "last_surah" to T.I, "last_ayah" to T.I,
        "quran_bookmarks" to T.SS, "tasbih_count" to T.I, "tasbih_goal" to T.I, "tasbih_label" to T.S, "tasbih_history" to T.S,
        "loc_name" to T.S, "loc_lat" to T.D, "loc_lon" to T.D, "loc_tz" to T.S, "loc_gps" to T.B,
        "prayer_method" to T.S, "prayer_madhab" to T.S, "notify_enabled" to T.B, "notify_prayers" to T.SS,
        "fav_adhkar" to T.SS, "fav_duas" to T.SS,
    )

    suspend fun exportJson(): String {
        val data = JSONObject()
        for ((k, v) in ctx.store.data.first().asMap()) {
            if (k.name !in schema) continue
            @Suppress("UNCHECKED_CAST")
            when (v) { is Set<*> -> data.put(k.name, JSONArray(v.map { it.toString() })); else -> data.put(k.name, v) }
        }
        return JSONObject().put("app", "waha").put("version", 1).put("exportedAt", System.currentTimeMillis()).put("data", data).toString(2)
    }

    /** يستعيد النسخة بالكامل (يستبدل البيانات الحالية). يرجع null عند النجاح أو رسالة الخطأ دون أي تعديل عند الفشل. */
    suspend fun importJson(text: String): String? {
        val values = mutableMapOf<String, Any>()
        try {
            val root = JSONObject(text)
            if (root.optString("app") != "waha" || root.optInt("version", 0) != 1) return "الملف ليس نسخة احتياطية صالحة من واحة"
            val data = root.getJSONObject("data")
            for (name in data.keys()) {
                val type = schema[name] ?: continue
                val v: Any = when (type) {
                    T.S -> data.getString(name)
                    T.I -> data.getInt(name)
                    T.F -> data.getDouble(name).toFloat()
                    T.D -> data.getDouble(name)
                    T.B -> data.getBoolean(name)
                    T.SS -> data.getJSONArray(name).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                }
                values[name] = v
            }
        } catch (e: Exception) { return "تعذّرت قراءة الملف: النسخة تالفة أو غير مكتملة" }
        // تحقق من القيم قبل الكتابة
        fun bad(m: String) = "قيمة غير صالحة في النسخة ($m)"
        (values["theme"] as String?)?.let { t -> if (ThemeMode.entries.none { it.name == t }) return bad("theme") }
        (values["loc_lat"] as Double?)?.let { if (it !in -90.0..90.0) return bad("loc_lat") }
        (values["loc_lon"] as Double?)?.let { if (it !in -180.0..180.0) return bad("loc_lon") }
        (values["loc_tz"] as String?)?.let { if (runCatching { java.time.ZoneId.of(it) }.isFailure) return bad("loc_tz") }
        (values["prayer_method"] as String?)?.let { if (it !in com.waha.app.domain.Methods.labels) return bad("prayer_method") }
        (values["prayer_madhab"] as String?)?.let { if (it != "SHAFI" && it != "HANAFI") return bad("prayer_madhab") }
        (values["last_surah"] as Int?)?.let { if (it !in 1..114) return bad("last_surah") }
        (values["tasbih_goal"] as Int?)?.let { if (it !in 1..100000) return bad("tasbih_goal") }
        (values["tasbih_count"] as Int?)?.let { if (it < 0) return bad("tasbih_count") }
        (values["quran_font_scale"] as Float?)?.let { if (it !in 0.8f..2.2f) return bad("quran_font_scale") }
        if (("loc_lat" in values) != ("loc_lon" in values) || ("loc_lat" in values) != ("loc_tz" in values)) return bad("loc")
        ctx.store.edit { p ->
            for (k in p.asMap().keys.filter { it.name in schema }) p.remove(k)
            for ((name, v) in values) {
                @Suppress("UNCHECKED_CAST")
                when (schema[name]!!) {
                    T.S -> p[stringPreferencesKey(name)] = v as String
                    T.I -> p[intPreferencesKey(name)] = v as Int
                    T.F -> p[floatPreferencesKey(name)] = v as Float
                    T.D -> p[doublePreferencesKey(name)] = v as Double
                    T.B -> p[booleanPreferencesKey(name)] = v as Boolean
                    T.SS -> p[stringSetPreferencesKey(name)] = v as Set<String>
                }
            }
        }
        return null
    }
}
