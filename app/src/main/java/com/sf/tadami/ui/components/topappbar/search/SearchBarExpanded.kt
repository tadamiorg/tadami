package com.sf.tadami.ui.components.topappbar.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.ActionItem
import com.sf.tadami.ui.utils.AppKeyboardFocusManager
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchBarExpanded(
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    onSearchCancel: () -> Unit,
    onSearch: (value: String) -> Unit,
    onSearchChange: (value: String) -> Unit,
    actions: List<Action> = emptyList(),
    backHandlerEnabled : Boolean = true,
    value : String
) {
    var initialFocus by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    BackHandler(backHandlerEnabled) {
        onSearchCancel()
    }

    if (!initialFocus && value.isEmpty()) {
        LaunchedEffect(focusRequester) {
            delay(0.1.seconds)
            focusRequester.requestFocus()
            initialFocus = true
        }
    }

    TopAppBar(
        colors = colors,
        title = {
            AppKeyboardFocusManager()
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            val searchAndClearFocus: () -> Unit = f@{
                if (value.isBlank()) return@f
                onSearch(value)
                focusManager.clearFocus()
                keyboardController?.hide()
            }

            val resetAndFocus: () -> Unit = {
                onSearchChange("")
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = value,
                onValueChange = { newText ->
                    onSearchChange(newText)
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                placeholder = { Text(text = stringResource(id = R.string.discover_search_screen_search_placeholder)) },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchAndClearFocus() }),
                trailingIcon = {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = resetAndFocus) {
                            Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                        }
                    }
                }
            )
        },
        actions = {
            actions.forEach {
                ActionItem(action = it)
            }
        },
        navigationIcon = {
            IconButton(onClick = onSearchCancel) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    tint = if(backHandlerEnabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground,
                    contentDescription = null,
                )
            }
        },
    )
}