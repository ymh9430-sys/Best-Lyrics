package com.lyrics.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyrics.app.model.SongInfo
import com.lyrics.app.model.UiState
import com.lyrics.app.network.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    // Auto Search - URL
    fun processUrl(input: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val song = withContext(Dispatchers.IO) {
                if (input.startsWith("http")) {
                    val trackId = LyricsRepository.extractTrackId(input)
                    if (trackId != null) {
                        LyricsRepository.getSongData(trackId)
                    } else {
                        val info = LyricsRepository.extractFromPage(input)
                        if (info != null) LyricsRepository.searchSong(info.first, info.second) else null
                    }
                } else null
            }

            if (song == null) {
                _uiState.value = UiState.Error("❌ لم أستطع استخراج بيانات الأغنية")
                return@launch
            }

            fetchLyrics(song)
        }
    }

    // Auto Search - Title + Artist
    fun processTitleArtist(title: String, artist: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val song = withContext(Dispatchers.IO) {
                LyricsRepository.searchSong(title, artist)
            }

            if (song == null) {
                _uiState.value = UiState.Error("❌ لم أستطع العثور على الأغنية")
                return@launch
            }

            fetchLyrics(song)
        }
    }

    // Manual Search - Direct to API
    fun processManual(title: String, artist: String, album: String, duration: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val song = SongInfo(title, artist, album, duration)
            _uiState.value = UiState.SongFound(song)

            fetchLyrics(song)
        }
    }

    private suspend fun fetchLyrics(song: SongInfo) {
        _uiState.value = UiState.SongFound(song)

        val results = withContext(Dispatchers.IO) {
            val apple = LyricsRepository.fetchAppleLyrics(song)
            val plus = LyricsRepository.fetchLyricsPlus(song)
            listOfNotNull(apple, plus)
        }

        if (results.isEmpty()) {
            _uiState.value = UiState.Error("❌ لم يتم العثور على كلمات لهذه الأغنية")
        } else {
            _uiState.value = UiState.Success(song, results)
        }
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }
}
