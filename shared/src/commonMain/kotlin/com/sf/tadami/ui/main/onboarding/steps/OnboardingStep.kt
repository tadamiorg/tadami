package com.sf.tadami.ui.main.onboarding.steps

import androidx.compose.runtime.Composable

public interface OnboardingStep {

    val isComplete: Boolean

    @Composable
    fun Content()
}