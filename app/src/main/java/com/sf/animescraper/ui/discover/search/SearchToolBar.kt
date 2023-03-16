package com.sf.animescraper.ui.discover.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.sf.animescraper.R
import com.sf.animescraper.ui.base.widgets.topbar.ActionItem
import com.sf.animescraper.ui.components.toolbar.Action
import com.sf.animescraper.ui.utils.AppKeyboardFocusManager
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun SearchToolBar(
    modifier: Modifier = Modifier,
    title: String,
    onBackClicked: () -> Unit,
    onCancelActionMode: () -> Unit,
    onSearchClicked: () -> Unit,
    onSearch: () -> Unit,
    onUpdateSearch: (value : String) -> Unit,
    searchEnabled: Boolean = false

) {

    Column(
        modifier = modifier,
    ) {
        if (searchEnabled) {
            SearchToolBarExpanded(onCollapse = onCancelActionMode, onUpdateSearch = onUpdateSearch, onSearch = onSearch)
        } else {
            SearchToolBarCollapsed(
                title = title,
                onBackClicked = onBackClicked,
                onExpand = onSearchClicked
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchToolBarExpanded(
    onCollapse: () -> Unit,
    onSearch: () -> Unit,
    onUpdateSearch: (value : String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    BackHandler {
        onCollapse()
    }

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            AppKeyboardFocusManager()
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            val searchAndClearFocus: () -> Unit = f@{
                if (value.isBlank()) return@f
                onSearch()
                focusManager.clearFocus()
                keyboardController?.hide()
            }

            val resetAndFocus: () -> Unit = {
                value = ""
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = value,
                onValueChange = { newText ->
                    onUpdateSearch(newText)
                    value = newText
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
        navigationIcon = {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    contentDescription = null,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchToolBarCollapsed(
    title: String,
    onBackClicked: () -> Unit,
    onExpand: () -> Unit
) {
    val actions = remember {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_search,
                onClick = onExpand
            )
        )
    }

    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        actions = {
            actions.forEach { action ->
                ActionItem(action = action)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}








