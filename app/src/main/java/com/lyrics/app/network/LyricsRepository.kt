package com.lyrics.app.network

import com.lyrics.app.model.LyricsResult
import com.lyrics.app.model.LyricsType
import com.lyrics.app.model.SongInfo
import com.lyrics.app.utils.LyricsConverter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchResult(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val artworkUrl: String,
    val trackId: String
)

object LyricsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String? {
        return try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanTitle(title: String): String {
        return title.replace(
            Regex("""\s*\((?i:feat\.?|ft\.?|with|from)[^)]*\)"""), ""
        ).trim()
    }

    private fun cleanAlbum(album: String?): String? {
        if (album == null) return null
        return album.replace(Regex("""\s*\([^)]*\)"""), "").trim()
    }

    // =========================
    // iTunes Search
    // =========================
    fun searchSongs(query: String, limit: Int = 10): List<SearchResult> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val body = get("https://itunes.apple.com/search?term=$encoded&entity=song&limit=$limit") ?: return emptyList()
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<SearchResult>()

        for (i in 0 until results.length()) {
            val track = results.getJSONObject(i)
            if (track.optString("kind") != "song") continue
            val title = cleanTitle(track.getString("trackName"))
            val artist = track.getString("artistName")
            var album = cleanAlbum(track.optString("collectionName", "")) ?: title
            if (album.contains("single", ignoreCase = true)) album = title
            val duration = (track.getLong("trackTimeMillis") / 1000).toInt()
            val artwork = track.optString("artworkUrl100", "").replace("100x100", "300x300")
            val trackId = track.getLong("trackId").toString()
            list.add(SearchResult(title, artist, album, duration, artwork, trackId))
        }
        return list
    }

    // =========================
    // iTunes Top Charts
    // =========================
        // =========================
    // Apple Music Top 100 Global Playlist
    // =========================
    fun getTopCharts(limit: Int = 10): List<SearchResult> {
        // Fetch Apple Music Top 100 Global playlist
        val playlistId = "pl.d25f5d1181894928af76c85c967f8f31"
        val body = get("https://itunes.apple.com/lookup?id=$playlistId&entity=song&limit=$limit")
            ?: return getFallbackCharts(limit)

        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return getFallbackCharts(limit)
        val list = mutableListOf<SearchResult>()

        for (i in 0 until results.length()) {
            val track = results.getJSONObject(i)
            if (track.optString("kind") != "song") continue
            val title = cleanTitle(track.getString("trackName"))
            val artist = track.getString("artistName")
            var album = cleanAlbum(track.optString("collectionName", "")) ?: title
            if (album.contains("single", ignoreCase = true)) album = title
            val duration = (track.getLong("trackTimeMillis") / 1000).toInt()
            val artwork = track.optString("artworkUrl100", "").replace("100x100", "300x300")
            val trackId = track.getLong("trackId").toString()
            list.add(SearchResult(title, artist, album, duration, artwork, trackId))
            if (list.size >= limit) break
        }

        return if (list.isNotEmpty()) list else getFallbackCharts(limit)
    }

    private fun getFallbackCharts(limit: Int): List<SearchResult> {
        val body = get("https://itunes.apple.com/search?term=top+hits+2025&entity=song&limit=$limit")
            ?: return emptyList()
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<SearchResult>()

        for (i in 0 until results.length()) {
            val track = results.getJSONObject(i)
            if (track.optString("kind") != "song") continue
            val title = cleanTitle(track.getString("trackName"))
            val artist = track.getString("artistName")
            var album = cleanAlbum(track.optString("collectionName", "")) ?: title
            if (album.contains("single", ignoreCase = true)) album = title
            val duration = (track.getLong("trackTimeMillis") / 1000).toInt()
            val artwork = track.optString("artworkUrl100", "").replace("100x100", "300x300")
            val trackId = track.getLong("trackId").toString()
            list.add(SearchResult(title, artist, album, duration, artwork, trackId))
        }
        return list
    }

    // =========================
    // Convert SearchResult to SongInfo
    // =========================
    fun toSongInfo(result: SearchResult) = SongInfo(
    title = result.title,
    artist = result.artist,
    album = result.album,
    duration = result.duration,
    artworkUrl = result.artworkUrl
)

    // =========================
    // iTunes lookup by ID
    // =========================
    fun getSongData(trackId: String): SongInfo? {
        val body = get("https://itunes.apple.com/lookup?id=$trackId") ?: return null
        val json = JSONObject(body)
        if (json.getInt("resultCount") == 0) return null

        val results = json.getJSONArray("results")
        var track: JSONObject? = null
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            if (item.optString("kind") == "song") { track = item; break }
        }
        track ?: return null

        val title = cleanTitle(track.getString("trackName"))
        val artist = track.getString("artistName")
        var album = cleanAlbum(track.optString("collectionName", "")) ?: title
        if (album.contains("single", ignoreCase = true)) album = title
        val duration = (track.getLong("trackTimeMillis") / 1000).toInt()
        return SongInfo(title, artist, album, duration)
    }

    fun searchSong(title: String, artist: String): SongInfo? {
        val results = searchSongs("$title $artist", 1)
        return results.firstOrNull()?.let { toSongInfo(it) }
    }

    fun extractTrackId(url: String): String? {
        val m1 = Regex("""[?&]i=(\d+)""").find(url)
        if (m1 != null) return m1.groupValues[1]
        val m2 = Regex("""/(\d{6,})""").find(url)
        return m2?.groupValues?.get(1)
    }

    fun extractFromPage(url: String): Pair<String, String>? {
        val body = get(url) ?: return null
        val m = Regex("<title>(.*?)</title>").find(body) ?: return null
        var title = m.groupValues[1]
            .replace(" - YouTube Music", "")
            .replace(" - YouTube", "")
            .replace(" | Spotify", "")
        val parts = title.split(" - ")
        return if (parts.size >= 2) Pair(parts[1].trim(), parts[0].trim())
        else Pair(title.trim(), "")
    }

    fun parseManual(text: String): SongInfo? {
        if ("🎵" in text && "👤" in text) {
            var title: String? = null; var artist: String? = null
            var album: String? = null; var duration: Int? = null
            text.lines().forEach { line ->
                val l = line.trim()
                when {
                    l.startsWith("🎵") -> title = l.removePrefix("🎵").trim()
                    l.startsWith("👤") -> artist = l.removePrefix("👤").trim()
                    l.startsWith("💿") -> album = l.removePrefix("💿").trim()
                    l.startsWith("⏱") -> duration = l.removePrefix("⏱").replace("s", "").trim().toIntOrNull()
                }
            }
            if (title != null && artist != null && album != null && duration != null)
                return SongInfo(title!!, artist!!, album!!, duration!!)
        }
        if ("|" in text) {
            val parts = text.split("|").map { it.trim() }
            if (parts.size == 4) {
                val dur = parts[3].toIntOrNull() ?: return null
                return SongInfo(parts[0], parts[1], parts[2], dur)
            }
        }
        val m = Regex("""(.+?)\s*-\s*(.+)""").find(text)
        if (m != null) return searchSong(m.groupValues[1].trim(), m.groupValues[2].trim())
        return null
    }

    // =========================
    // Apple Music Lyrics
    // =========================
    fun fetchAppleLyrics(song: SongInfo): LyricsResult? {
        val url = "https://lyrics-api.boidu.dev/getLyrics" +
                "?s=${encode(song.title)}&a=${encode(song.artist)}&al=${encode(song.album)}&d=${song.duration}"
        val body = get(url) ?: return null
        if (body.isBlank()) return null
        val json = try { JSONObject(body) } catch (e: Exception) { return null }

        val ttml = json.optString("ttml", "")
        if (ttml.isNotBlank()) {
            val isLine = ttml.contains("itunes:timing=\"Line\"")
            val converted = LyricsConverter.convertTtml(ttml)
            if (converted.isNotBlank())
                return LyricsResult("Apple Music", converted, if (isLine) LyricsType.LINE else LyricsType.WORD)
        }
        val plainLyrics = json.optString("lyrics", "")
        if (plainLyrics.isNotBlank())
            return LyricsResult("Apple Music", plainLyrics, LyricsType.LINE)
        return null
    }

    // =========================
    // LyricsPlus
    // =========================
    fun fetchLyricsPlus(song: SongInfo): LyricsResult? {
        val bases = listOf(
            "https://lyricsplus.binimum.org",
            "https://lyricsplus.atomix.one",
            "https://lyricsplus.prjktla.my.id",
            "https://lyricsplus-seven.vercel.app"
        )
        for (base in bases) {
            try {
                val url = "$base/v2/lyrics/get?title=${encode(song.title)}&artist=${encode(song.artist)}&duration=${song.duration}"
                val body = get(url) ?: continue
                if (body.isBlank()) continue
                val json = JSONObject(body)
                val lyricsArr = json.optJSONArray("lyrics")
                if (lyricsArr == null || lyricsArr.length() == 0) continue
                val isLine = json.optString("type", "Word").equals("Line", ignoreCase = true)
                val converted = if (isLine) LyricsConverter.convertJsonLine(json) else LyricsConverter.convertJsonLyrics(json)
                if (converted.isNotBlank())
                    return LyricsResult("LyricsPlus", converted, if (isLine) LyricsType.LINE else LyricsType.WORD)
            } catch (e: Exception) { continue }
        }
        return null
    }

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
