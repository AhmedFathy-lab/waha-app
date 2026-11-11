package com.waha.app.ui.adhkar

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waha.app.WahaApp
import com.waha.app.data.adhkar.*
import com.waha.app.ui.theme.wahaBackground
import com.waha.app.util.arabic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(wahaBackground()),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        content = content,
    )
}

@Composable
fun AdhkarHomeScreen(onBack: () -> Unit, onSection: (String) -> Unit, onFavorites: () -> Unit) {
    val items = listOf(
        Triple(AdhkarSection.MORNING, Icons.Filled.WbSunny, "morning"),
        Triple(AdhkarSection.EVENING, Icons.Filled.NightsStay, "evening"),
        Triple(AdhkarSection.SLEEP, Icons.Filled.Bedtime, "sleep"),
        Triple(AdhkarSection.WAKING, Icons.Filled.Alarm, "waking"),
        Triple(AdhkarSection.AFTER_PRAYER, Icons.Filled.Mosque, "after_prayer"),
        Triple(AdhkarSection.MISC, Icons.Filled.Category, "misc"),
    )
    Page("الأذكار", onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (sec, icon, key) ->
                        Card(Modifier.weight(1f).clickable { onSection(key) }, shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(sec.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth().clickable(onClick = onFavorites), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("المفضلة", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun MiscCategoriesScreen(onBack: () -> Unit, onCategory: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    var cats by remember { mutableStateOf<List<DhikrCategory>?>(null) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { cats = runCatching { app.adhkar.miscCategories() }.getOrDefault(emptyList()) }
    Page("أذكار متنوعة", onBack) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(12.dp), singleLine = true, label = { Text("بحث في الأبواب") },
                leadingIcon = { Icon(Icons.Filled.Search, null) })
            val shown = cats?.filter { q.isBlank() || it.title.contains(q.trim()) }
            when {
                shown == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("لا توجد نتائج") }
                else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { c ->
                        Card(Modifier.fillMaxWidth().clickable { onCategory(c.id) }, shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(c.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text(c.size.arabic(), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** يحمّل قائمة أذكار من أحد المصادر: قسم، باب، أو المفضلة. */
@Composable
fun DhikrListScreen(kind: String, arg: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    val favs by app.prefs.favAdhkar.collectAsState(initial = emptySet())
    var list by remember(kind, arg) { mutableStateOf<List<Dhikr>?>(null) }
    var title by remember(kind, arg) { mutableStateOf("") }
    LaunchedEffect(kind, arg) {
        runCatching {
            when (kind) {
                "section" -> { val s = AdhkarSection.of(arg)!!; title = s.title; list = app.adhkar.section(s) }
                "category" -> { val l = app.adhkar.category(arg.toInt()); title = l.firstOrNull()?.categoryTitle ?: ""; list = l }
                else -> { title = "المفضلة" }
            }
        }.onFailure { list = emptyList() }
    }
    // المفضلة تتحدث مباشرة عند إزالة عنصر
    LaunchedEffect(kind, favs) { if (kind == "favorites") list = app.adhkar.byIds(favs) }
    Page(title, onBack) { pad ->
        val l = list
        when {
            l == null -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            l.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(if (kind == "favorites") "لا توجد أذكار مفضلة بعد. اضغط على القلب في أي ذكر لإضافته." else "لا توجد أذكار", textAlign = TextAlign.Center)
            }
            else -> DhikrPager(l, favs, Modifier.padding(pad))
        }
    }
}

@Composable
private fun DhikrPager(list: List<Dhikr>, favs: Set<Int>, modifier: Modifier) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { list.size }
    val counts = remember(list.map { it.id }) { mutableStateMapOf<Int, Int>() }
    val font by app.prefs.quranFont.collectAsState(initial = 1f)
    val clipboard = LocalClipboardManager.current
    val snack = remember { SnackbarHostState() }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text("${(pager.currentPage + 1).arabic()} / ${list.size.arabic()}", Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp), style = MaterialTheme.typography.labelMedium)
            HorizontalPager(pager, Modifier.weight(1f), key = { list[it].id }) { i ->
                val d = list[i]
                val done = counts[d.id] ?: 0
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Card(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
                            if (d.categoryTitle.isNotBlank()) Text(d.categoryTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(d.text, fontSize = (22 * font).sp, lineHeight = (38 * font).sp, textAlign = TextAlign.Justify)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { scope.launch { app.prefs.toggleFavAdhkar(d.id) } }) {
                            Icon(if (d.id in favs) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "المفضلة", tint = MaterialTheme.colorScheme.primary)
                        }
                        // عدّاد: الضغط يزيد، وعند بلوغ العدد المطلوب ينتقل تلقائياً للذكر التالي
                        Box(
                            Modifier.size(110.dp).clip(CircleShape)
                                .background(if (done >= d.count) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                                .clickable {
                                    val n = done + 1; counts[d.id] = n
                                    if (n == d.count && i < list.size - 1) scope.launch { pager.animateScrollToPage(i + 1) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(done.arabic(), color = Color.White, fontSize = 32.sp)
                                Text("من ${d.count.arabic()}", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { counts[d.id] = 0 }) { Icon(Icons.Filled.Refresh, "تصفير العدّاد") }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(enabled = i > 0, onClick = { scope.launch { pager.animateScrollToPage(i - 1) } }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null); Text("السابق")
                        }
                        Row {
                            IconButton(onClick = { clipboard.setText(AnnotatedString(d.text)); scope.launch { snack.showSnackbar("تم النسخ") } }) { Icon(Icons.Filled.ContentCopy, "نسخ") }
                            IconButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, d.text) }
                                ctx.startActivity(Intent.createChooser(send, "مشاركة"))
                            }) { Icon(Icons.Filled.Share, "مشاركة") }
                        }
                        TextButton(enabled = i < list.size - 1, onClick = { scope.launch { pager.animateScrollToPage(i + 1) } }) {
                            Text("التالي"); Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                        }
                    }
                }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
}
