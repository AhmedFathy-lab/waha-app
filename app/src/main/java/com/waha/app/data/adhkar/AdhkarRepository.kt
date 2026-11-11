package com.waha.app.data.adhkar

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.waha.app.data.db.AssetDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Dhikr(val id: Int, val categoryId: Int, val categoryTitle: String, val text: String, val count: Int)
data class DhikrCategory(val id: Int, val title: String, val size: Int)

enum class AdhkarSection(val key: String, val title: String) {
    MORNING("morning", "أذكار الصباح"),
    EVENING("evening", "أذكار المساء"),
    SLEEP("sleep", "أذكار النوم"),
    WAKING("waking", "أذكار الاستيقاظ"),
    AFTER_PRAYER("after_prayer", "أذكار بعد الصلاة"),
    MISC("misc", "أذكار متنوعة");
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } }
}

class AdhkarRepository(private val ctx: Context) {
    private val db: SQLiteDatabase by lazy { AssetDb.open(ctx, "adhkar.db", "1") }
    private val select = "SELECT d.id,d.category_id,c.title,d.text,d.count FROM dhikr d JOIN category c ON c.id=d.category_id"

    private fun Cursor.toDhikr() = Dhikr(getInt(0), getInt(1), getString(2), getString(3), getInt(4))

    suspend fun section(s: AdhkarSection): List<Dhikr> = withContext(Dispatchers.IO) {
        db.rawQuery("$select JOIN section_item si ON si.dhikr_id=d.id WHERE si.section=? ORDER BY si.ord", arrayOf(s.key)).use { c ->
            buildList { while (c.moveToNext()) add(c.toDhikr()) }
        }
    }

    suspend fun miscCategories(): List<DhikrCategory> = categories("misc")
    suspend fun duaCategories(): List<DhikrCategory> = categories("duas")
    suspend fun allDuas(): List<Dhikr> = withContext(Dispatchers.IO) {
        db.rawQuery("$select JOIN section_item si ON si.dhikr_id=d.id WHERE si.section='duas' ORDER BY d.id", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toDhikr()) }
        }
    }

    private suspend fun categories(section: String): List<DhikrCategory> = withContext(Dispatchers.IO) {
        db.rawQuery(
            "SELECT c.id,c.title,COUNT(*) FROM category c JOIN dhikr d ON d.category_id=c.id JOIN section_item si ON si.dhikr_id=d.id " +
                "WHERE si.section=? GROUP BY c.id ORDER BY c.id", arrayOf(section),
        ).use { c -> buildList { while (c.moveToNext()) add(DhikrCategory(c.getInt(0), c.getString(1), c.getInt(2))) } }
    }

    suspend fun category(id: Int): List<Dhikr> = withContext(Dispatchers.IO) {
        db.rawQuery("$select WHERE d.category_id=? ORDER BY d.id", arrayOf(id.toString())).use { c ->
            buildList { while (c.moveToNext()) add(c.toDhikr()) }
        }
    }

    suspend fun byIds(ids: Collection<Int>): List<Dhikr> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        db.rawQuery("$select WHERE d.id IN (${ids.joinToString(",")}) ORDER BY d.id", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toDhikr()) }
        }
    }
}
