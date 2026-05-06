package org.wikipedia.anywiki.mediawiki

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MediaWikiDiscoverTest {
    @Test
    fun normalizeBaseUrl_addsHttpsAndTrimsPath() {
        val normalized = MediaWikiDiscover.normalizeBaseUrl("example.com/wiki/")
        assertNotNull(normalized)
        assertEquals("https://example.com/wiki", normalized?.baseUrl)
        assertEquals("https://example.com", normalized?.origin)
    }

    @Test
    fun normalizeBaseUrl_rejectsInvalidInput() {
        val normalized = MediaWikiDiscover.normalizeBaseUrl("not a url")
        assertNull(normalized)
    }

    @Test
    fun candidateApiUrls_prefersPathSpecificEndpointFirst() {
        val normalized = MediaWikiDiscover.normalizeBaseUrl("https://example.com/wiki")!!
        val candidates = MediaWikiDiscover.candidateApiUrls(normalized)
        assertEquals(
            listOf(
                "https://example.com/wiki/api.php",
                "https://example.com/api.php",
                "https://example.com/w/api.php"
            ),
            candidates
        )
    }
}
