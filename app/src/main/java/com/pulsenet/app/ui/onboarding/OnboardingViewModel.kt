package com.pulsenet.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsenet.app.data.local.UserPreferences
import com.pulsenet.app.security.KeySigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val keySigner: KeySigner
) : ViewModel() {

    val isOnboardingComplete: StateFlow<Boolean> = userPreferences.isOnboardingComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Reading this triggers KeyManager's identity generation on first launch
    // (it's created lazily in KeyManager's init block the first time Hilt
    // resolves a KeySigner), so this doubles as the "Identity created" proof.
    val publicKeyPreview: String = keySigner.getPublicKeyBase64().take(20) + "…"

    fun completeOnboarding(alias: String, onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setAlias(alias.trim().ifBlank { UserPreferences.DEFAULT_ALIAS })
            userPreferences.setOnboardingComplete(true)
            onDone()
        }
    }
}
