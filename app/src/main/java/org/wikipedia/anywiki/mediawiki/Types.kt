package org.wikipedia.anywiki.mediawiki

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.util.UriUtil

@Serializable
data class WikiSiteConfig(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val apiUrl: String,
    val articlePath: String = "/wiki/\$1",
    val mainPageTitle: String = "Main Page",
    val logoUrl: String? = null,
    val supportsLangCode: Boolean = false,
    val restBaseUrl: String? = null,
    val supportsRestSummary: Boolean = false,
    val supportsMobileHtml: Boolean = false
) {
    @Transient
    val siteName = displayName

    @Transient
    val origin: String = run {
        val scheme = UriUtil.decodeURL(baseUrl).substringBefore("://", "https")
        val withoutScheme = baseUrl.substringAfter("://", baseUrl)
        val authority = withoutScheme.substringBefore("/")
        "$scheme://$authority"
    }
}

typealias WikiSource = WikiSiteConfig

@Serializable
data class WikiSearchResult(
    val title: String,
    val snippet: String? = null
)

@Serializable
data class WikiSection(
    val index: Int,
    val anchor: String,
    val line: String
)

@Serializable
data class WikiArticle(
    val title: String,
    val displayTitle: String,
    val html: String,
    val sections: List<WikiSection>,
    val canonicalUrl: String? = null
)

@Serializable
data class SavedArticle(
    val siteId: String,
    val siteName: String,
    val title: String,
    val displayTitle: String,
    val url: String,
    val timestamp: Long
)

@Serializable
data class CachedArticle(
    val siteId: String,
    val title: String,
    val article: WikiArticle,
    val cachedAt: Long
)

@Serializable
enum class ReaderThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

sealed interface DiscoveryResult {
    data class Success(val config: WikiSiteConfig) : DiscoveryResult
    data class Failure(val reason: DiscoverFailure) : DiscoveryResult
}

enum class DiscoverFailure {
    INVALID_URL,
    NOT_MEDIAWIKI,
    API_UNREACHABLE,
    API_DISABLED,
    NETWORK_OFFLINE
}

@Serializable
data class SiteInfoResponse(
    val query: SiteInfoQuery? = null,
    val error: ApiError? = null
)

@Serializable
data class SiteInfoQuery(
    val general: SiteGeneral? = null
)

@Serializable
data class SiteGeneral(
    val sitename: String? = null,
    val base: String? = null,
    val server: String? = null,
    val logo: String? = null,
    val articlepath: String? = null,
    val mainpage: String? = null,
    val lang: String? = null
)

@Serializable
data class SearchResponse(
    val query: SearchQueryData? = null,
    val error: ApiError? = null
)

@Serializable
data class SearchQueryData(
    val search: List<SearchResultData> = emptyList()
)

@Serializable
data class SearchResultData(
    val title: String,
    val snippet: String? = null
)

@Serializable
data class ParseResponse(
    val parse: ParseData? = null,
    val error: ApiError? = null
)

@Serializable
data class ParseData(
    val title: String? = null,
    val displaytitle: String? = null,
    val text: String? = null,
    val sections: List<ParseSectionData> = emptyList()
)

@Serializable
data class ParseSectionData(
    val index: String? = null,
    val anchor: String? = null,
    val line: String? = null
)

@Serializable
data class ApiError(
    val code: String? = null,
    val info: String? = null
)
