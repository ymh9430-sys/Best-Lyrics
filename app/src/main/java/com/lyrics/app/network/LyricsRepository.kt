package com.lyrics.app.network

import com.lyrics.app.model.LyricsResult
import com.lyrics.app.model.LyricsType
import com.lyrics.app.model.SongInfo
import com.lyrics.app.utils.LyricsConverter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    fun extractTrackId(url: String): String? {
        val m1 = Regex("""[?&]i=(\d+)""").find(url)
        if (m1 != null) return m1.groupValues[1]
        val m2 = Regex("""/(\d{6,})""").find(url)
        return m2?.groupValues?.get(1)
    }

    fun getSongData(trackId: String): SongInfo? {
        val body = get("https://itunes.apple.com/lookup?id=$trackId") ?: return null
        val json = JSONObject(body)
        if (json.getInt("resultCount") == 0) return null

        val results = json.getJSONArray("results")
        var track: JSONObject? = null
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            if (item.optString("kind") == "song") {
                track = item
                break
            }
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
        val query = "${title} ${artist}".replace(" ", "+")
        val body = get("https://itunes.apple.com/search?term=$query&entity=song&limit=1") ?: return null
        val json = JSONObject(body)
        if (json.getInt("resultCount") == 0) return null

        val track = json.getJSONArray("results").getJSONObject(0)
        val t = cleanTitle(track.getString("trackName"))
        val a = track.getString("artistName")
        var al = cleanAlbum(track.optString("collectionName", "")) ?: t
        if (al.contains("single", ignoreCase = true)) al = t
        val dur = (track.getLong("trackTimeMillis") / 1000).toInt()

        return SongInfo(t, a, al, dur)
    }

    fun extractFromPage(url: String): Pair<String, String>? {
        val body = get(url) ?: return null
        val m = Regex("<title>(.*?)</title>").find(body) ?: return null
        var title = m.groupValues[1]
            .replace(" - YouTube Music", "")
            .replace(" - YouTube", "")
            .replace(" | Spotify", "")

        val parts = title.split(" - ")
        return if (parts.size >= 2) {
            Pair(parts[1].trim(), parts[0].trim())
        } else {
            Pair(title.trim(), "")
        }
    }

    fun parseManual(text: String): SongInfo? {
        if ("🎵" in text && "👤" in text) {
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var duration: Int? = null

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
        if (m != null) {
            return searchSong(m.groupValues[1].trim(), m.groupValues[2].trim())
        }

        return null
    }

    // =========================
    // Apple Music
    // =========================
    fun fetchAppleLyrics(song: SongInfo): LyricsResult? {
        val url = "https://lyrics-api.boidu.dev/getLyrics" +
                "?s=${encode(song.title)}&a=${encode(song.artist)}&al=${encode(song.album)}&d=${song.duration}"

        val body = get(url) ?: return null
        if (body.isBlank()) return null

        val json = try { JSONObject(body) } catch (e: Exception) { return null }

        val ttml = json.optString("ttml", "")
        if (ttml.isNotBlank()) {
            // detect Line vs Word timing
            val isLine = ttml.contains("itunes:timing=\"Line\"")
            val converted = LyricsConverter.convertTtml(ttml)
            if (converted.isNotBlank())
                return LyricsResult(
                    source = "Apple Music",
                    lyrics = converted,
                    type = if (isLine) LyricsType.LINE else LyricsType.WORD
                )
        }

        val plainLyrics = json.optString("lyrics", "")
        if (plainLyrics.isNotBlank()) {
            return LyricsResult(
                source = "Apple Music",
                lyrics = plainLyrics,
                type = LyricsType.LINE
            )
        }

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
                val url = "$base/v2/lyrics/get" +
                        "?title=${encode(song.title)}&artist=${encode(song.artist)}&duration=${song.duration}"
                val body = get(url) ?: continue
                if (body.isBlank()) continue

                val json = JSONObject(body)
                val lyricsArr = json.optJSONArray("lyrics")
                if (lyricsArr == null || lyricsArr.length() == 0) continue

                // detect Line vs Word
                val type = json.optString("type", "Word")
                val isLine = type.equals("Line", ignoreCase = true)

                if (isLine) {
                    // convert directly from time+text
                    val converted = LyricsConverter.convertJsonLine(json)
                    if (converted.isNotBlank())
                        return LyricsResult(
                            source = "LyricsPlus",
                            lyrics = converted,
                            type = LyricsType.LINE
                        )
                } else {
                    val converted = LyricsConverter.convertJsonLyrics(json)
                    if (converted.isNotBlank())
                        return LyricsResult(
                            source = "LyricsPlus",
                            lyrics = converted,
                            type = LyricsType.WORD
                        )
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
