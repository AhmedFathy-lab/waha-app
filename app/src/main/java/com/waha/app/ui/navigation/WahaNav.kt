package com.waha.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waha.app.WahaApp
import com.waha.app.ui.adhkar.*
import com.waha.app.ui.duas.*
import com.waha.app.ui.home.HomeScreen
import com.waha.app.ui.settings.SettingsScreen
import com.waha.app.ui.prayer.PrayerScreen
import com.waha.app.ui.tasbih.*
import com.waha.app.util.arabic
import com.waha.app.ui.quran.QuranHomeScreen
import com.waha.app.ui.quran.ReaderScreen

object Routes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val ADHKAR = "adhkar"
    const val ADHKAR_LIST = "adhkar/{kind}/{arg}"
    const val ADHKAR_MISC = "adhkar_misc"
    const val SETTINGS = "settings"
    const val PRAYER = "prayer"
    const val TASBIH = "tasbih"
    const val TASBIH_HISTORY = "tasbih_history"
    const val DUAS = "duas"
    const val DUAS_LIST = "duas/{kind}/{arg}"
    const val READER = "reader/{surah}/{ayah}"
    fun reader(s: Int, a: Int) = "reader/$s/$a"
}

@Composable
fun WahaNav() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as WahaApp
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val last by app.prefs.lastPos.collectAsState(initial = null)
            var resumeName by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(last) { resumeName = last?.let { runCatching { app.quran.surah(it.surah)?.name }.getOrNull() } }
            HomeScreen(
                resumeText = last?.let { p -> resumeName?.let { "سورة $it • آية ${p.ayah.arabic()}" } },
                onResume = { last?.let { nav.navigate(Routes.reader(it.surah, it.ayah)) } },
                onQuran = { nav.navigate(Routes.QURAN) },
                onAdhkar = { nav.navigate(Routes.ADHKAR) }, onTasbih = { nav.navigate(Routes.TASBIH) }, onDuas = { nav.navigate(Routes.DUAS) }, onSettings = { nav.navigate(Routes.SETTINGS) }, onPrayer = { nav.navigate(Routes.PRAYER) },
            )
        }
        composable(Routes.QURAN) {
            QuranHomeScreen(onBack = { nav.popBackStack() }, onOpen = { s, a -> nav.navigate(Routes.reader(s, a)) })
        }
        composable(Routes.ADHKAR) {
            AdhkarHomeScreen(
                onBack = { nav.popBackStack() },
                onSection = { key -> if (key == "misc") nav.navigate(Routes.ADHKAR_MISC) else nav.navigate("adhkar/section/$key") },
                onFavorites = { nav.navigate("adhkar/favorites/all") },
            )
        }
        composable(Routes.ADHKAR_MISC) { MiscCategoriesScreen({ nav.popBackStack() }) { nav.navigate("adhkar/category/$it") } }
        composable(Routes.ADHKAR_LIST, arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("arg") { type = NavType.StringType })) { e ->
            DhikrListScreen(e.arguments!!.getString("kind")!!, e.arguments!!.getString("arg")!!, onBack = { nav.popBackStack() })
        }
        composable(Routes.DUAS) { DuasHomeScreen({ nav.popBackStack() }, { nav.navigate("duas/category/$it") }, { nav.navigate("duas/favorites/all") }) }
        composable(Routes.DUAS_LIST, arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("arg") { type = NavType.StringType })) { e ->
            DuasListScreen(e.arguments!!.getString("kind")!!, e.arguments!!.getString("arg")!!, onBack = { nav.popBackStack() })
        }
        composable(Routes.TASBIH) { TasbihScreen({ nav.popBackStack() }, { nav.navigate(Routes.TASBIH_HISTORY) }) }
        composable(Routes.TASBIH_HISTORY) { TasbihHistoryScreen { nav.popBackStack() } }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() }, onPrayer = { nav.navigate(Routes.PRAYER) },
                onAdhkarFavs = { nav.navigate("adhkar/favorites/all") }, onDuaFavs = { nav.navigate("duas/favorites/all") },
                onBookmarks = { nav.navigate(Routes.QURAN) },
            )
        }
        composable(Routes.PRAYER) { PrayerScreen { nav.popBackStack() } }
        composable(Routes.READER, arguments = listOf(navArgument("surah") { type = NavType.IntType }, navArgument("ayah") { type = NavType.IntType })) { e ->
            ReaderScreen(
                surahId = e.arguments!!.getInt("surah"), startAyah = e.arguments!!.getInt("ayah"),
                onBack = { nav.popBackStack() },
                onOpenSurah = { nav.navigate(Routes.reader(it, 1)) { popUpTo(Routes.QURAN) } },
            )
        }
    }
}
