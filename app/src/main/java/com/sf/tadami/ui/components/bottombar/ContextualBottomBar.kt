package com.sf.tadami.ui.components.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.components.topappbar.ActionItem
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.utils.clickableNoIndication

@Composable
fun ContextualBottomBar(
    visible: Boolean,
    actions: List<Action>
) {
    val visibleState = remember { MutableTransitionState(visible) }

    visibleState.targetState = visible

    var dismissingBottomBar by rememberSaveable { mutableStateOf(false) }

    if (!visibleState.currentState && visibleState.isIdle) {
        dismissingBottomBar = false
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickableNoIndication {  },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            actions.forEach { action ->
                ActionItem(action = action, enabled = !dismissingBottomBar && action.enabled) {
                    dismissingBottomBar = true
                }
            }
        }
    }
}