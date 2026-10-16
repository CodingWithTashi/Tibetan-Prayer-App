package com.codingwithtashi.dailyprayer.ui.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.codingwithtashi.dailyprayer.core.init.AppInitializer
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.locale.AppLocale
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface AppRootState {
    data object Loading : AppRootState
    data class Ready(val onboarded: Boolean, val appearance: Appearance, val language: AppLanguage, val textSize: AppTextSize) : AppRootState
}

@HiltViewModel
class AppRootViewModel @Inject constructor(
    initializer: AppInitializer,
    prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<AppRootState> =
        combine(initializer.ready, prefsRepository.prefs) { ready, prefs ->
            if (!ready) {
                AppRootState.Loading
            } else {
                val language = if (AppCompatDelegate.getApplicationLocales().isEmpty) prefs.appLanguage else AppLocale.current()
                AppRootState.Ready(prefs.onboarded, prefs.appearance, language, prefs.appTextSize)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppRootState.Loading)
}
