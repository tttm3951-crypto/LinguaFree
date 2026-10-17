package org.linguafree.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.linguafree.core.data.AppBootstrapper
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.repository.ContentStatus
import org.linguafree.core.domain.repository.SettingsRepository
import javax.inject.Inject

sealed interface StartupState {
    data object Loading : StartupState
    data object Ready : StartupState
    data class Failed(val errors: List<String>) : StartupState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val bootstrapper: AppBootstrapper,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            _state.value = when (val s = bootstrapper.start()) {
                is ContentStatus.Ready -> StartupState.Ready
                is ContentStatus.Invalid -> StartupState.Failed(s.errors)
            }
        }
    }

    fun onResume() {
        if (_state.value == StartupState.Ready) viewModelScope.launch { bootstrapper.refresh() }
    }
}
