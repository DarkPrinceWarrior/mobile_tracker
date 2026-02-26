package com.example.mobile_tracker.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val userRole: String = "",
    val scopeType: String = "",
    val scopeIds: List<String> = emptyList(),
    val isLoggedIn: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val serverBaseUrl: String = "",
)

class UserPreferencesManager(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val SCOPE_TYPE = stringPreferencesKey("scope_type")
        val SCOPE_IDS = stringPreferencesKey("scope_ids")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val LAST_SYNC = longPreferencesKey("last_sync_timestamp")
        val SERVER_URL = stringPreferencesKey("server_base_url")
    }

    val userPreferences: Flow<UserPreferences> =
        context.dataStore.data.map { prefs ->
            UserPreferences(
                userId = prefs[Keys.USER_ID] ?: "",
                userEmail = prefs[Keys.USER_EMAIL] ?: "",
                userName = prefs[Keys.USER_NAME] ?: "",
                userRole = prefs[Keys.USER_ROLE] ?: "",
                scopeType = prefs[Keys.SCOPE_TYPE] ?: "",
                scopeIds = prefs[Keys.SCOPE_IDS]?.let {
                    json.decodeFromString<List<String>>(it)
                } ?: emptyList(),
                isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
                lastSyncTimestamp = prefs[Keys.LAST_SYNC] ?: 0L,
                serverBaseUrl = prefs[Keys.SERVER_URL] ?: "",
            )
        }

    suspend fun saveUserData(
        userId: String,
        email: String,
        name: String,
        role: String,
        scopeType: String,
        scopeIds: List<String>,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_ROLE] = role
            prefs[Keys.SCOPE_TYPE] = scopeType
            prefs[Keys.SCOPE_IDS] = json.encodeToString(scopeIds)
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    suspend fun updateServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = url
        }
    }

    suspend fun updateLastSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC] = timestamp
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
