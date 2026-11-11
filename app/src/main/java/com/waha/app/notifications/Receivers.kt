package com.waha.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.waha.app.MainActivity
import com.waha.app.domain.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val key = intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER)
                val prayer = Prayer.entries.firstOrNull { it.key == key }
                val allowed = Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (prayer != null && allowed) {
                    val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                    val city = intent.getStringExtra(PrayerScheduler.EXTRA_CITY).orEmpty()
                    val n = NotificationCompat.Builder(ctx, PrayerScheduler.CHANNEL)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("حان الآن وقت صلاة ${prayer.title}")
                        .setContentText(if (city.isNotEmpty()) "بحسب مواقيت $city" else "واحة")
                        .setContentIntent(open).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
                    ctx.getSystemService(NotificationManager::class.java).notify(prayer.ordinal + 100, n)
                }
                PrayerScheduler.scheduleNext(ctx)
            } finally { pr.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch { try { PrayerScheduler.scheduleNext(ctx) } finally { pr.finish() } }
    }
}
