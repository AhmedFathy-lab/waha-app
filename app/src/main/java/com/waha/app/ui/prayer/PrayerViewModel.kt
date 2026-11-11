package com.waha.app.ui.prayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waha.app.WahaApp
import com.waha.app.data.settings.PrayerSettings
import com.waha.app.domain.Prayer
import com.waha.app.domain.PrayerCalc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class PrayerUi(
    val settings: PrayerSettings? = null,
    val times: List<Pair<Prayer, Long>> = emptyList(),
    val next: Pair<Prayer, Long>? = null,
    val remainingMs: Long = 0,
    val error: String? = null,
)

class PrayerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = (app as WahaApp).prefs
    private val _ui = MutableStateFlow(PrayerUi())
    val ui: StateFlow<PrayerUi> = _ui

    private var cacheKey: String? = null
    private var cached: List<Pair<Prayer, Long>> = emptyList()
    private var next: Pair<Prayer, Long>? = null
    private var settings: PrayerSettings? = null

    init {
        viewModelScope.launch { prefs.prayerSettings.collect { settings = it; cacheKey = null; tick() } }
        viewModelScope.launch { while (true) { tick(); delay(1000L - System.currentTimeMillis() % 1000L) } }
    }

    private fun tick() {
        val s = settings ?: return
        val loc = s.location
        if (loc == null) { _ui.value = PrayerUi(settings = s); return }
        val now = System.currentTimeMillis()
        runCatching {
            val key = "${loc.lat},${loc.lon},${loc.tz},${s.method},${s.madhab},${LocalDate.now(ZoneId.of(loc.tz))}"
            if (key != cacheKey) { cached = PrayerCalc.today(loc, s.method, s.madhab); cacheKey = key; next = null }
            if (next == null || next!!.second <= now) next = PrayerCalc.next(loc, s.method, s.madhab, now)
            _ui.value = PrayerUi(s, cached, next, ((next?.second ?: now) - now).coerceAtLeast(0))
        }.onFailure { _ui.value = PrayerUi(settings = s, error = "تعذّر حساب المواقيت لهذا الموقع") }
    }
}
