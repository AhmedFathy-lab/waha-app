package com.waha.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waha.app.ui.theme.*

/** كل بطاقة تستقبل إجراء التنقل الخاص بها؛ تُربط الوجهات في NavHost كلما اكتملت ميزتها. */
@Composable
fun HomeScreen(
    resumeText: String?, onResume: () -> Unit,
    onQuran: () -> Unit, onAdhkar: () -> Unit, onTasbih: () -> Unit,
    onDuas: () -> Unit, onSettings: () -> Unit, onPrayer: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val c by vm.clock.collectAsState()
    Box(Modifier.fillMaxSize().background(wahaBackground())) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("واحة", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "الإعدادات", tint = MaterialTheme.colorScheme.primary) }
            }
            ClockCard(c)
            PrayerCard(onPrayer)
            QuranCard(onQuran)
            if (resumeText != null) ResumeCard(resumeText, onResume)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile("أذكار الصباح والمساء", Icons.Filled.WbSunny, onAdhkar, Modifier.weight(1f))
                FeatureTile("المسبحة", Icons.Filled.TouchApp, onTasbih, Modifier.weight(1f))
            }
            FeatureTile("أدعية", Icons.Filled.FrontHand, onDuas, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClockCard(c: ClockState) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(c.time, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(c.ampm, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            }
            Text("${c.weekday}  •  ${c.hijri}", style = MaterialTheme.typography.titleMedium)
            Text(c.gregorian, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
        }
    }
}

@Composable
private fun PrayerCard(onPrayer: () -> Unit, vm: com.waha.app.ui.prayer.PrayerViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val loc = ui.settings?.location
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPrayer),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mosque, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("مواقيت الصلاة", style = MaterialTheme.typography.titleMedium)
                    if (loc == null) Text("اضغط لاختيار المدينة أو تحديد موقعك", style = MaterialTheme.typography.bodyMedium)
                    else Text(loc.name, style = MaterialTheme.typography.bodyMedium)
                }
                ui.next?.let { (p, _) ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(p.title, style = MaterialTheme.typography.labelMedium)
                        Text(com.waha.app.ui.prayer.countdownText(ui.remainingMs), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (loc != null && ui.times.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ui.times.filter { it.first.isPrayer }.forEach { (p, t) ->
                        val isNext = ui.next?.first == p
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.title, style = MaterialTheme.typography.labelMedium, color = if (isNext) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Unspecified, fontWeight = if (isNext) FontWeight.Bold else null)
                            Text(com.waha.app.ui.prayer.timeText(t, loc.tz).substringBefore(' '), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranCard(onQuran: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip28().background(Brush3()).clickable(onClick = onQuran).padding(24.dp),
    ) {
        Column {
            Icon(Icons.Filled.MenuBook, null, tint = White, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("القرآن الكريم", color = White, style = MaterialTheme.typography.titleLarge)
            Text("السور والبحث والعلامات المرجعية", color = White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FeatureTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun Modifier.clip28() = this.then(Modifier.clip(RoundedCornerShape(28.dp)))
private fun Brush3() = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Emerald, EmeraldDark))

@Composable
private fun ResumeCard(text: String, onResume: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onResume),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("متابعة القراءة", style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
