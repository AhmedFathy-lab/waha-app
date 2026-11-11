package com.waha.app.ui.prayer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waha.app.WahaApp
import com.waha.app.data.prayer.Cities
import com.waha.app.domain.*
import com.waha.app.notifications.PrayerScheduler
import com.waha.app.ui.theme.wahaBackground
import com.waha.app.util.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

fun timeText(ms: Long, tz: String): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.of(tz))
    return "${clockText(z.hour, z.minute)} ${ampmText(z.hour)}"
}

fun countdownText(ms: Long): String {
    val s = ms / 1000
    val h = (s / 3600).toInt(); val m = ((s % 3600) / 60).toInt(); val sec = (s % 60).toInt()
    return "${h.arabic()}:${m.toString().padStart(2, '0').arabicDigits()}:${sec.toString().padStart(2, '0').arabicDigits()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(onBack: () -> Unit, vm: PrayerViewModel = viewModel()) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    val ui by vm.ui.collectAsState()
    val s = ui.settings
    var showCities by remember { mutableStateOf(false) }
    var methodMenu by remember { mutableStateOf(false) }
    var gpsBusy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(msg) { msg?.let { snack.showSnackbar(it); msg = null } }

    fun reschedule() { scope.launch { PrayerScheduler.scheduleNext(ctx) } }

    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) {
            gpsBusy = true
            LocationHelper.get(ctx) { l ->
                gpsBusy = false
                if (l == null) msg = "تعذّر تحديد موقعك. تأكد من تشغيل خدمة الموقع أو اختر مدينتك يدوياً."
                else scope.launch {
                    app.prefs.setLocation(SavedLocation("موقعي الحالي", l.latitude, l.longitude, ZoneId.systemDefault().id, gps = true))
                    PrayerScheduler.scheduleNext(ctx)
                }
            }
        } else msg = "لم يُمنح إذن الموقع. يمكنك اختيار مدينتك يدوياً."
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) scope.launch { app.prefs.setNotifyEnabled(true); PrayerScheduler.scheduleNext(ctx) }
        else msg = "لم يُمنح إذن الإشعارات، فلن تظهر التنبيهات."
    }

    Scaffold(
        containerColor = Color.Transparent, modifier = Modifier.background(wahaBackground()),
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(title = { Text("مواقيت الصلاة") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        },
    ) { pad ->
        if (s == null) { Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        val loc = s.location
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loc == null) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Text("حدّد مدينتك أو موقعك لعرض مواقيت الصلاة. لا تُعرض أي مواقيت قبل تحديد الموقع.", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ui.next?.let { (p, t) ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الصلاة القادمة: ${p.title}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(countdownText(ui.remainingMs), color = Color.White, fontSize = 44.sp)
                            Text("الساعة ${timeText(t, loc.tz)}", color = Color.White.copy(alpha = .85f))
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        ui.times.forEach { (p, t) ->
                            val isNext = ui.next?.first == p
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(44.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(p.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (isNext) MaterialTheme.colorScheme.primary else Color.Unspecified)
                                Text(timeText(t, loc.tz), style = MaterialTheme.typography.titleMedium)
                                if (p.isPrayer) {
                                    val on = s.notifyEnabled && p.key in s.notifyPrayers
                                    IconButton(onClick = { scope.launch { app.prefs.toggleNotifyPrayer(p.key); PrayerScheduler.scheduleNext(ctx) } }, enabled = s.notifyEnabled) {
                                        Icon(if (on) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff, "تنبيه ${p.title}")
                                    }
                                } else Spacer(Modifier.width(48.dp))
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الموقع: ${loc?.let { if (it.gps) "${it.name} (${"%.2f".format(it.lat).arabicDigits()}، ${"%.2f".format(it.lon).arabicDigits()})" else it.name } ?: "غير محدد"}", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCities = true }, Modifier.weight(1f)) { Icon(Icons.Filled.LocationCity, null); Spacer(Modifier.width(6.dp)); Text("اختر مدينة") }
                        OutlinedButton(enabled = !gpsBusy, onClick = { locPerm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }, modifier = Modifier.weight(1f)) {
                            if (gpsBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.MyLocation, null)
                            Spacer(Modifier.width(6.dp)); Text("موقعي")
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("طريقة الحساب", style = MaterialTheme.typography.titleMedium)
                    Box {
                        OutlinedButton(onClick = { methodMenu = true }, Modifier.fillMaxWidth()) { Text(Methods.labels[s.method] ?: s.method) }
                        DropdownMenu(methodMenu, { methodMenu = false }) {
                            Methods.labels.forEach { (k, v) ->
                                DropdownMenuItem(text = { Text(v) }, onClick = { methodMenu = false; scope.launch { app.prefs.setMethod(k); PrayerScheduler.scheduleNext(ctx) } })
                            }
                        }
                    }
                    Text("حساب العصر", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(s.madhab == "SHAFI", { scope.launch { app.prefs.setMadhab("SHAFI"); PrayerScheduler.scheduleNext(ctx) } }, { Text("الجمهور (ظل المثل)") })
                        FilterChip(s.madhab == "HANAFI", { scope.launch { app.prefs.setMadhab("HANAFI"); PrayerScheduler.scheduleNext(ctx) } }, { Text("الحنفي (ظل المثلين)") })
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تنبيه عند دخول الوقت", style = MaterialTheme.typography.titleMedium)
                        Text("إشعار نصي فقط (بلا صوت أذان)، ويمكن تخصيص كل صلاة من الأجراس أعلاه.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = s.notifyEnabled, onCheckedChange = { on ->
                        if (!on) scope.launch { app.prefs.setNotifyEnabled(false); PrayerScheduler.scheduleNext(ctx) }
                        else if (loc == null) msg = "حدّد موقعك أولاً"
                        else if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else scope.launch { app.prefs.setNotifyEnabled(true); PrayerScheduler.scheduleNext(ctx) }
                    })
                }
            }
        }
    }

    if (showCities) {
        var q by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCities = false }, title = { Text("اختر مدينة") },
            text = {
                Column {
                    OutlinedTextField(q, { q = it }, singleLine = true, label = { Text("بحث") }, modifier = Modifier.fillMaxWidth())
                    val shown = Cities.all.filter { q.isBlank() || normalizeArabic(it.name + it.country).contains(normalizeArabic(q.trim())) }
                    LazyColumn(Modifier.heightIn(max = 340.dp)) {
                        items(shown, key = { it.name + it.country }) { c ->
                            ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.country) }, modifier = Modifier.clickable {
                                showCities = false
                                scope.launch { app.prefs.setLocation(SavedLocation(c.name, c.lat, c.lon, c.tz), c.method); PrayerScheduler.scheduleNext(ctx) }
                            })
                        }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showCities = false }) { Text("إغلاق") } },
        )
    }
}
