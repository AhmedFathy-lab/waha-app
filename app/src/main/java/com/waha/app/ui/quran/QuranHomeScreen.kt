package com.waha.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waha.app.WahaApp
import com.waha.app.data.quran.Ayah
import com.waha.app.data.quran.Surah
import com.waha.app.data.settings.QuranPos
import com.waha.app.ui.theme.wahaBackground
import com.waha.app.util.arabic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHomeScreen(onBack: () -> Unit, onOpen: (surah: Int, ayah: Int) -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching { app.quran.surahs() }.onSuccess { surahs = it }.onFailure { error = "تعذّر تحميل المصحف" } }
    var tab by remember { mutableIntStateOf(0) }
    var showGoto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.background(wahaBackground()),
        topBar = {
            TopAppBar(
                title = { Text("القرآن الكريم") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                actions = { IconButton(onClick = { showGoto = true }) { Icon(Icons.Filled.Input, "اذهب إلى سورة وآية") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                listOf("السور", "الأجزاء", "البحث", "العلامات").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            when {
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!) }
                surahs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                tab == 0 -> SurahList(surahs, onOpen)
                tab == 1 -> JuzList(surahs, onOpen)
                tab == 2 -> SearchTab(surahs, onOpen)
                else -> BookmarksTab(surahs, onOpen)
            }
        }
    }
    if (showGoto && surahs.isNotEmpty()) GotoDialog(surahs, { showGoto = false }) { s, a -> showGoto = false; onOpen(s, a) }
}

@Composable
private fun SurahList(surahs: List<Surah>, onOpen: (Int, Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(surahs, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(s.id, 1) }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.id.arabic(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text("سورة ${s.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${s.ayahCount.arabic()} آية  •  الجزء ${s.firstJuz.arabic()}  •  صفحة ${s.firstPage.arabic()}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun JuzList(surahs: List<Surah>, onOpen: (Int, Int) -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    var starts by remember { mutableStateOf<List<Triple<Int, Int, Int>>>(emptyList()) }
    LaunchedEffect(Unit) {
        // أول آية في كل جزء، من بيانات الجزء المخزنة في المصدر
        val out = mutableListOf<Triple<Int, Int, Int>>()
        var seen = 0
        for (s in surahs) for (a in app.quran.ayat(s.id)) if (a.juz > seen) { out.add(Triple(a.juz, a.surah, a.ayah)); seen = a.juz }
        starts = out
    }
    if (starts.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(starts, key = { it.first }) { (juz, s, a) ->
            val name = surahs.first { it.id == s }.name
            Card(Modifier.fillMaxWidth().clickable { onOpen(s, a) }, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("الجزء ${juz.arabic()}", style = MaterialTheme.typography.titleMedium)
                    Text("يبدأ من سورة $name، آية ${a.arabic()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SearchTab(surahs: List<Surah>, onOpen: (Int, Int) -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    LaunchedEffect(q) {
        if (q.isBlank()) { results = emptyList(); searched = false; return@LaunchedEffect }
        kotlinx.coroutines.delay(250)
        results = app.quran.search(q); searched = true
    }
    Column {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(12.dp), singleLine = true,
            label = { Text("ابحث في القرآن (بدون تشكيل)") }, leadingIcon = { Icon(Icons.Filled.Search, null) })
        when {
            !searched -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("اكتب كلمة أو أكثر للبحث في كل الآيات") }
            results.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("لا توجد نتائج") }
            else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("${results.size.arabic()} نتيجة" + if (results.size >= 300) " (أول ٣٠٠)" else "", style = MaterialTheme.typography.labelMedium) }
                items(results, key = { it.surah * 1000 + it.ayah }) { a ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(a.surah, a.ayah) }, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(a.text, style = MaterialTheme.typography.bodyLarge)
                            Text("سورة ${surahs.first { it.id == a.surah }.name} • آية ${a.ayah.arabic()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksTab(surahs: List<Surah>, onOpen: (Int, Int) -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    val marks by app.prefs.bookmarks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    if (marks.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("لا توجد علامات مرجعية بعد. اضغط على آية أثناء القراءة لإضافتها.") }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(marks, key = { it.surah * 1000 + it.ayah }) { m: QuranPos ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(m.surah, m.ayah) }, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("سورة ${surahs.first { it.id == m.surah }.name} • آية ${m.ayah.arabic()}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { scope.launch { app.prefs.toggleBookmark(m.surah, m.ayah) } }) { Icon(Icons.Filled.Delete, "حذف العلامة") }
                }
            }
        }
    }
}

@Composable
private fun GotoDialog(surahs: List<Surah>, onDismiss: () -> Unit, onGo: (Int, Int) -> Unit) {
    var sel by remember { mutableStateOf(surahs.first()) }
    var expanded by remember { mutableStateOf(false) }
    var ayahText by remember { mutableStateOf("1") }
    val ayah = ayahText.toIntOrNull()
    val valid = ayah != null && ayah in 1..sel.ayahCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الانتقال إلى آية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth()) { Text("${sel.id.arabic()}. سورة ${sel.name}") }
                    DropdownMenu(expanded, { expanded = false }, Modifier.heightIn(max = 320.dp)) {
                        surahs.forEach { s -> DropdownMenuItem(text = { Text("${s.id.arabic()}. ${s.name}") }, onClick = { sel = s; expanded = false; ayahText = "1" }) }
                    }
                }
                OutlinedTextField(ayahText, { ayahText = it.filter(Char::isDigit).take(3) }, label = { Text("رقم الآية (١–${sel.ayahCount.arabic()})") },
                    singleLine = true, isError = !valid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onGo(sel.id, ayah!!) }) { Text("انتقال") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
