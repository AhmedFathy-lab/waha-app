package com.waha.app.ui.duas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waha.app.WahaApp
import com.waha.app.data.adhkar.Dhikr
import com.waha.app.data.adhkar.DhikrCategory
import com.waha.app.ui.theme.wahaBackground
import com.waha.app.util.arabic
import com.waha.app.util.normalizeArabic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = Color.Transparent, modifier = Modifier.background(wahaBackground()),
        topBar = {
            TopAppBar(title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        },
        content = content,
    )
}

@Composable
fun DuasHomeScreen(onBack: () -> Unit, onCategory: (Int) -> Unit, onFavorites: () -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    var cats by remember { mutableStateOf<List<DhikrCategory>?>(null) }
    var all by remember { mutableStateOf<List<Dhikr>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        runCatching { cats = app.adhkar.duaCategories(); all = app.adhkar.allDuas() }.onFailure { cats = emptyList() }
    }
    Page("الأدعية", onBack) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(12.dp), singleLine = true,
                label = { Text("ابحث في الأدعية") }, leadingIcon = { Icon(Icons.Filled.Search, null) })
            val nq = normalizeArabic(q.trim())
            when {
                cats == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                nq.isNotEmpty() -> {
                    val hits = all.filter { normalizeArabic(it.text).contains(nq) || normalizeArabic(it.categoryTitle).contains(nq) }
                    if (hits.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("لا توجد نتائج") }
                    else DuaCards(hits, showCategory = true)
                }
                else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Card(Modifier.fillMaxWidth().clickable(onClick = onFavorites), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp)); Text("المفضلة", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    items(cats!!, key = { it.id }) { c ->
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

@Composable
fun DuasListScreen(kind: String, arg: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as WahaApp
    val favs by app.prefs.favDuas.collectAsState(initial = emptySet())
    var list by remember(kind, arg) { mutableStateOf<List<Dhikr>?>(null) }
    LaunchedEffect(kind, arg, favs) {
        runCatching {
            list = if (kind == "favorites") app.adhkar.allDuas().filter { it.id in favs } else app.adhkar.category(arg.toInt())
        }.onFailure { list = emptyList() }
    }
    val title = if (kind == "favorites") "المفضلة" else list?.firstOrNull()?.categoryTitle ?: ""
    Page(title, onBack) { pad ->
        val l = list
        when {
            l == null -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            l.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد أدعية مفضلة بعد. اضغط على القلب في أي دعاء لإضافته.", textAlign = TextAlign.Center)
            }
            else -> Box(Modifier.padding(pad)) { DuaCards(l, showCategory = false) }
        }
    }
}

@Composable
private fun DuaCards(list: List<Dhikr>, showCategory: Boolean) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    val favs by app.prefs.favDuas.collectAsState(initial = emptySet())
    val font by app.prefs.quranFont.collectAsState(initial = 1f)
    val clipboard = LocalClipboardManager.current
    val snack = remember { SnackbarHostState() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        if (showCategory) Text(d.categoryTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(d.text, fontSize = (20 * font).sp, lineHeight = (34 * font).sp, textAlign = TextAlign.Justify)
                        Text("المصدر: حصن المسلم", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { scope.launch { app.prefs.toggleFavDua(d.id) } }) {
                                Icon(if (d.id in favs) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "المفضلة", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(d.text)); scope.launch { snack.showSnackbar("تم النسخ") } }) { Icon(Icons.Filled.ContentCopy, "نسخ") }
                            IconButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, d.text) }
                                ctx.startActivity(Intent.createChooser(send, "مشاركة"))
                            }) { Icon(Icons.Filled.Share, "مشاركة") }
                        }
                    }
                }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
}
