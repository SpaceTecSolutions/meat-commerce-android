package com.spacetecsolutions.meatapp.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.spacetecsolutions.meatapp.core.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DataStoreAppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferences {
    override val darkThemeEnabled: Flow<Boolean?> =
        dataStore.data.map { it[DARK_THEME] }

    override val lastAuthenticatedRole: Flow<UserRole?> =
        dataStore.data.map { preferences ->
            preferences[LAST_ROLE]?.let { stored ->
                UserRole.entries.firstOrNull { it.name == stored }
            }
        }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        dataStore.edit { it[DARK_THEME] = enabled }
    }

    override suspend fun setLastAuthenticatedRole(role: UserRole?) {
        dataStore.edit { preferences ->
            if (role == null) preferences.remove(LAST_ROLE) else preferences[LAST_ROLE] = role.name
        }
    }

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LAST_ROLE = stringPreferencesKey("last_authenticated_role")
    }
}
