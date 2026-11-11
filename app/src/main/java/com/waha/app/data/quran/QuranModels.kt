package com.waha.app.data.quran

data class Surah(val id: Int, val name: String, val ayahCount: Int, val basmala: String?, val firstPage: Int, val firstJuz: Int)
data class Ayah(val surah: Int, val ayah: Int, val text: String, val juz: Int, val page: Int, val sajda: Boolean)
