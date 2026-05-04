package com.lyrics.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyrics.app.model.SongInfo
import com.lyrics.app.model.UiState
import com.lyrics.app.network.LyricsRepository
import com.lyrics.app.network.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    private val _topCharts = MutableStateFlow<List<SearchResult>>(emptyList())
    val topCharts: StateFlow<List<SearchResult>> = _topCharts

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    init {
        loadTopCharts()
    }

    fun loadTopCharts() {
        viewModelScope.launch {
            val charts = withContext(Dispatchers.IO) {
                LyricsRepository.getTopCharts(10)
            }
            _topCharts.value = charts
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val results = withContext(Dispatchers.IO) {
                LyricsRepository.searchSongs(query, 15)
            }
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun fetchLyricsFromResult(result: SearchResult) {
        val song = LyricsRepository.toSongInfo(result)
        fetchLyrics(song)
    }

    fun processUrl(input: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val song = withContext(Dispatchers.IO) {
                if (input.startsWith("http")) {
                    val trackId = LyricsRepository.extractTrackId(input)
                    if (trackId != null) LyricsRepository.getSongData(trackId)
                    else {
                        val info = LyricsRepository.extractFromPage(input)
                        if (info != null) LyricsRepository.searchSong(info.first, info.second) else null
                    }
                } else null
            }
            if (song == null) {
                _uiState.value = UiState.Error("❌ Could not extract song data")
                return@launch
            }
            fetchLyrics(song)
        }
    }

    fun processTitleArtist(title: String, artist: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val song = withContext(Dispatchers.IO) {
                LyricsRepository.searchSong(title, artist)
            }
            if (song == null) {
                _uiState.value = UiState.Error("❌ Song not found")
                return@launch
            }
            fetchLyrics(song)
        }
    }

    fun processManual(title: String, artist: String, album: String, duration: Int) {
        val song = SongInfo(title, artist, album, duration)
        fetchLyrics(song)
    }

    private fun fetchLyrics(song: SongInfo) {
        viewModelScope.launch {
            _uiState.value = UiState.SongFound(song)
            val results = withContext(Dispatchers.IO) {
                val apple = LyricsRepository.fetchAppleLyrics(song)
                val plus = LyricsRepository.fetchLyricsPlus(song)
                listOfNotNull(apple, plus)
            }
            if (results.isEmpty()) {
                _uiState.value = UiState.Error("❌ No lyrics found for this song")
            } else {
                _uiState.value = UiState.Success(song, results)
            }
        }
    }

    fun reset() {
        _uiState.value = UiState.Idle
        _searchResults.value = emptyList()
    }
}
