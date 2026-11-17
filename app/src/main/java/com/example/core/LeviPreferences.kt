package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LaunchHistoryEntry
import com.example.model.LaunchMode
import com.example.model.SavedServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.leviDataStore by preferencesDataStore(name = "levilab_prefs")

class LeviPreferences(private val context: Context) {

    private val languageKey = stringPreferencesKey("app_language")
    private val serversJsonKey = stringPreferencesKey("saved_servers_json")
    private val launchModeKey = stringPreferencesKey("preferred_launch_mode")
    private val autoVerifyCacheKey = booleanPreferencesKey("auto_verify_cache")
    private val preferDirectApkLoadKey = booleanPreferencesKey("prefer_direct_apk_load")
    private val launchHistoryJsonKey = stringPreferencesKey("launch_history_json")

    val languageFlow: Flow<AppLanguage> = context.leviDataStore.data.map { prefs ->
        when (prefs[languageKey]) {
            AppLanguage.EN.name -> AppLanguage.EN
            else -> AppLanguage.AR
        }
    }

    val launchModeFlow: Flow<LaunchMode> = context.leviDataStore.data.map { prefs ->
        runCatching {
            LaunchMode.valueOf(prefs[launchModeKey] ?: LaunchMode.DIRECT_NATIVE.name)
        }.getOrDefault(LaunchMode.DIRECT_NATIVE)
    }

    val autoVerifyCacheFlow: Flow<Boolean> = context.leviDataStore.data.map { prefs ->
        prefs[autoVerifyCacheKey] ?: true
    }

    val preferDirectApkLoadFlow: Flow<Boolean> = context.leviDataStore.data.map { prefs ->
        prefs[preferDirectApkLoadKey] ?: true
    }

    val savedServersFlow: Flow<List<SavedServer>> = context.leviDataStore.data.map { prefs ->
        decodeServers(prefs[serversJsonKey])
    }

    val launchHistoryFlow: Flow<List<LaunchHistoryEntry>> = context.leviDataStore.data.map { prefs ->
        decodeLaunchHistory(prefs[launchHistoryJsonKey])
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.leviDataStore.edit { prefs ->
            prefs[languageKey] = language.name
        }
    }

    suspend fun setLaunchMode(mode: LaunchMode) {
        context.leviDataStore.edit { prefs ->
            prefs[launchModeKey] = mode.name
        }
    }

    suspend fun setAutoVerifyCache(enabled: Boolean) {
        context.leviDataStore.edit { prefs ->
            prefs[autoVerifyCacheKey] = enabled
        }
    }

    suspend fun setPreferDirectApkLoad(enabled: Boolean) {
        context.leviDataStore.edit { prefs ->
            prefs[preferDirectApkLoadKey] = enabled
        }
    }

    suspend fun saveServers(servers: List<SavedServer>) {
        context.leviDataStore.edit { prefs ->
            prefs[serversJsonKey] = encodeServers(servers)
        }
    }

    suspend fun appendLaunchHistory(entry: LaunchHistoryEntry) {
        context.leviDataStore.edit { prefs ->
            val current = decodeLaunchHistory(prefs[launchHistoryJsonKey])
            val updated = (listOf(entry) + current).take(50)
            prefs[launchHistoryJsonKey] = encodeLaunchHistory(updated)
        }
    }

    suspend fun clearLaunchHistory() {
        context.leviDataStore.edit { prefs ->
            prefs.remove(launchHistoryJsonKey)
        }
    }

    private fun encodeServers(servers: List<SavedServer>): String {
        val array = JSONArray()
        for (server in servers) {
            val obj = JSONObject()
            obj.put("id", server.id)
            obj.put("label", server.label)
            obj.put("host", server.host)
            obj.put("port", server.port)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeServers(raw: String?): List<SavedServer> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val host = obj.optString("host", "").trim()
                    if (host.isNotEmpty()) {
                        add(
                            SavedServer(
                                id = obj.optString("id", "$host:${obj.optInt("port", 19132)}"),
                                label = obj.optString("label", host),
                                host = host,
                                port = obj.optInt("port", 19132)
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun encodeLaunchHistory(entries: List<LaunchHistoryEntry>): String {
        val array = JSONArray()
        for (item in entries) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("packageName", item.packageName)
            obj.put("versionName", item.versionName)
            obj.put("mode", item.mode.name)
            obj.put("succeeded", item.succeeded)
            obj.put("usedFallback", item.usedFallback)
            obj.put("mainLibLoadSource", item.mainLibLoadSource)
            obj.put("summary", item.summary)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeLaunchHistory(raw: String?): List<LaunchHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkg = obj.optString("packageName", "")
                    if (pkg.isNotBlank()) {
                        val mode = runCatching {
                            LaunchMode.valueOf(obj.optString("mode", LaunchMode.DIRECT_NATIVE.name))
                        }.getOrDefault(LaunchMode.DIRECT_NATIVE)
                        add(
                            LaunchHistoryEntry(
                                id = obj.optString("id", i.toString()),
                                timestamp = obj.optString("timestamp", ""),
                                packageName = pkg,
                                versionName = obj.optString("versionName", "unknown"),
                                mode = mode,
                                succeeded = obj.optBoolean("succeeded", false),
                                usedFallback = obj.optBoolean("usedFallback", false),
                                mainLibLoadSource = obj.optString("mainLibLoadSource", "UNKNOWN"),
                                summary = obj.optString("summary", "")
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
