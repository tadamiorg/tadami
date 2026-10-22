package com.sf.tadami.ui.components.modal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModalRulesTest {

    @Test
    fun aSinglePageHasNoTabRow() {
        assertFalse(ModalRules.showsTabRow(0))
        assertFalse(ModalRules.showsTabRow(1))
        assertTrue(ModalRules.showsTabRow(2))
        assertTrue(ModalRules.showsTabRow(3))
    }

    @Test
    fun anUntouchedInputDialogClosesOnAnOutsideTap() {
        assertTrue(ModalRules.inputDismissesOnClickOutside(text = "", initialValue = "", busy = false))
        assertTrue(ModalRules.inputDismissesOnClickOutside(text = "Watching", initialValue = "Watching", busy = false))
    }

    @Test
    fun typedTextIsKeptOnAnOutsideTap() {
        assertFalse(ModalRules.inputDismissesOnClickOutside(text = "https://", initialValue = "", busy = false))
        // Changed, emptied included: typed over
        assertFalse(ModalRules.inputDismissesOnClickOutside(text = "Watchin", initialValue = "Watching", busy = false))
        assertFalse(ModalRules.inputDismissesOnClickOutside(text = "", initialValue = "Watching", busy = false))
    }

    @Test
    fun aBusyInputDialogIgnoresOutsideTaps() {
        assertFalse(ModalRules.inputDismissesOnClickOutside(text = "", initialValue = "", busy = true))
        assertFalse(ModalRules.inputDismissesOnClickOutside(text = "https://github.com/a/b", initialValue = "", busy = true))
    }

    @Test
    fun aSingleChoiceAppliesOnlyAChange() {
        assertTrue(ModalRules.singleChoiceApplies(selected = 1, picked = 2))
        assertTrue(ModalRules.singleChoiceApplies(selected = null, picked = 2))
        assertFalse(ModalRules.singleChoiceApplies(selected = 2, picked = 2))
    }

    @Test
    fun thePagesTakeTheTallestPageHeight() {
        assertEquals(0, ModalRules.tallestPage(emptyMap(), pageCount = 3))
        // A short page doesn't shrink them
        assertEquals(480, ModalRules.tallestPage(mapOf(0 to 120, 1 to 480, 2 to 240), pageCount = 3))
        // A page that is gone, after a tab was removed, no longer counts
        assertEquals(240, ModalRules.tallestPage(mapOf(0 to 240, 1 to 900), pageCount = 1))
    }
}
