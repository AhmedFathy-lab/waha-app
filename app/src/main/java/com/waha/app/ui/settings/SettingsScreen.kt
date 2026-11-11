package com.waha.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waha.app.WahaApp
import com.waha.app.data.settings.ThemeMode
import com.waha.app.domain.Methods
import com.waha.app.notifications.PrayerScheduler
import com.waha.app.ui.theme.wahaBackground
import com.waha.app.util.arabic
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, onPrayer: () -> Unit, onAdhkarFavs: () -> Unit, onDuaFavs: () -> Unit, onBookmarks: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    val theme by app.prefs.theme.collectAsState(initial = ThemeMode.SYSTEM)
    val font by app.prefs.quranFont.collectAsState(initial = 1f)
    val night by app.prefs.quranNight.collectAsState(initial = false)
    val prayer by app.prefs.prayerSettings.collectAsState(initial = null)
    val favA by app.prefs.favAdhkar.collectAsState(initial = emptySet())
    val favD by app.prefs.favDuas.collectAsState(initial = emptySet())
    val marks by app.prefs.bookmarks.collectAsState(initial = emptyList())
    val snack = remember { SnackbarHostState() }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = app.prefs.exportJson()
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }.isSuccess
            snack.showSnackbar(if (ok) "تم حفظ النسخة الاحتياطية" else "تعذّر حفظ الملف")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            if (text == null || text.length > 5_000_000) snack.showSnackbar("تعذّرت قراءة الملف") else pendingImport = text
        }
    }

    @Composable fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); content()
            }
        }
    }
    @Composable fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, sub: String? = null, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(text, style = MaterialTheme.typography.titleMedium); if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium) }
            if (onClear != null) IconButton(onClick = onClear) { Icon(Icons.Filled.Delete, "حذف الكل") }
        }
    }

    Scaffold(
        containerColor = Color.Transparent, modifier = Modifier.background(wahaBackground()), snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(title = { Text("الإعدادات") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Section("المظهر") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to "تلقائي", ThemeMode.LIGHT to "فاتح", ThemeMode.DARK to "داكن").forEach { (m, t) ->
                        FilterChip(theme == m, { scope.launch { app.prefs.setTheme(m) } }, { Text(t) })
                    }
                }
                Text("حجم الخط (القرآن والأذكار والأدعية): ${(font * 100).toInt().arabic()}٪")
                Slider(font, { scope.launch { app.prefs.setQuranFont(it) } }, valueRange = 0.8f..2.2f)
                Text("معاينة: بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", fontSize = (20 * font).sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("وضع القراءة الليلية في المصحف", Modifier.weight(1f))
                    Switch(night, { scope.launch { app.prefs.setQuranNight(it) } })
                }
            }
            Section("مواقيت الصلاة والتنبيهات") {
                val p = prayer
                LinkRow(Icons.Filled.Mosque, "إعدادات المواقيت",
                    if (p?.location == null) "لم يُحدَّد موقع بعد" else "${p.location.name} • ${Methods.labels[p.method] ?: p.method}", onPrayer)
                LinkRow(Icons.Filled.Notifications, "التنبيهات", if (p?.notifyEnabled == true) "مفعّلة" else "متوقفة (تُضبط من صفحة المواقيت)", onPrayer)
            }
            Section("إدارة المفضلة والعلامات") {
                LinkRow(Icons.Filled.Favorite, "أذكار مفضلة", "${favA.size.arabic()} ذكر", onAdhkarFavs,
                    onClear = if (favA.isNotEmpty()) ({ confirm = "حذف كل الأذكار المفضلة؟" to { scope.launch { app.prefs.clearFavAdhkar() } } }) else null)
                LinkRow(Icons.Filled.Favorite, "أدعية مفضلة", "${favD.size.arabic()} دعاء", onDuaFavs,
                    onClear = if (favD.isNotEmpty()) ({ confirm = "حذف كل الأدعية المفضلة؟" to { scope.launch { app.prefs.clearFavDuas() } } }) else null)
                LinkRow(Icons.Filled.Bookmark, "علامات القرآن", "${marks.size.arabic()} علامة", onBookmarks,
                    onClear = if (marks.isNotEmpty()) ({ confirm = "حذف كل العلامات المرجعية؟" to { scope.launch { app.prefs.clearBookmarks() } } }) else null)
            }
            Section("حفظ واستعادة التقدم") {
                Text("تشمل النسخة: آخر موضع قراءة والعلامات والمفضلة والمسبحة وسجلها والإعدادات والموقع المحفوظ. تُحفظ في ملف تختاره أنت.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("waha-backup-${LocalDate.now()}.json") }, Modifier.weight(1f)) { Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(6.dp)); Text("حفظ نسخة") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.weight(1f)) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text("استعادة") }
                }
            }
            Section("عن واحة") {
                Text("الإصدار 1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text("نص القرآن: ملف ARQURAN.TXT (رواية حفص) المرفق مع المشروع، دون تعديل على حروفه.", style = MaterialTheme.typography.bodyMedium)
                Text("الأذكار والأدعية: نصوص حصن المسلم كما وردت في الملف المرفق.", style = MaterialTheme.typography.bodyMedium)
                Text("مواقيت الصلاة: مكتبة Adhan (batoulapps).", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    confirm?.let { (msg, action) ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(msg) }, text = { Text("لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = { TextButton(onClick = { action(); confirm = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("إلغاء") } })
    }
    pendingImport?.let { text ->
        AlertDialog(onDismissRequest = { pendingImport = null }, title = { Text("استعادة النسخة؟" ) },
            text = { Text("ستُستبدل بياناتك الحالية (المفضلة والعلامات والمسبحة والإعدادات) بما في النسخة.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val err = app.prefs.importJson(text)
                        if (err == null) { PrayerScheduler.scheduleNext(ctx); snack.showSnackbar("تمت استعادة النسخة") } else snack.showSnackbar(err)
                    }
                }) { Text("استعادة") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("إلغاء") } })
    }
}
