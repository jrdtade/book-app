package com.folio.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel(app: FolioApp) : ViewModel() {
    private val repo = app.userPrefsRepository

    /** Null until the first DataStore read completes, so callers can tell "not loaded yet" from "not onboarded". */
    val prefs: StateFlow<UserPrefs?> = repo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding(name: String, annualGoal: Int) {
        viewModelScope.launch {
            repo.update(UserPrefs(onboardingComplete = true, name = name, annualGoal = annualGoal))
        }
    }

    fun update(transform: (UserPrefs) -> UserPrefs) {
        viewModelScope.launch { repo.update(transform(prefs.value ?: UserPrefs())) }
    }
}
