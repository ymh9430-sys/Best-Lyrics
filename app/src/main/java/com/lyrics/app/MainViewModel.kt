package com.lyrics.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun process(input: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val song = withContext(Dispatchers.IO) {
                resolveSong(input.trim())
            }

            if (song == null) {
                _uiState.value = UiState.Error("❌ لم أستطع استخراج بيانات الأغنية")
                return@launch
            }

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
    }

    private fun resolveSong(input: String) = when {
        input.startsWith("http") -> {
            val trackId = LyricsRepository.extractTrackId(input)
            if (trackId != null) {
                LyricsRepository.getSongData(trackId)
            } else {
                val info = LyricsRepository.extractFromPage(input) ?: return@when null
                LyricsRepository.searchSong(info.first, info.second)
            }
        }
        else -> LyricsRepository.parseManual(input)
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }
}
