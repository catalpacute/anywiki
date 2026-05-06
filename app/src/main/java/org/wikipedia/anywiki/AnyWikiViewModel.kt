package org.wikipedia.anywiki

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.wikipedia.R
import org.wikipedia.anywiki.mediawiki.DiscoverFailure
import org.wikipedia.anywiki.mediawiki.DiscoveryResult
import org.wikipedia.anywiki.mediawiki.MediaWikiClient
import org.wikipedia.anywiki.mediawiki.MediaWikiDiscover
import org.wikipedia.anywiki.mediawiki.ReaderThemeMode
import org.wikipedia.anywiki.mediawiki.SavedArticle
import org.wikipedia.anywiki.mediawiki.WikiArticle
import org.wikipedia.anywiki.mediawiki.WikiSearchResult
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig
import org.wikipedia.anywiki.storage.ArticleCache
import org.wikipedia.anywiki.storage.BookmarkStore
import org.wikipedia.anywiki.storage.HistoryStore
import org.wikipedia.anywiki.storage.ReaderSettingsStore
import org.wikipedia.anywiki.storage.WikiStore

enum class ReaderTab {
    SOURCES,
    SEARCH,
    BOOKMARKS,
    HISTORY,
    SETTINGS
}

data class AnyWikiUiState(
    val wikis: List<WikiSiteConfig> = emptyList(),
    val activeWikiId: String? = null,
    val activeTab: ReaderTab = ReaderTab.SOURCES,
    val addWikiInput: String = "",
    val isDiscoveringWiki: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<WikiSearchResult> = emptyList(),
    val loadingArticle: Boolean = false,
    val currentArticle: WikiArticle? = null,
    val articleFallbackUrl: String? = null,
    val articleErrorMessage: String? = null,
    val bookmarks: List<SavedArticle> = emptyList(),
    val history: List<SavedArticle> = emptyList(),
    val fontScale: Float = ReaderSettingsStore.DEFAULT_FONT_SCALE,
    val themeMode: ReaderThemeMode = ReaderThemeMode.SYSTEM,
    val message: String? = null
)

class AnyWikiViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val wikiStore = WikiStore(appContext)
    private val articleCache = ArticleCache(appContext)
    private val bookmarkStore = BookmarkStore(appContext)
    private val historyStore = HistoryStore(appContext)
    private val settingsStore = ReaderSettingsStore(appContext)
    private val mediaWikiClient = MediaWikiClient()

    private val _uiState = MutableStateFlow(AnyWikiUiState())
    val uiState: StateFlow<AnyWikiUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    fun setActiveTab(tab: ReaderTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateAddWikiInput(value: String) {
        _uiState.update { it.copy(addWikiInput = value) }
    }

    fun updateSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissArticleError() {
        _uiState.update { it.copy(articleErrorMessage = null) }
    }

    fun addWiki() {
        val input = _uiState.value.addWikiInput.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(message = appContext.getString(R.string.anywiki_error_invalid_url)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringWiki = true, message = null) }
            when (val result = MediaWikiDiscover.discover(input, mediaWikiClient)) {
                is DiscoveryResult.Success -> {
                    val current = wikiStore.getWikis()
                    if (current.any { it.apiUrl.equals(result.config.apiUrl, ignoreCase = true) }) {
                        _uiState.update {
                            it.copy(
                                isDiscoveringWiki = false,
                                addWikiInput = "",
                                message = appContext.getString(R.string.anywiki_message_wiki_exists)
                            )
                        }
                        return@launch
                    }
                    wikiStore.addWiki(result.config)
                    wikiStore.setActiveWikiId(result.config.id)
                    refreshCoreState(
                        message = appContext.getString(R.string.anywiki_message_wiki_added, result.config.siteName),
                        clearAddInput = true
                    )
                }
                is DiscoveryResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDiscoveringWiki = false,
                            message = discoveryMessage(result.reason)
                        )
                    }
                }
            }
        }
    }

    fun removeWiki(wikiId: String) {
        wikiStore.removeWiki(wikiId)
        refreshCoreState(message = appContext.getString(R.string.anywiki_message_wiki_removed))
    }

    fun selectWiki(wikiId: String) {
        wikiStore.setActiveWikiId(wikiId)
        _uiState.update {
            it.copy(
                activeWikiId = wikiId,
                searchResults = emptyList(),
                searchQuery = "",
                message = appContext.getString(R.string.anywiki_message_active_source_changed)
            )
        }
    }

    fun search() {
        val activeWiki = activeWiki()
        if (activeWiki == null) {
            _uiState.update { it.copy(message = appContext.getString(R.string.anywiki_error_select_wiki_first)) }
            return
        }
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, message = null) }
            val result = mediaWikiClient.search(activeWiki.apiUrl, query)
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = list.map { item ->
                            item.copy(snippet = item.snippet?.let { snippet -> Jsoup.parse(snippet).text() })
                        }
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        message = appContext.getString(R.string.anywiki_error_search_failed)
                    )
                }
            }
        }
    }

    fun openArticle(title: String, fallbackUrl: String? = null) {
        val activeWiki = activeWiki()
        if (activeWiki == null) {
            _uiState.update { it.copy(message = appContext.getString(R.string.anywiki_error_select_wiki_first)) }
            return
        }

        val cached = articleCache.get(activeWiki.id, title)
        if (cached != null) {
            setArticle(article = cached, fallbackUrl = null, error = null)
            addToHistory(activeWiki, cached)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingArticle = true,
                    articleFallbackUrl = null,
                    articleErrorMessage = null
                )
            }
            mediaWikiClient.parseArticle(activeWiki, title)
                .onSuccess { article ->
                    articleCache.put(activeWiki.id, article.title, article)
                    setArticle(article = article, fallbackUrl = null, error = null)
                    addToHistory(activeWiki, article)
                }
                .onFailure {
                    val fallback = fallbackUrl ?: mediaWikiClient.buildArticleUrl(activeWiki, title)
                    _uiState.update { state ->
                        state.copy(
                            loadingArticle = false,
                            currentArticle = null,
                            articleFallbackUrl = fallback,
                            articleErrorMessage = appContext.getString(R.string.anywiki_error_article_fetch_failed)
                        )
                    }
                }
        }
    }

    fun closeArticle() {
        _uiState.update {
            it.copy(
                currentArticle = null,
                articleFallbackUrl = null,
                articleErrorMessage = null,
                loadingArticle = false
            )
        }
    }

    fun toggleBookmarkForCurrentArticle() {
        val article = _uiState.value.currentArticle ?: return
        val wiki = activeWiki() ?: return
        val saved = SavedArticle(
            siteId = wiki.id,
            siteName = wiki.siteName,
            title = article.title,
            displayTitle = article.displayTitle,
            url = article.canonicalUrl ?: mediaWikiClient.buildArticleUrl(wiki, article.title),
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { it.copy(bookmarks = bookmarkStore.toggle(saved)) }
    }

    fun openSavedArticle(entry: SavedArticle) {
        val knownWiki = wikiStore.getWikis().firstOrNull { it.id == entry.siteId }
        if (knownWiki == null) {
            _uiState.update { it.copy(message = appContext.getString(R.string.anywiki_error_source_missing)) }
            return
        }
        wikiStore.setActiveWikiId(knownWiki.id)
        _uiState.update { it.copy(activeWikiId = knownWiki.id) }
        openArticle(entry.title, entry.url)
    }

    fun onArticleLinkClicked(url: String): Boolean {
        val internalTitle = extractInternalTitle(url) ?: return false
        openArticle(internalTitle, url)
        return true
    }

    fun setThemeMode(mode: ReaderThemeMode) {
        settingsStore.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setFontScale(scale: Float) {
        settingsStore.setFontScale(scale)
        _uiState.update { it.copy(fontScale = settingsStore.getFontScale()) }
    }

    fun isCurrentArticleBookmarked(): Boolean {
        val article = _uiState.value.currentArticle ?: return false
        val wiki = activeWiki() ?: return false
        return bookmarkStore.contains(wiki.id, article.title)
    }

    private fun bootstrap() {
        var wikis = wikiStore.getWikis()
        if (wikis.isEmpty()) {
            val sample = WikiSiteConfig(
                id = SAMPLE_WIKI_ID,
                siteName = "MediaWiki.org (Sample)",
                baseUrl = "https://www.mediawiki.org",
                origin = "https://www.mediawiki.org",
                apiUrl = "https://www.mediawiki.org/w/api.php",
                articlePath = "/wiki/\$1",
                logoUrl = null
            )
            wikis = listOf(sample)
            wikiStore.saveWikis(wikis)
            wikiStore.setActiveWikiId(sample.id)
        }

        val activeId = wikiStore.getActiveWikiId() ?: wikis.firstOrNull()?.id
        if (activeId != null) {
            wikiStore.setActiveWikiId(activeId)
        }
        _uiState.value = _uiState.value.copy(
            wikis = wikis,
            activeWikiId = activeId,
            bookmarks = bookmarkStore.getBookmarks(),
            history = historyStore.getHistory(),
            fontScale = settingsStore.getFontScale(),
            themeMode = settingsStore.getThemeMode()
        )
    }

    private fun refreshCoreState(message: String? = null, clearAddInput: Boolean = false) {
        val wikis = wikiStore.getWikis()
        val activeId = wikiStore.getActiveWikiId() ?: wikis.firstOrNull()?.id
        _uiState.update {
            it.copy(
                wikis = wikis,
                activeWikiId = activeId,
                isDiscoveringWiki = false,
                addWikiInput = if (clearAddInput) "" else it.addWikiInput,
                bookmarks = bookmarkStore.getBookmarks(),
                history = historyStore.getHistory(),
                message = message
            )
        }
    }

    private fun setArticle(article: WikiArticle, fallbackUrl: String?, error: String?) {
        _uiState.update {
            it.copy(
                loadingArticle = false,
                currentArticle = article,
                articleFallbackUrl = fallbackUrl,
                articleErrorMessage = error
            )
        }
    }

    private fun addToHistory(wiki: WikiSiteConfig, article: WikiArticle) {
        val saved = SavedArticle(
            siteId = wiki.id,
            siteName = wiki.siteName,
            title = article.title,
            displayTitle = article.displayTitle,
            url = article.canonicalUrl ?: mediaWikiClient.buildArticleUrl(wiki, article.title),
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { it.copy(history = historyStore.add(saved)) }
    }

    private fun discoveryMessage(failure: DiscoverFailure): String = when (failure) {
        DiscoverFailure.INVALID_URL -> appContext.getString(R.string.anywiki_error_invalid_url)
        DiscoverFailure.NOT_MEDIAWIKI -> appContext.getString(R.string.anywiki_error_not_mediawiki)
        DiscoverFailure.API_UNREACHABLE -> appContext.getString(R.string.anywiki_error_api_unreachable)
        DiscoverFailure.API_DISABLED -> appContext.getString(R.string.anywiki_error_api_disabled)
        DiscoverFailure.NETWORK_OFFLINE -> appContext.getString(R.string.anywiki_error_network_offline)
    }

    private fun activeWiki(): WikiSiteConfig? {
        val state = _uiState.value
        return state.wikis.firstOrNull { it.id == state.activeWikiId }
    }

    private fun extractInternalTitle(url: String): String? {
        val wiki = activeWiki() ?: return null
        if (url.startsWith("#")) {
            return null
        }
        val resolved = if (url.startsWith("/")) {
            wiki.origin.trimEnd('/') + url
        } else {
            url
        }
        val uri = runCatching { Uri.parse(resolved) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (!host.equals(Uri.parse(wiki.origin).host, ignoreCase = true)) {
            return null
        }

        val titleFromQuery = uri.getQueryParameter("title")
        if (!titleFromQuery.isNullOrBlank()) {
            return titleFromQuery.replace('_', ' ')
        }

        val path = uri.path.orEmpty()
        val marker = "/wiki/"
        val wikiIndex = path.indexOf(marker)
        if (wikiIndex < 0) {
            return null
        }
        val rawTitle = path.substring(wikiIndex + marker.length)
        if (rawTitle.isBlank()) {
            return null
        }
        return Uri.decode(rawTitle).replace('_', ' ')
    }

    companion object {
        private const val SAMPLE_WIKI_ID = "sample-mediawiki-org"
    }
}
