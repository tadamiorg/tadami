package com.sf.tadami.ui.components.modal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sf.tadami.R
import kotlinx.coroutines.launch

/**
 * A modal of the player, drawn in the composition (never a window, so the immersive mode and the
 * cutout stay as they are). In landscape a side panel at the end,
 * 40% of the width (320 to 420dp), the full height; in portrait at the bottom, at most 60% of the
 * height. A light scrim on the video, a tap on it closes the panel, none on the [clearScrim] pages, so
 * their changes can be seen on the video. Segmented tabs for 2 pages or more.
 */
@Composable
fun PlayerPanel(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    tabs: List<String> = emptyList(),
    clearScrim: (page: Int) -> Boolean = { false },
    content: @Composable (page: Int) -> Unit,
) {
    if (visible) BackHandler(onBack = onDismissRequest)
    val pageCount = tabs.size.coerceAtLeast(1)
    val pagerState = rememberPagerState { pageCount }
    val scrimAlpha = if (clearScrim(pagerState.currentPage)) 0f else 0.2f
    val scrimColor by animateColorAsState(
        targetValue = if (visible) Color.Black.copy(alpha = scrimAlpha) else Color.Transparent,
        animationSpec = tween(250),
        label = "scrim",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().zIndex(5f)) {
        val landscape = maxWidth > maxHeight
        val width = maxWidth
        val height = maxHeight
        if (visible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
            )
        }
        val enterState = remember { MutableTransitionState(false) }
        enterState.targetState = visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = if (landscape) Alignment.CenterEnd else Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visibleState = enterState,
                enter = if (landscape) {
                    slideFromEndEnter()
                } else {
                    fadeIn(tween(250)) + slideInVertically(tween(250, easing = EmphasizedDecelerate)) { it / 2 }
                },
                exit = if (landscape) {
                    fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { it / 2 }
                } else {
                    fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 }
                },
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    // surfaceContainer is nearly black on pure black, as the player always is
                    color = (if (isPureBlack()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
                        .copy(alpha = 0.92f),
                    border = modalBorder(),
                    // The surface keeps its taps from the scrim under it
                    modifier = modifier
                        .then(
                            if (landscape) {
                                Modifier
                                    .padding(12.dp)
                                    .width((width * 0.4f).coerceIn(320.dp, 420.dp))
                                    .fillMaxHeight()
                            } else {
                                Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = height * 0.6f)
                            },
                        ),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                        ) {
                            IconButton(onClick = onDismissRequest) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                            }
                            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        }
                        if (ModalRules.showsTabRow(tabs.size)) {
                            val scope = rememberCoroutineScope()
                            PanelTabs(tabs, pagerState.currentPage) { scope.launch { pagerState.animateScrollToPage(it) } }
                        }
                        HorizontalPager(
                            state = pagerState,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.weight(1f, fill = false),
                        ) { page ->
                            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                                content(page)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTabs(tabs: List<String>, page: Int, onPage: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = page == index,
                onClick = { onPage(index) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                label = { Text(tab, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** A choice of the player, applied at once: the selected one a tonal pill with a trailing check. */
@Composable
fun PlayerChoiceRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    dimmed: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val baseColor = LocalContentColor.current
    val textColor = if (dimmed) baseColor.copy(alpha = 0.6f) else baseColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .heightIn(min = ModalDefaults.RowHeight)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
            }
        }
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            it()
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(24.dp))
        }
    }
}
