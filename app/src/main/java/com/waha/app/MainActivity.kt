package com.waha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.waha.app.data.settings.ThemeMode
import com.waha.app.ui.navigation.WahaNav
import com.waha.app.ui.theme.WahaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { runCatching { com.waha.app.notifications.PrayerScheduler.scheduleNext(this@MainActivity) } }
        val prefs = (application as WahaApp).prefs
        setContent {
            val mode by prefs.theme.collectAsState(initial = ThemeMode.SYSTEM)
            WahaTheme(dark = when (mode) { ThemeMode.SYSTEM -> null; ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }) { WahaNav() }
        }
    }
}
