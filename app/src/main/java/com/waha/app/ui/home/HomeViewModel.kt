package com.waha.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waha.app.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ClockState(
    val time: String, val ampm: String, val weekday: String,
    val hijri: String, val gregorian: String,
)

class HomeViewModel : ViewModel() {
    private val _clock = MutableStateFlow(now())
    val clock: StateFlow<ClockState> = _clock

    init {
        viewModelScope.launch {
            while (true) {
                _clock.value = now()
                delay(1000L - System.currentTimeMillis() % 1000L)
            }
        }
    }

    private fun now(): ClockState {
        val t = LocalDateTime.now()
        return ClockState(
            time = clockText(t.hour, t.minute), ampm = ampmText(t.hour),
            weekday = weekdayText(t.toLocalDate()),
            hijri = hijriText(t.toLocalDate()), gregorian = gregorianText(t.toLocalDate()),
        )
    }
}
