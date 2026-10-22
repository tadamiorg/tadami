package com.sf.tadami.ui.components.modal

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
}
