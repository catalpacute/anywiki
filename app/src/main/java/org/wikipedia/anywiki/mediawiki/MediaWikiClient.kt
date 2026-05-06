package org.wikipedia.anywiki.mediawiki

import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class MediaWikiClient(
    private val httpClient: OkHttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun fetchSiteInfo(apiUrl: String): Result<SiteGeneral> = runCatching {
        val response = request<SiteInfoResponse>(
            apiUrl = apiUrl,
            params = mapOf(
                "action" to "query",
                "meta" to "siteinfo",
                "siprop" to "general|namespaces|statistics"
            )
        )
        response.error?.let { throw MediaWikiApiException(it.code, it.info ?: "MediaWiki API error") }
        response.query?.general ?: throw InvalidMediaWikiResponseException("Missing siteinfo.general")
    }

    suspend fun search(apiUrl: String, query: String): Result<List<WikiSearchResult>> = runCatching {
        val response = request<SearchResponse>(
            apiUrl = apiUrl,
            params = mapOf(
                "action" to "query",
                "list" to "search",
                "srsearch" to query
            )
        )
        response.error?.let { throw MediaWikiApiException(it.code, it.info ?: "MediaWiki API error") }
        response.query?.search.orEmpty().map {
            WikiSearchResult(
                title = it.title,
                snippet = it.snippet
            )
        }
    }

    suspend fun parseArticle(site: WikiSiteConfig, title: String): Result<WikiArticle> = runCatching {
        val response = request<ParseResponse>(
            apiUrl = site.apiUrl,
            params = mapOf(
                "action" to "parse",
                "page" to title,
                "prop" to "text|sections|displaytitle",
                "redirects" to "1"
            )
        )
        response.error?.let { throw MediaWikiApiException(it.code, it.info ?: "MediaWiki API error") }
        val parse = response.parse ?: throw InvalidMediaWikiResponseException("Missing parse result")
        val parsedTitle = parse.title ?: title
        val html = parse.text ?: throw InvalidMediaWikiResponseException("Missing parse.text")
        val displayTitle = parse.displaytitle ?: parsedTitle
        val sections = parse.sections.mapNotNull { section ->
            val index = section.index?.toIntOrNull() ?: return@mapNotNull null
            val anchor = section.anchor ?: return@mapNotNull null
            val line = section.line ?: return@mapNotNull null
            WikiSection(index = index, anchor = anchor, line = line)
        }
        WikiArticle(
            title = parsedTitle,
            displayTitle = displayTitle,
            html = html,
            sections = sections,
            canonicalUrl = buildArticleUrl(site, parsedTitle)
        )
    }

    fun buildActionUrl(apiUrl: String, params: Map<String, String>): String {
        val parsed = apiUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid API URL: $apiUrl")
        val builder = parsed.newBuilder()
        params.forEach { (key, value) ->
            builder.addQueryParameter(key, value)
        }
        if (!params.containsKey("format")) {
            builder.addQueryParameter("format", "json")
        }
        if (!params.containsKey("formatversion")) {
            builder.addQueryParameter("formatversion", "2")
        }
        return builder.build().toString()
    }

    fun buildArticleUrl(site: WikiSiteConfig, title: String): String {
        val encodedTitle = URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
            .replace("+", "%20")
        val resolvedPath = site.articlePath.replace("\$1", encodedTitle)
        return if (resolvedPath.startsWith("http://") || resolvedPath.startsWith("https://")) {
            resolvedPath
        } else {
            site.origin.trimEnd('/') + "/" + resolvedPath.trimStart('/')
        }
    }

    private suspend inline fun <reified T> request(apiUrl: String, params: Map<String, String>): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildActionUrl(apiUrl, params))
            .header("User-Agent", USER_AGENT)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} from ${request.url}")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw IOException("Empty API response body")
            }
            return@withContext json.decodeFromString(body)
        }
    }

    companion object {
        const val USER_AGENT = "AnyWikiReader/0.1 contact: dev@example.com"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}

class MediaWikiApiException(val code: String?, override val message: String) : RuntimeException(message)
class InvalidMediaWikiResponseException(override val message: String) : RuntimeException(message)
