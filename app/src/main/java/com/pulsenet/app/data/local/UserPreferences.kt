package com.pulsenet.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pulseNetDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsenet_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_ALIAS = "PulseNet User"
        private val KEY_ALIAS = stringPreferencesKey("user_alias")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
    }

    private val dataStore get() = context.pulseNetDataStore

    val alias: Flow<String> = dataStore.data.map { it[KEY_ALIAS] ?: DEFAULT_ALIAS }
    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun getAliasSnapshot(): String = alias.first()

    suspend fun setAlias(alias: String) {
        dataStore.edit { it[KEY_ALIAS] = alias }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = complete }
    }
}
