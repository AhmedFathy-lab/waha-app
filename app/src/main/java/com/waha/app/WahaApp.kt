package com.waha.app

import android.app.Application
import com.waha.app.data.quran.QuranRepository
import com.waha.app.data.settings.Prefs

class WahaApp : Application() {
    lateinit var prefs: Prefs
    lateinit var quran: QuranRepository
    lateinit var adhkar: com.waha.app.data.adhkar.AdhkarRepository
    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        com.waha.app.notifications.PrayerScheduler.createChannel(this)
        quran = QuranRepository(this)
        adhkar = com.waha.app.data.adhkar.AdhkarRepository(this)
    }
}
