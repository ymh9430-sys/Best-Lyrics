package com.lyrics.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyrics.app.model.SongInfo
import com.lyrics.app.model.UiState
import com.lyrics.app.network.LyricsRepository
import com.lyrics.app.network.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("lyrics_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    private val _topCharts = MutableStateFlow<List<SearchResult>>(emptyList())
    val topCharts: StateFlow<List<SearchResult>> = _topCharts

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _recentSearches = MutableStateFlow<List<SearchResult>>(emptyList())
    val recentSearches: StateFlow<List<SearchResult>> = _recentSearches

    init {
        loadTopCharts()
        loadRecentSearches()
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
        addToRecentSearches(result)
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
            val song = withContext(Dispatchers.IO) { LyricsRepository.searchSong(title, artist) }
            if (song == null) {
                _uiState.value = UiState.Error("❌ Song not found")
                return@launch
            }
            fetchLyrics(song)
        }
    }

    fun processManual(title: String, artist: String, album: String, duration: Int) {
        fetchLyrics(SongInfo(title, artist, album, duration))
    }

    private fun fetchLyrics(song: SongInfo) {
        viewModelScope.launch {
            _uiState.value = UiState.SongFound(song)
            val results = withContext(Dispatchers.IO) {
                // الثلاثة بيشتغلوا مع بعض في نفس الوقت
                val appleDeferred = async { LyricsRepository.fetchAppleLyrics(song) }
val paxsenixDeferred = async { LyricsRepository.fetchPaxsenix(song) }
val plusDeferred = async { LyricsRepository.fetchLyricsPlus(song) }
val lrcLibDeferred = async { LyricsRepository.fetchLrcLib(song) }

listOfNotNull(
    appleDeferred.await(),
    paxsenixDeferred.await(),
    plusDeferred.await(),
    lrcLibDeferred.await()
)
            }
            if (results.isEmpty()) {
                _uiState.value = UiState.Error("❌ No lyrics found for this song")
            } else {
                _uiState.value = UiState.Success(song, results)
            }
        }
    }

    // =========================
    // Recent Searches
    // =========================
    private fun addToRecentSearches(result: SearchResult) {
        val current = _recentSearches.value.toMutableList()
        current.removeAll { it.trackId == result.trackId }
        current.add(0, result)
        val trimmed = current.take(30)
        _recentSearches.value = trimmed
        saveRecentSearches(trimmed)
    }

    private fun saveRecentSearches(list: List<SearchResult>) {
        val arr = JSONArray()
        list.forEach { r ->
            val obj = JSONObject()
            obj.put("title", r.title)
            obj.put("artist", r.artist)
            obj.put("album", r.album)
            obj.put("duration", r.duration)
            obj.put("artworkUrl", r.artworkUrl)
            obj.put("trackId", r.trackId)
            arr.put(obj)
        }
        prefs.edit().putString("recent_searches", arr.toString()).apply()
    }

    private fun loadRecentSearches() {
        val json = prefs.getString("recent_searches", null) ?: return
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<SearchResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(SearchResult(
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    album = obj.getString("album"),
                    duration = obj.getInt("duration"),
                    artworkUrl = obj.getString("artworkUrl"),
                    trackId = obj.getString("trackId")
                ))
            }
            _recentSearches.value = list
        } catch (e: Exception) { }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove("recent_searches").apply()
    }

    fun reset() {
        _uiState.value = UiState.Idle
        _searchResults.value = emptyList()
    }
}
