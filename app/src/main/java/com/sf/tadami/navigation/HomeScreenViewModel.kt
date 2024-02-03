package com.sf.tadami.navigation

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeScreenViewModel : ViewModel() {
    val bottomNavDisplayed = MutableTransitionState(true)

    private val _barHeight = MutableStateFlow(0.dp)
    val barHeight = _barHeight.asStateFlow()

    fun setBarHeight(newValue : Dp){
        if (newValue.value > _barHeight.value.value) {
            _barHeight.update { newValue }
        }
    }
}