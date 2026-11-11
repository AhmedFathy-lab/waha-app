package com.waha.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.waha.app.data.quran.Ayah
import com.waha.app.data.quran.Surah
import com.waha.app.util.arabic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import android.content.Intent

private val NightBg = Color(0xFF0B1A12)
private val NightFg = Color(0xFFE6F2EA)
private val PaperBg = Color(0xFFFFFCF2)
private val PaperFg = Color(0xFF14231B)

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(surahId: Int, startAyah: Int, onBack: () -> Unit, onOpenSurah: (Int) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as WahaApp
    val scope = rememberCoroutineScope()
    var surah by remember(surahId) { mutableStateOf<Surah?>(null) }
    var ayat by remember(surahId) { mutableStateOf<List<Ayah>>(emptyList()) }
    var error by remember(surahId) { mutableStateOf<String?>(null) }
    LaunchedEffect(surahId) {
        runCatching { surah = app.quran.surah(surahId); ayat = app.quran.ayat(surahId) }.onFailure { error = "تعذّر تحميل السورة" }
    }
    val font by app.prefs.quranFont.collectAsState(initial = 1f)
    val night by app.prefs.quranNight.collectAsState(initial = false)
    val marks by app.prefs.bookmarks.collectAsState(initial = emptyList())
    val bg = if (night) NightBg else PaperBg
    val fg = if (night) NightFg else PaperFg
    var selected by remember { mutableStateOf<Ayah?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val snack = remember { SnackbarHostState() }

    val list = rememberLazyListState()
    // الصف 0 هو ترويسة السورة، لذلك الآية n في الفهرس n
    LaunchedEffect(ayat) { if (ayat.isNotEmpty()) list.scrollToItem(startAyah.coerceIn(1, ayat.size).let { if (it == 1) 0 else it }) }
    LaunchedEffect(ayat) {
        if (ayat.isEmpty()) return@LaunchedEffect
        snapshotFlow { list.firstVisibleItemIndex }.debounce(400).collect { idx ->
            app.prefs.saveLastPos(surahId, idx.coerceIn(1, ayat.size))
        }
    }
    val current = ayat.getOrNull((list.firstVisibleItemIndex - 1).coerceAtLeast(0))

    Scaffold(
        containerColor = bg,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(surah?.let { "سورة ${it.name}" } ?: "", color = fg)
                        if (current != null) Text("الجزء ${current.juz.arabic()} • صفحة ${current.page.arabic()}", fontSize = 12.sp, color = fg.copy(alpha = .7f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = fg) } },
                actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.TextFields, "إعدادات القراءة", tint = fg) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
            )
        },
    ) { pad ->
        when {
            error != null -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text(error!!, color = fg) }
            ayat.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(state = list, modifier = Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
                itemsIndexed(listOf<Ayah?>(null) + ayat, key = { i, a -> if (a == null) -1 else a.ayah }) { _, a ->
                    if (a == null) SurahHeader(surah, fg, font, night, surahId, onOpenSurah)
                    else {
                        val marked = marks.any { it.surah == a.surah && it.ayah == a.ayah }
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (marked) fg.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { selected = a }.padding(vertical = 8.dp),
                        ) {
                            Text(
                                a.text + " ﴿${a.ayah.arabic()}﴾" + if (a.sajda) "  ۩" else "",
                                color = fg, fontSize = (24 * font).sp, lineHeight = (24 * font * 1.9f).sp, textAlign = TextAlign.Justify,
                            )
                        }
                    }
                }
                item {
                    val next = surahId + 1
                    if (next <= 114) TextButton(onClick = { onOpenSurah(next) }, Modifier.fillMaxWidth()) { Text("السورة التالية", color = fg) }
                }
            }
        }
    }

    selected?.let { a ->
        val isMarked = marks.any { it.surah == a.surah && it.ayah == a.ayah }
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("سورة ${surah?.name} • آية ${a.ayah.arabic()}", style = MaterialTheme.typography.titleMedium)
                ListItem(headlineContent = { Text(if (isMarked) "إزالة العلامة المرجعية" else "إضافة علامة مرجعية") },
                    leadingContent = { Icon(if (isMarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null) },
                    modifier = Modifier.clickable { scope.launch { app.prefs.toggleBookmark(a.surah, a.ayah) }; selected = null })
                ListItem(headlineContent = { Text("نسخ الآية") }, leadingContent = { Icon(Icons.Filled.ContentCopy, null) },
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString("${a.text} ﴿${a.ayah.arabic()}﴾ [سورة ${surah?.name}]"))
                        scope.launch { snack.showSnackbar("تم النسخ") }; selected = null
                    })
                ListItem(headlineContent = { Text("مشاركة") }, leadingContent = { Icon(Icons.Filled.Share, null) },
                    modifier = Modifier.clickable {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${a.text} ﴿${a.ayah.arabic()}﴾ [سورة ${surah?.name}]") }
                        ctx.startActivity(Intent.createChooser(send, "مشاركة الآية")); selected = null
                    })
            }
        }
    }
    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("إعدادات القراءة", style = MaterialTheme.typography.titleMedium)
                Text("حجم الخط")
                Slider(value = font, onValueChange = { scope.launch { app.prefs.setQuranFont(it) } }, valueRange = 0.8f..2.2f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("وضع القراءة الليلية", Modifier.weight(1f))
                    Switch(checked = night, onCheckedChange = { scope.launch { app.prefs.setQuranNight(it) } })
                }
            }
        }
    }
}

@Composable
private fun SurahHeader(s: Surah?, fg: Color, font: Float, night: Boolean, id: Int, onOpen: (Int) -> Unit) {
    if (s == null) return
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (night) Color(0xFF16311F) else Color(0xFFCDEFDD)).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) { Text("سورة ${s.name}", color = fg, fontSize = (26 * font).sp) }
        if (s.basmala != null) Text(s.basmala, color = fg, fontSize = (24 * font).sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
    }
}
