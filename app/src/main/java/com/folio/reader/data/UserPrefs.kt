package com.folio.reader.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore by preferencesDataStore(name = "folio_user_prefs")

data class UserPrefs(
    val onboardingComplete: Boolean = false,
    val name: String = "",
    val annualGoal: Int = 24,
)

private object UserPrefKeys {
    val ONBOARDED = booleanPreferencesKey("onboarded")
    val NAME = stringPreferencesKey("name")
    val ANNUAL_GOAL = intPreferencesKey("annual_goal")
}

class UserPrefsRepository(private val context: Context) {
    val prefsFlow: Flow<UserPrefs> = context.userDataStore.data.map { p ->
        UserPrefs(
            onboardingComplete = p[UserPrefKeys.ONBOARDED] ?: false,
            name = p[UserPrefKeys.NAME] ?: "",
            annualGoal = p[UserPrefKeys.ANNUAL_GOAL] ?: 24,
        )
    }

    suspend fun update(prefs: UserPrefs) {
        context.userDataStore.edit { p ->
            p[UserPrefKeys.ONBOARDED] = prefs.onboardingComplete
            p[UserPrefKeys.NAME] = prefs.name
            p[UserPrefKeys.ANNUAL_GOAL] = prefs.annualGoal
        }
    }
}
