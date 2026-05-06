package org.wikipedia.anywiki.mediawiki

import java.net.ConnectException
import java.net.URI
import java.net.UnknownHostException
import java.util.UUID
import javax.net.ssl.SSLException

object MediaWikiDiscover {
    suspend fun discover(baseUrlInput: String, client: MediaWikiClient): DiscoveryResult {
        val normalized = normalizeBaseUrl(baseUrlInput) ?: return DiscoveryResult.Failure(DiscoverFailure.INVALID_URL)
        val candidates = candidateApiUrls(normalized)

        var sawApiDisabled = false
        var sawOffline = false
        var sawMediaWikiLikeError = false

        for (apiUrl in candidates) {
            val result = client.fetchSiteInfo(apiUrl)
            if (result.isSuccess) {
                val general = result.getOrThrow()
                val siteName = general.sitename?.takeIf { it.isNotBlank() } ?: normalized.host
                val articlePath = general.articlepath?.takeIf { it.contains("\$1") } ?: "/wiki/\$1"
                return DiscoveryResult.Success(
                    WikiSiteConfig(
                        id = UUID.randomUUID().toString(),
                        siteName = siteName,
                        baseUrl = normalized.baseUrl,
                        origin = normalized.origin,
                        apiUrl = apiUrl,
                        articlePath = articlePath,
                        logoUrl = resolveLogoUrl(normalized, general.logo)
                    )
                )
            }

            val error = result.exceptionOrNull()
            when {
                error is MediaWikiApiException && error.code.equals("readapidenied", ignoreCase = true) -> sawApiDisabled = true
                error is UnknownHostException || error is ConnectException || error is SSLException -> sawOffline = true
                error is InvalidMediaWikiResponseException -> sawMediaWikiLikeError = true
                error is MediaWikiApiException -> sawMediaWikiLikeError = true
            }
        }

        return when {
            sawApiDisabled -> DiscoveryResult.Failure(DiscoverFailure.API_DISABLED)
            sawOffline -> DiscoveryResult.Failure(DiscoverFailure.NETWORK_OFFLINE)
            sawMediaWikiLikeError -> DiscoveryResult.Failure(DiscoverFailure.NOT_MEDIAWIKI)
            else -> DiscoveryResult.Failure(DiscoverFailure.API_UNREACHABLE)
        }
    }

    internal fun candidateApiUrls(url: NormalizedWikiUrl): List<String> {
        val base = url.baseUrl.trimEnd('/')
        val origin = url.origin.trimEnd('/')
        return listOf(
            "$base/api.php",
            "$origin/api.php",
            "$origin/w/api.php",
            "$origin/wiki/api.php"
        ).distinct()
    }

    internal fun normalizeBaseUrl(input: String): NormalizedWikiUrl? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return try {
            val parsed = URI(withScheme)
            val scheme = parsed.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") {
                return null
            }
            val host = parsed.host ?: return null
            val portSuffix = when {
                parsed.port == -1 -> ""
                else -> ":${parsed.port}"
            }
            val origin = "$scheme://$host$portSuffix"
            val normalizedPath = parsed.path?.trimEnd('/').orEmpty()
            val baseUrl = if (normalizedPath.isBlank()) origin else origin + normalizedPath
            NormalizedWikiUrl(
                baseUrl = baseUrl,
                origin = origin,
                scheme = scheme,
                host = host
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveLogoUrl(url: NormalizedWikiUrl, logo: String?): String? {
        if (logo.isNullOrBlank()) {
            return null
        }
        return when {
            logo.startsWith("https://") || logo.startsWith("http://") -> logo
            logo.startsWith("//") -> "${url.scheme}:$logo"
            logo.startsWith("/") -> url.origin + logo
            else -> url.origin + "/" + logo
        }
    }
}

internal data class NormalizedWikiUrl(
    val baseUrl: String,
    val origin: String,
    val scheme: String,
    val host: String
)
