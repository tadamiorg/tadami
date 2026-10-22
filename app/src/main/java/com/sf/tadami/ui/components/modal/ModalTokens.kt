package com.sf.tadami.ui.components.modal

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ModalDefaults {
    val MinWidth = 280.dp
    val MaxWidth = 560.dp
    val WindowMargin = 24.dp
    val SheetMaxWidth = 640.dp
    val RowHeight = 48.dp
    val RowHeightWithSupporting = 56.dp
}

/** The rules of the modals, apart from their drawing. */
object ModalRules {
    /** No tab row for a single page: its title says it all. */
    fun showsTabRow(tabCount: Int): Boolean = tabCount > 1

    /** A tap outside an input dialog loses nothing: ignored once the text changed, and while it's checked. */
    fun inputDismissesOnClickOutside(text: String, initialValue: String, busy: Boolean): Boolean =
        !busy && text == initialValue

    /** A single choice applies a pick only when it changes the value, it closes either way. */
    fun <T> singleChoiceApplies(selected: T?, picked: T): Boolean = picked != selected
}

/** Pure black (AMOLED) theme: the containers need an edge. */
@Composable
@ReadOnlyComposable
fun isPureBlack(): Boolean = MaterialTheme.colorScheme.surface == Color.Black

/** The edge of the containers on pure black, of the dialogs always. */
@Composable
@ReadOnlyComposable
fun modalBorder(always: Boolean = false): BorderStroke? =
    if (always || isPureBlack()) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
