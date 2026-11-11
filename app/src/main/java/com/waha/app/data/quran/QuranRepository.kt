package com.waha.app.data.quran

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.waha.app.data.db.AssetDb
import com.waha.app.util.normalizeArabic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** قراءة فقط من quran.db المبني من ARQURAN.TXT (يُنسخ من assets عند أول تشغيل أو عند تغيّر الإصدار). */
class QuranRepository(private val ctx: Context) {
    private val db: SQLiteDatabase by lazy { open() }

    private fun open(): SQLiteDatabase = AssetDb.open(ctx, DB_NAME, DB_VERSION)

    suspend fun surahs(): List<Surah> = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT id,name,ayah_count,basmala,first_page,first_juz FROM surah ORDER BY id", null).use { c ->
            buildList { while (c.moveToNext()) add(Surah(c.getInt(0), c.getString(1), c.getInt(2), c.getString(3), c.getInt(4), c.getInt(5))) }
        }
    }

    suspend fun surah(id: Int): Surah? = surahs().firstOrNull { it.id == id }

    suspend fun ayat(surah: Int): List<Ayah> = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT surah,ayah,text,juz,page,sajda FROM ayah WHERE surah=? ORDER BY ayah", arrayOf(surah.toString())).use { c ->
            buildList { while (c.moveToNext()) add(c.toAyah()) }
        }
    }

    /** بحث في النص المطبّع: كل كلمات الاستعلام يجب أن تظهر في الآية. */
    suspend fun search(query: String, limit: Int = 300): List<Ayah> = withContext(Dispatchers.IO) {
        val words = normalizeArabic(query).split(' ', '\t').filter { it.isNotBlank() }
        if (words.isEmpty()) return@withContext emptyList()
        val where = words.joinToString(" AND ") { "text_norm LIKE ? ESCAPE '\\'" }
        val args = words.map { "%" + it.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%" }.toTypedArray()
        db.rawQuery("SELECT surah,ayah,text,juz,page,sajda FROM ayah WHERE $where ORDER BY id LIMIT $limit", args).use { c ->
            buildList { while (c.moveToNext()) add(c.toAyah()) }
        }
    }

    private fun android.database.Cursor.toAyah() = Ayah(getInt(0), getInt(1), getString(2), getInt(3), getInt(4), getInt(5) == 1)

    companion object {
        const val DB_NAME = "quran.db"
        const val DB_VERSION = "1"
    }
}
