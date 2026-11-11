package com.waha.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** يفتح قاعدة للقراءة فقط منسوخة من assets، وينسخها من جديد عند تغيّر رقم الإصدار. */
object AssetDb {
    fun open(ctx: Context, name: String, version: String): SQLiteDatabase {
        val f = ctx.getDatabasePath(name)
        val marker = File(f.parentFile, "$name.ver")
        if (!f.exists() || runCatching { marker.readText() }.getOrNull() != version) {
            f.parentFile?.mkdirs()
            ctx.assets.open(name).use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            marker.writeText(version)
        }
        return SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
    }
}
