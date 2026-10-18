package com.universe.remote.data.web

import android.content.Context

interface WebCacheStore {
    fun json(key: String): String?
    fun savedAt(key: String): Long
    fun write(key: String, json: String, savedAt: Long)
}

object NoOpWebCacheStore : WebCacheStore {
    override fun json(key: String): String? = null
    override fun savedAt(key: String): Long = 0L
    override fun write(key: String, json: String, savedAt: Long) = Unit
}

class PreferencesWebCacheStore(context: Context) : WebCacheStore {
    private val preferences = context.getSharedPreferences(
        "spotify_library_cache",
        Context.MODE_PRIVATE,
    )

    override fun json(key: String): String? = preferences.getString("${key}_json", null)
    override fun savedAt(key: String): Long = preferences.getLong("${key}_saved_at", 0L)
    override fun write(key: String, json: String, savedAt: Long) {
        preferences.edit()
            .putString("${key}_json", json)
            .putLong("${key}_saved_at", savedAt)
            .apply()
    }
}
