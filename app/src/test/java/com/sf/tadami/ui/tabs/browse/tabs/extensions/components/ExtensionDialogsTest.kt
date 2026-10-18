package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionDialogsTest {

    @Test
    fun theFingerprintIsUppercaseInGroupsOfFour() {
        assertEquals(
            "11DE C208 A209 773E FDC5 348A F5DE 709C A9AF 067E 9154 D1DD 3C6A BDB0 FA3C C200",
            groupedFingerprint("11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200"),
        )
    }
}
