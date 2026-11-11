package com.waha.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.waha.app.WahaApp
import com.waha.app.domain.PrayerCalc
import kotlinx.coroutines.flow.first

object PrayerScheduler {
    const val CHANNEL = "prayer_times"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_CITY = "city"
    private const val REQ = 4711

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "تنبيه عند دخول وقت الصلاة"
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun pending(ctx: Context, prayerKey: String?, city: String?, create: Boolean): PendingIntent? {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_PRAYER, prayerKey).putExtra(EXTRA_CITY, city)
        val flags = PendingIntent.FLAG_IMMUTABLE or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(ctx, REQ, i, flags)
    }

    fun cancel(ctx: Context) {
        val pi = pending(ctx, null, null, create = false) ?: return
        ctx.getSystemService(AlarmManager::class.java).cancel(pi); pi.cancel()
    }

    /** يجدولي تنبيهاً واحداً للصلاة القادمة المفعّلة؛ يُستدعى عند فتح التطبيق وتغيير الإعدادات وبعد كل تنبيه وبعد إعادة التشغيل. */
    suspend fun scheduleNext(ctx: Context) {
        val app = ctx.applicationContext as WahaApp
        val s = app.prefs.prayerSettings.first()
        cancel(ctx)
        val loc = s.location ?: return
        if (!s.notifyEnabled || s.notifyPrayers.isEmpty()) return
        val next = runCatching { PrayerCalc.next(loc, s.method, s.madhab, System.currentTimeMillis() + 1000, s.notifyPrayers) }.getOrNull() ?: return
        val pi = pending(ctx, next.first.key, loc.name, create = true)!!
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second, pi)
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second, pi)
    }
}
