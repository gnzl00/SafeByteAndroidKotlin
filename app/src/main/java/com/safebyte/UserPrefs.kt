package com.safebyte

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "safebyte_prefs")

private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
private val KEY_EMAIL = stringPreferencesKey("logged_email")
private val KEY_ALLERGENS = stringSetPreferencesKey("allergens")

class UserPrefs(private val context: Context) {

    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_LOGGED_IN] ?: false }

    val loggedEmail: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[KEY_EMAIL] ?: "" }

    val allergens: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[KEY_ALLERGENS] ?: emptySet() }

    /** Guarda sesión (log in) con email */
    suspend fun setLoggedIn(email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_EMAIL] = email
        }
    }

    /** Cierra sesión */
    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = false
            prefs.remove(KEY_EMAIL)
        }
    }

    suspend fun setAllergens(values: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALLERGENS] = values
        }
    }
}