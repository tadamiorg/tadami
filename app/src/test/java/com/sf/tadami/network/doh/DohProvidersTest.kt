package com.sf.tadami.network.doh

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DohProvidersTest {

    @Test
    fun everyProviderBuildsAResolver() {
        val client = OkHttpClient()
        // A malformed address or URL would throw here
        DohProviders.all.forEach { provider ->
            assertNotNull(provider.name, DohProviders.dns(provider.id, client))
        }
    }

    @Test
    fun theIdsAreUniqueAndKeepMihonsValues() {
        // Stored in the settings: changing one would switch the users to another provider
        assertEquals((1..12).toList(), DohProviders.all.map { it.id })
        assertEquals("Cloudflare", DohProviders.all.first { it.id == 1 }.name)
    }

    @Test
    fun disabledOrUnknownHasNoResolver() {
        val client = OkHttpClient()
        assertNull(DohProviders.dns(DohProviders.DISABLED, client))
        assertNull(DohProviders.dns(99, client))
    }
}
