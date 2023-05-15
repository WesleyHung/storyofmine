package com.unlone.app.android.viewmodel

import androidx.lifecycle.ViewModel
import com.unlone.app.data.userPreference.UnloneLocale
import com.unlone.app.data.userPreference.UserPreferenceRepository
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


data class SettingUiState(
    val loading: Boolean = true,
    val errorMsg: String? = null,
    val currentLocale: UnloneLocale? = null
)


class SettingsViewModel(
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {

    private val _state: MutableStateFlow<SettingUiState> = MutableStateFlow(SettingUiState())
    val state = _state.asStateFlow()

    fun refreshData() {
        _state.value = _state.value.copy(currentLocale = userPreferenceRepository.getLocale())
    }

    fun switchLocale(locale: UnloneLocale) {
        userPreferenceRepository.setLocale(locale)
        refreshData()
    }
}