package com.oybek.webapphub.data

import android.content.Context
import com.oybek.webapphub.model.HistoryItem
import com.oybek.webapphub.model.SiteApp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("webapphub_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", "tr") ?: "tr"
        set(value) = prefs.edit().putString("language", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var globalAdBlock: Boolean
        get() = prefs.getBoolean("global_adblock", true)
        set(value) = prefs.edit().putBoolean("global_adblock", value).apply()

    var backgroundAudioEnabled: Boolean
        get() = prefs.getBoolean("bg_audio", true)
        set(value) = prefs.edit().putBoolean("bg_audio", value).apply()

    var videoGesturesEnabled: Boolean
        get() = prefs.getBoolean("video_gestures", true)
        set(value) = prefs.edit().putBoolean("video_gestures", value).apply()

    var autoLandscapeFullscreen: Boolean
        get() = prefs.getBoolean("auto_landscape", true)
        set(value) = prefs.edit().putBoolean("auto_landscape", value).apply()

    fun getSites(): MutableList<SiteApp> {
        val raw = prefs.getString("sites_json", null)
        if (raw == null) {
            val defaults = mutableListOf(
                SiteApp(UUID.randomUUID().toString(), "YouTube", "https://m.youtube.com", true, false),
                SiteApp(UUID.randomUUID().toString(), "Kun.uz", "https://kun.uz", true, false),
                SiteApp(UUID.randomUUID().toString(), "Google", "https://www.google.com", true, false),
                SiteApp(UUID.randomUUID().toString(), "Wikipedia", "https://tr.m.wikipedia.org", true, false)
            )
            saveSites(defaults)
            return defaults
        }
        val list = mutableListOf<SiteApp>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SiteApp(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    url = obj.getString("url"),
                    adBlockEnabled = obj.optBoolean("adBlockEnabled", true),
                    desktopMode = obj.optBoolean("desktopMode", false)
                )
            )
        }
        return list
    }

    fun saveSites(sites: List<SiteApp>) {
        val arr = JSONArray()
        for (s in sites) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("url", s.url)
            obj.put("adBlockEnabled", s.adBlockEnabled)
            obj.put("desktopMode", s.desktopMode)
            arr.put(obj)
        }
        prefs.edit().putString("sites_json", arr.toString()).apply()
    }

    fun getHistory(): MutableList<HistoryItem> {
        val raw = prefs.getString("history_json", "[]") ?: "[]"
        val list = mutableListOf<HistoryItem>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                HistoryItem(
                    title = obj.optString("title", ""),
                    url = obj.optString("url", ""),
                    timestamp = obj.optLong("timestamp", 0L)
                )
            )
        }
        return list
    }

    fun addHistory(title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        val list = getHistory()
        list.removeAll { it.url == url }
        list.add(0, HistoryItem(title.ifBlank { url }, url, System.currentTimeMillis()))
        if (list.size > 100) {
            list.subList(100, list.size).clear()
        }
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString("history_json", arr.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().putString("history_json", "[]").apply()
    }
}
