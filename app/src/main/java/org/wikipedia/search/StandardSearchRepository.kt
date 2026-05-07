package org.wikipedia.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.anywiki.WikiSourceRepository
import org.wikipedia.anywiki.mediawiki.MediaWikiClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil

class StandardSearchRepository : SearchRepository<StandardSearchResults> {
    override suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>
    ): StandardSearchResults {
        val wikiSite = WikiSourceRepository.getActiveWikiSite()
        val resultList = mutableListOf<SearchResult>()
        var response: MwQueryResponse? = null
        var currentContinuation = continuation
        var lastXSearchIdPrefix: String? = null
        var lastXSearchIdFullText: String? = null
        val sourceConfig = WikiSourceRepository.findMatchingSource(wikiSite.uri)
        val useActionApiSearchFallback = sourceConfig != null && !sourceConfig.apiUrl.contains("/w/api.php")

        if (useActionApiSearchFallback) {
            if (isPrefixSearch && searchTerm.length >= 2 && invokeSource != Constants.InvokeSource.PLACES) {
                withContext(Dispatchers.IO) {
                    listOf(async {
                        getSearchResultsFromTabs(wikiSite, searchTerm)
                    }, async {
                        AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(wikiSite, searchTerm)
                    }, async {
                        AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(wikiSite, searchTerm)
                    }).awaitAll().forEach {
                        resultList.addAll(it.results.take(1))
                    }
                }
            }

            val searchResults = MediaWikiClient().search(sourceConfig.apiUrl, searchTerm).getOrElse { emptyList() }
            resultList.addAll(searchResults.mapIndexed { index, result ->
                SearchResult(
                    pageTitle = PageTitle(result.title, wikiSite),
                    searchResultType = SearchResult.SearchResultType.FULL_TEXT,
                    snippet = result.snippet,
                    indexInApiCall = index + 1
                )
            })
            countsPerLanguageCode.clear()
            return StandardSearchResults(
                results = resultList.distinctBy { it.pageTitle.prefixedText }.toMutableList(),
                continuation = null
            )
        }

        if (isPrefixSearch) {
            if (searchTerm.length >= 2 && invokeSource != Constants.InvokeSource.PLACES) {
                withContext(Dispatchers.IO) {
                    listOf(async {
                        getSearchResultsFromTabs(wikiSite, searchTerm)
                    }, async {
                        AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(wikiSite, searchTerm)
                    }, async {
                        AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(wikiSite, searchTerm)
                    }).awaitAll().forEach {
                        resultList.addAll(it.results.take(1))
                    }
                }
            }
            val prefixResponse = ServiceFactory.get(wikiSite).prefixSearchResponse(searchTerm, batchSize, 0)
            lastXSearchIdPrefix = prefixResponse.headers()["x-search-id"]
            response = prefixResponse.body()
            currentContinuation = 0
        }

        resultList.addAll(SearchResultsViewModel.buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.PREFIX))

        if (resultList.size < batchSize) {
            val fullTextResponse = ServiceFactory.get(wikiSite)
                .fullTextSearchResponse(searchTerm, batchSize, currentContinuation)
            lastXSearchIdFullText = fullTextResponse.headers()["x-search-id"]
            response = fullTextResponse.body()
            currentContinuation = response?.continuation?.gsroffset
            resultList.addAll(SearchResultsViewModel.buildList(response, invokeSource, wikiSite, SearchResult.SearchResultType.FULL_TEXT))
        }

        countsPerLanguageCode.clear()

        return StandardSearchResults(
            results = resultList.distinctBy { it.pageTitle.prefixedText }.toMutableList(),
            continuation = currentContinuation,
            xSearchIdPrefix = lastXSearchIdPrefix,
            xSearchIdFullText = lastXSearchIdFullText
        )
    }

    private fun getSearchResultsFromTabs(wikiSite: WikiSite, searchTerm: String): SearchResults {
        for (tab in WikipediaApp.instance.tabList) {
            val title = tab.backStackPositionTitle ?: continue
            if (wikiSite == title.wikiSite && StringUtil.fromHtml(title.displayText).contains(searchTerm, true)) {
                return SearchResults(mutableListOf(SearchResult(title, SearchResult.SearchResultType.TAB_LIST)))
            }
        }
        return SearchResults()
    }
}

data class StandardSearchResults(
    var results: List<SearchResult>,
    val continuation: Int?,
    val xSearchIdPrefix: String? = null,
    val xSearchIdFullText: String? = null
)
