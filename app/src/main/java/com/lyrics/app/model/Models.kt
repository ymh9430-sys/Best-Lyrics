package com.lyrics.app.model

data class SongInfo(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int
)

data class LyricsResult(
    val source: String,
    val lyrics: String,
    val type: LyricsType = LyricsType.WORD
)

enum class LyricsType {
    WORD, // كلمة كلمة - يعرض الـ 4 أزرار
    LINE  // سطر سطر - يعرض Synced و Plain بس
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class SongFound(val song: SongInfo) : UiState()
    data class Success(val song: SongInfo, val results: List<LyricsResult>) : UiState()
    data class Error(val message: String) : UiState()
}
