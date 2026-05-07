package org.wikipedia.anywiki

import android.net.Uri
import androidx.core.net.toUri
import org.wikipedia.WikipediaApp
import org.wikipedia.anywiki.mediawiki.MediaWikiClient
import org.wikipedia.anywiki.mediawiki.MediaWikiDiscover
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.PrefsIoUtil
import org.wikipedia.staticdata.MainPageNameData
import work.czzzz.anywiki.R
import java.util.UUID

object WikiSourceRepository {
    private const val DEFAULT_ARTICLE_PATH = "/wiki/\$1"

    fun getSources(): List<WikiSiteConfig> {
        val stored = JsonUtil.decodeFromString<List<WikiSiteConfig>>(
            PrefsIoUtil.getString(R.string.preference_key_anywiki_sources_json, null)
        ).orEmpty()
        if (stored.isNotEmpty()) {
            return stored
        }
        val defaults = listOf(buildDefaultSource())
        saveSources(defaults, defaults.first().id)
        return defaults
    }

    fun getActiveSource(): WikiSiteConfig {
        val sources = getSources()
        val activeId = PrefsIoUtil.getString(R.string.preference_key_anywiki_active_source_id, null)
        return sources.firstOrNull { it.id == activeId } ?: sources.first().also {
            PrefsIoUtil.setString(R.string.preference_key_anywiki_active_source_id, it.id)
        }
    }

    fun getActiveWikiSite(): WikiSite {
        return WikiSite(getActiveSource().baseUrl)
    }

    fun setActiveSource(sourceId: String) {
        if (getSources().any { it.id == sourceId }) {
            PrefsIoUtil.setString(R.string.preference_key_anywiki_active_source_id, sourceId)
            WikipediaApp.instance.resetWikiSite()
        }
    }

    fun addSource(source: WikiSiteConfig): Boolean {
        val sources = getSources().toMutableList()
        if (sources.any { normalizeComparableBaseUrl(it.baseUrl) == normalizeComparableBaseUrl(source.baseUrl) }) {
            return false
        }
        sources.add(source)
        saveSources(sources, PrefsIoUtil.getString(R.string.preference_key_anywiki_active_source_id, null) ?: source.id)
        WikipediaApp.instance.resetWikiSite()
        return true
    }

    fun removeSource(sourceId: String) {
        val updated = getSources().filterNot { it.id == sourceId }
        val finalSources = if (updated.isEmpty()) listOf(buildDefaultSource()) else updated
        val currentActiveId = PrefsIoUtil.getString(R.string.preference_key_anywiki_active_source_id, null)
        val nextActiveId = if (finalSources.none { it.id == currentActiveId }) finalSources.first().id else currentActiveId
        saveSources(finalSources, nextActiveId)
        WikipediaApp.instance.resetWikiSite()
    }

    fun updateSource(updatedSource: WikiSiteConfig) {
        val current = getSources().toMutableList()
        val index = current.indexOfFirst { it.id == updatedSource.id }
        if (index >= 0) {
            current[index] = updatedSource
            saveSources(current, PrefsIoUtil.getString(R.string.preference_key_anywiki_active_source_id, null) ?: updatedSource.id)
            WikipediaApp.instance.resetWikiSite()
        }
    }

    fun findMatchingSource(uri: Uri?): WikiSiteConfig? {
        if (uri == null) {
            return null
        }
        val normalizedUri = ensureScheme(uri)
        val authority = normalizedUri.authority.orEmpty().lowercase()
        val path = normalizedUri.path.orEmpty()
        val sourceCandidates = getSources()
        return sourceCandidates.firstOrNull { source ->
            val sourceUri = ensureScheme(source.baseUrl.toUri())
            if (authority != sourceUri.authority.orEmpty().lowercase()) {
                return@firstOrNull false
            }

            val basePath = sourceUri.path.orEmpty().trimEnd('/')
            val articlePrefix = source.articlePath.substringBefore("\$1").trimEnd('/')
            val apiPath = ensureScheme(source.apiUrl.toUri()).path.orEmpty()
            val indexPath = apiPath.substringBeforeLast("/", "").let { prefix ->
                if (prefix.isBlank()) "/index.php" else "$prefix/index.php"
            }

            path == basePath ||
                (basePath.isNotBlank() && path.startsWith("$basePath/")) ||
                (articlePrefix.isNotBlank() && path.startsWith(articlePrefix)) ||
                path == apiPath ||
                path == indexPath ||
                (!normalizedUri.getQueryParameter("title").isNullOrBlank() && (path == indexPath || path == basePath))
        }
    }

    fun resolveRestBaseUrl(site: WikiSite): String? {
        return findMatchingSource(site.uri)?.restBaseUrl
    }

    fun supportsRestSummary(site: WikiSite): Boolean {
        return findMatchingSource(site.uri)?.supportsRestSummary ?: true
    }

    fun supportsMobileHtml(site: WikiSite): Boolean {
        return findMatchingSource(site.uri)?.supportsMobileHtml ?: true
    }

    fun resolveApiUrl(site: WikiSite): String {
        return findMatchingSource(site.uri)?.apiUrl ?: "${site.url().trimEnd('/')}/w/api.php"
    }

    fun resolveArticlePath(site: WikiSite): String {
        return findMatchingSource(site.uri)?.articlePath ?: DEFAULT_ARTICLE_PATH
    }

    fun resolveMainPageTitle(site: WikiSite): String {
        return findMatchingSource(site.uri)?.mainPageTitle
            ?: MainPageNameData.valueFor(site.languageCode.ifBlank { WikipediaApp.instance.appOrSystemLanguageCode })
    }

    fun resolveSiteName(site: WikiSite): String? {
        return findMatchingSource(site.uri)?.displayName
    }

    fun resolveLogoUrl(site: WikiSite): String? {
        return findMatchingSource(site.uri)?.logoUrl
    }

    fun resolveBaseUrl(site: WikiSite): String {
        return findMatchingSource(site.uri)?.baseUrl ?: site.uri.toString().trimEnd('/')
    }

    suspend fun discoverAndCreateSource(baseUrl: String, client: MediaWikiClient = MediaWikiClient()): WikiSiteConfig? {
        return when (val result = MediaWikiDiscover.discover(baseUrl, client)) {
            is org.wikipedia.anywiki.mediawiki.DiscoveryResult.Success -> result.config
            else -> null
        }
    }

    private fun saveSources(sources: List<WikiSiteConfig>, activeSourceId: String?) {
        PrefsIoUtil.setString(
            R.string.preference_key_anywiki_sources_json,
            JsonUtil.encodeToString(sources)
        )
        PrefsIoUtil.setString(R.string.preference_key_anywiki_active_source_id, activeSourceId)
    }

    private fun buildDefaultSource(): WikiSiteConfig {
        val languageCode = WikipediaApp.instance.appOrSystemLanguageCode
        val baseUrl = "https://${languageCode}.wikipedia.org"
        return WikiSiteConfig(
            id = UUID.randomUUID().toString(),
            displayName = "Wikipedia",
            baseUrl = baseUrl,
            apiUrl = "$baseUrl/w/api.php",
            articlePath = DEFAULT_ARTICLE_PATH,
            mainPageTitle = MainPageNameData.valueFor(languageCode),
            logoUrl = null,
            supportsLangCode = false,
            restBaseUrl = "$baseUrl/api/rest_v1",
            supportsRestSummary = true,
            supportsMobileHtml = true
        )
    }

    private fun normalizeComparableBaseUrl(url: String): String {
        return MediaWikiDiscover.normalizeBaseUrl(url)?.baseUrl?.lowercase() ?: url.trim().trimEnd('/').lowercase()
    }

    private fun ensureScheme(uri: Uri): Uri {
        return if (uri.scheme.isNullOrBlank()) {
            uri.buildUpon().scheme(WikiSite.DEFAULT_SCHEME).build()
        } else {
            uri
        }
    }
}
