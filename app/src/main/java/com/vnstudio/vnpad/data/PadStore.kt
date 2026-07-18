package com.vnstudio.vnpad.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vnstudio.vnpad.model.GridSettings
import com.vnstudio.vnpad.model.Pad
import com.vnstudio.vnpad.net.ConnectionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vnpad")

private val KEY_PADS = stringPreferencesKey("pads_json")
private val KEY_CONN = stringPreferencesKey("conn_json")
private val KEY_SETTINGS = stringPreferencesKey("grid_settings_json")

/**
 * Persists the pad layout and the last paired host. JSON-encoded in a single
 * Preferences DataStore; small enough that whole-list rewrites are fine.
 */
class PadStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val pads: Flow<List<Pad>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PADS] ?: return@map defaultPads()
        runCatching { json.decodeFromString<List<Pad>>(raw) }.getOrElse { defaultPads() }
    }

    val connection: Flow<ConnectionInfo?> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CONN] ?: return@map null
        runCatching { json.decodeFromString<StoredConn>(raw).toInfo() }.getOrNull()
    }

    val settings: Flow<GridSettings> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SETTINGS] ?: return@map GridSettings()
        runCatching { json.decodeFromString<GridSettings>(raw) }.getOrElse { GridSettings() }
    }

    suspend fun savePads(pads: List<Pad>) {
        val encoded = json.encodeToString(pads)
        context.dataStore.edit { it[KEY_PADS] = encoded }
    }

    suspend fun saveSettings(settings: GridSettings) {
        val encoded = json.encodeToString(settings)
        context.dataStore.edit { it[KEY_SETTINGS] = encoded }
    }

    suspend fun saveConnection(info: ConnectionInfo) {
        val encoded = json.encodeToString(StoredConn(info.ip, info.port, info.token))
        context.dataStore.edit { it[KEY_CONN] = encoded }
    }
}

@kotlinx.serialization.Serializable
private data class StoredConn(val ip: String, val port: Int, val token: String) {
    fun toInfo() = ConnectionInfo(ip, port, token)
}
