package org.wikipedia.anywiki.mediawiki

import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaWikiClientUrlTest {
    private val client = MediaWikiClient(httpClient = OkHttpClient())

    @Test
    fun buildActionUrl_encodesQueryAndAddsDefaultFormat() {
        val url = client.buildActionUrl(
            apiUrl = "https://example.com/w/api.php",
            params = mapOf(
                "action" to "parse",
                "page" to "A B/C?D"
            )
        )

        assertTrue(url.contains("action=parse"))
        assertTrue(url.contains("page=A%20B%2FC%3FD"))
        assertTrue(url.contains("format=json"))
        assertTrue(url.contains("formatversion=2"))
    }

    @Test
    fun buildArticleUrl_usesArticlePathTemplate() {
        val site = WikiSiteConfig(
            id = "id",
            displayName = "site",
            baseUrl = "https://example.com/wiki",
            apiUrl = "https://example.com/api.php",
            articlePath = "/custom/\$1"
        )

        val url = client.buildArticleUrl(site, "Main Page")
        assertTrue(url == "https://example.com/custom/Main_Page")
    }
}
