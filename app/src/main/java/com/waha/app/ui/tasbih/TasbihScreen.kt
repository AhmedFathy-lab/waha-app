package com.waha.app.ui.tasbih

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waha.app.WahaApp
import com.waha.app.data.settings.TasbihEntry
import com.waha.app.ui.theme.wahaBackground
import com.waha.app.util.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private val PRESETS = listOf("سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله", "أستغفر الله", "اللهم صل وسلم على نبينا محمد")
private val GOALS = listOf(33, 99, 100, 1000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(onBack: () -> Unit, onHistory: () -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val count by app.prefs.tasbihCount.collectAsState(initial = 0)
    val goal by app.prefs.tasbihGoal.collectAsState(initial = 33)
    val label by app.prefs.tasbihLabel.collectAsState(initial = "سبحان الله")
    var confirmReset by remember { mutableStateOf(false) }
    var customGoal by remember { mutableStateOf(false) }
    var customLabel by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent, modifier = Modifier.background(wahaBackground()),
        topBar = {
            TopAppBar(
                title = { Text("المسبحة") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                actions = { IconButton(onClick = onHistory) { Icon(Icons.Filled.History, "السجل") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            val reached = count >= goal
            Box(
                Modifier.size(250.dp).clip(CircleShape)
                    .background(if (reached) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                    .clickable {
                        val n = count + 1
                        scope.launch { app.prefs.setTasbihCount(n) }
                        haptic.performHapticFeedback(if (n == goal) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(count.arabic(), color = Color.White, fontSize = 72.sp)
                    Text(if (reached) "اكتمل الهدف ✓" else "الهدف ${goal.arabic()}", color = Color.White.copy(alpha = .9f))
                }
            }
            LinearProgressIndicator(progress = { (count.toFloat() / goal).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            Button(onClick = { scope.launch { app.prefs.setTasbihCount(count + 1) } }, Modifier.fillMaxWidth().height(56.dp)) { Text("سبّح", fontSize = 18.sp) }
            OutlinedButton(onClick = { confirmReset = true }, enabled = count > 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("إعادة ضبط")
            }

            Text("الذكر", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            FlowRowChips(PRESETS, selected = label, extra = "ذكر مخصص", onPick = { scope.launch { app.prefs.setTasbihLabel(it) } }, onExtra = { customLabel = true })
            Text("الهدف", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            FlowRowChips(GOALS.map { it.arabic() }, selected = goal.arabic(), extra = "هدف مخصص", onPick = { picked -> scope.launch { app.prefs.setTasbihGoal(GOALS[GOALS.map { it.arabic() }.indexOf(picked)]) } }, onExtra = { customGoal = true })
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("إعادة ضبط العدّاد؟") },
        text = { Text("سيُسجَّل العدد الحالي (${count.arabic()}) في السجل ثم يعود العدّاد إلى الصفر.") },
        confirmButton = { TextButton(onClick = { scope.launch { app.prefs.logAndResetTasbih() }; confirmReset = false }) { Text("تأكيد") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } },
    )
    if (customGoal) {
        var t by remember { mutableStateOf(goal.toString()) }
        val n = t.toIntOrNull()
        AlertDialog(
            onDismissRequest = { customGoal = false }, title = { Text("هدف مخصص") },
            text = { OutlinedTextField(t, { t = it.filter(Char::isDigit).take(6) }, singleLine = true, isError = n == null || n < 1, label = { Text("العدد") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = { TextButton(enabled = n != null && n >= 1, onClick = { scope.launch { app.prefs.setTasbihGoal(n!!) }; customGoal = false }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { customGoal = false }) { Text("إلغاء") } },
        )
    }
    if (customLabel) {
        var t by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { customLabel = false }, title = { Text("ذكر مخصص") },
            text = { OutlinedTextField(t, { t = it.take(60) }, singleLine = true, label = { Text("نص الذكر") }) },
            confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { scope.launch { app.prefs.setTasbihLabel(t) }; customLabel = false }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { customLabel = false }) { Text("إلغاء") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FlowRowChips(items: List<String>, selected: String, extra: String, onPick: (String) -> Unit, onExtra: () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { FilterChip(selected = it == selected, onClick = { onPick(it) }, label = { Text(it) }) }
        AssistChip(onClick = onExtra, label = { Text(extra) }, leadingIcon = { Icon(Icons.Filled.Edit, null, Modifier.size(16.dp)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihHistoryScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    val history by app.prefs.tasbihHistory.collectAsState(initial = emptyList())
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent, modifier = Modifier.background(wahaBackground()),
        topBar = {
            TopAppBar(
                title = { Text("سجل التسبيح") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                actions = { if (history.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.DeleteSweep, "مسح السجل") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        if (history.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("لا يوجد سجل بعد. تُسجَّل الجلسة عند إعادة ضبط العدّاد.", textAlign = TextAlign.Center)
            }
        } else {
            val zone = ZoneId.systemDefault()
            val today = java.time.LocalDate.now(zone)
            val todayTotal = history.filter { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() == today }.sumOf { it.count }
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("مجموع اليوم: ${todayTotal.arabic()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                items(history, key = { it.time }) { e: TasbihEntry ->
                    val dt = Instant.ofEpochMilli(e.time).atZone(zone)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.label, style = MaterialTheme.typography.titleMedium)
                                Text("${gregorianText(dt.toLocalDate())} • ${clockText(dt.hour, dt.minute)} ${ampmText(dt.hour)}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(e.count.arabic(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false }, title = { Text("مسح السجل؟") }, text = { Text("سيُحذف كل سجل التسبيح ولا يمكن التراجع.") },
        confirmButton = { TextButton(onClick = { scope.launch { app.prefs.clearTasbihHistory() }; confirmClear = false }) { Text("مسح") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } },
    )
}
