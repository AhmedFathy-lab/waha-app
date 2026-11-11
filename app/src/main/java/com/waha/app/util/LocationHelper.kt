package com.waha.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper

object LocationHelper {
    /** آخر موقع معروف، أو تحديث واحد جديد (مهلة 20 ثانية). يُستدعى بعد منح إذن الموقع. يرجع null عند الفشل. */
    @SuppressLint("MissingPermission")
    fun get(ctx: Context, onResult: (Location?) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null) { onResult(last); return }
        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
        if (provider == null) { onResult(null); return }
        val handler = Handler(Looper.getMainLooper())
        var done = false
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) { if (!done) { done = true; lm.removeUpdates(this); onResult(l) } }
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
            @Deprecated("Deprecated in Java") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
        }
        runCatching { lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }.onFailure { onResult(null); return }
        handler.postDelayed({ if (!done) { done = true; lm.removeUpdates(listener); onResult(null) } }, 20_000)
    }
}
