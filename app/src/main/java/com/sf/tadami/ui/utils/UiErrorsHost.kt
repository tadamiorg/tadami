package com.sf.tadami.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R

/**
 * Renders [UiErrors] events as a snackbar over whatever screen is showing, with a Retry action
 * when the failed operation can be repeated.
 *
 * Installed once around every activity's content (see `setComposeContent`) so no screen has to
 * opt in.
 */
@Composable
fun UiErrorsHost(content: @Composable () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(Unit) {
        UiErrors.events.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.retry?.let { retryLabel },
                duration = if (event.retry != null) {
                    SnackbarDuration.Long
                } else {
                    SnackbarDuration.Short
                },
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.retry?.invoke()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
