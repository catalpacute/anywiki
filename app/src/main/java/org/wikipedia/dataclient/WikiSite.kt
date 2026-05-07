package org.wikipedia.dataclient

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.anywiki.WikiSourceRepository
import org.wikipedia.json.UriSerializer
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.util.UriUtil

/**
 * Represents a concrete MediaWiki source.
 *
 * In the original app this object mostly identified a language family under wikipedia.org.
 * For AnyWiki Reader we keep the same public shape, but allow the underlying URI to carry a
 * full site base URL including path prefixes such as `https://example.com/wiki`.
 */
@Serializable
@Parcelize
data class WikiSite(
    @SerialName("domain") @Serializable(with = UriSerializer::class) var uri: Uri,
    var languageCode: String = ""
) : Parcelable {

    constructor(uri: Uri) : this(resolveUri(uri))

    private constructor(resolvedSite: ResolvedSite) : this(resolvedSite.uri, resolvedSite.languageCode)

    init {
        uri = ensureScheme(uri)
        val normalizedPath = uri.path?.trimEnd('/').orEmpty()
        uri = if (normalizedPath.isBlank()) {
            Uri.Builder()
                .scheme(uri.scheme)
                .encodedAuthority(uri.authority)
                .build()
        } else {
            uri.buildUpon().encodedPath(normalizedPath).build()
        }
    }

    constructor(url: String) : this(
        when {
            url.startsWith("http") -> Uri.parse(url)
            url.startsWith("//") -> Uri.parse("$DEFAULT_SCHEME:$url")
            else -> Uri.parse("$DEFAULT_SCHEME://$url")
        }
    )

    constructor(authority: String, languageCode: String) : this(authority) {
        this.languageCode = languageCode
    }

    fun scheme(): String {
        return uri.scheme.orEmpty().ifEmpty { DEFAULT_SCHEME }
    }

    fun authority(): String {
        return uri.authority.orEmpty()
    }

    fun origin(): String {
        return "${scheme()}://${authority()}"
    }

    fun subdomain(): String {
        return if (languageCode.isBlank()) "" else languageCodeToSubdomain(languageCode)
    }

    fun path(segment: String): String {
        val matchedSource = WikiSourceRepository.findMatchingSource(uri)
        val apiPath = matchedSource?.apiUrl?.toUri()?.path.orEmpty()
        val prefix = apiPath.substringBeforeLast("/", "")
        return if (prefix.isBlank()) "/$segment" else "$prefix/$segment"
    }

    fun url(): String {
        return WikiSourceRepository.resolveBaseUrl(this).trimEnd('/')
    }

    fun url(segment: String): String {
        return origin() + path(segment)
    }

    fun apiUrl(): String {
        return WikiSourceRepository.resolveApiUrl(this)
    }

    fun articlePath(): String {
        return WikiSourceRepository.resolveArticlePath(this)
    }

    fun restBaseUrl(): String? {
        return WikiSourceRepository.resolveRestBaseUrl(this)
    }

    fun supportsRestSummary(): Boolean {
        return WikiSourceRepository.supportsRestSummary(this)
    }

    fun supportsMobileHtml(): Boolean {
        return WikiSourceRepository.supportsMobileHtml(this)
    }

    fun mainPageTitle(): String {
        return WikiSourceRepository.resolveMainPageTitle(this)
    }

    fun siteName(): String? {
        return WikiSourceRepository.resolveSiteName(this)
    }

    fun logoUrl(): String? {
        return WikiSourceRepository.resolveLogoUrl(this)
    }

    fun articleUrl(prefixedTitle: String, fragment: String? = null): String {
        val encodedTitle = UriUtil.encodeURL(prefixedTitle.replace(' ', '_'))
        val resolvedPath = articlePath().replace("\$1", encodedTitle)
        val url = if (resolvedPath.startsWith("http://") || resolvedPath.startsWith("https://")) {
            resolvedPath
        } else {
            origin().trimEnd('/') + "/" + resolvedPath.trimStart('/')
        }
        return if (fragment.isNullOrEmpty()) url else "$url#${UriUtil.encodeURL(fragment)}"
    }

    fun dbName(): String {
        return (if (uri.authority.orEmpty().contains("wikidata")) {
            "wikidata"
        } else if (uri.authority.orEmpty().contains("commons")) {
            "commons"
        } else {
            subdomain().replace("-".toRegex(), "_").ifBlank { authority().substringBefore('.').replace("-", "_") }
        }) + "wiki"
    }

    companion object {
        const val DEFAULT_SCHEME = "https"
        const val BASE_DOMAIN = "wikipedia.org"
        private var DEFAULT_BASE_URL: String? = null

        fun supportedAuthority(authority: String): Boolean {
            val activeAuthority = DEFAULT_BASE_URL?.toUri()?.authority.orEmpty()
            return authority.equals(activeAuthority, ignoreCase = true)
        }

        fun matchesUri(uri: Uri?): Boolean {
            return WikiSourceRepository.findMatchingSource(uri) != null
        }

        fun setDefaultBaseUrl(url: String) {
            DEFAULT_BASE_URL = url.ifEmpty { Service.WIKIPEDIA_URL }
        }

        fun forLanguageCode(languageCode: String): WikiSite {
            val activeSource = WikiSourceRepository.getActiveSource()
            val baseUri = ensureScheme(activeSource.baseUrl.toUri())
            val resolvedLang = if (activeSource.supportsLangCode && languageCode.isNotEmpty()) {
                normalizeLanguageCode(languageCode)
            } else {
                resolveLanguageCode(baseUri, baseUri.authority.orEmpty())
            }
            return WikiSite(baseUri, resolvedLang)
        }

        fun normalizeLanguageCode(languageCode: String): String {
            return when (languageCode) {
                AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE -> AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE
                AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE -> AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE
                AppLanguageLookUpTable.CHINESE_LEGACY_YUE_LANGUAGE_CODE -> AppLanguageLookUpTable.CHINESE_YUE_LANGUAGE_CODE
                else -> languageCode
            }
        }

        fun preview(languageCode: String = "en"): WikiSite {
            return WikiSite("https://$languageCode.wikipedia.org/".toUri(), languageCode)
        }

        private fun languageCodeToSubdomain(languageCode: String): String {
            return WikipediaApp.instance.languageState.getDefaultLanguageCode(languageCode) ?: normalizeLanguageCode(languageCode)
        }

        fun authorityToLanguageCode(authority: String): String {
            val parts = authority.split("\\.".toRegex()).toTypedArray()
            val minLengthForSubdomain = 3
            return if (parts.size < minLengthForSubdomain ||
                parts.size == minLengthForSubdomain && parts[0] == "m"
            ) {
                ""
            } else {
                parts[0]
            }
        }

        private fun ensureScheme(uri: Uri): Uri {
            return if (uri.scheme.isNullOrEmpty()) {
                uri.buildUpon().scheme(DEFAULT_SCHEME).build()
            } else {
                uri
            }
        }

        private fun resolveUri(uri: Uri): ResolvedSite {
            val tempUri = ensureScheme(uri)
            val matchedSource = WikiSourceRepository.findMatchingSource(tempUri)
            val sourceUri = ensureScheme((matchedSource?.baseUrl ?: tempUri.toString()).toUri())
            var authority = sourceUri.authority.orEmpty().replace(".m.", ".")
            if ((BASE_DOMAIN == authority || ("www.$BASE_DOMAIN") == authority) &&
                tempUri.path?.startsWith("/wiki") == true
            ) {
                authority = "en.$BASE_DOMAIN"
            }

            val normalizedPath = when {
                matchedSource != null -> sourceUri.path?.trimEnd('/').orEmpty()
                looksLikeArticlePath(tempUri) -> ""
                else -> sourceUri.path?.trimEnd('/').orEmpty()
            }
            val baseUri = Uri.Builder()
                .scheme(tempUri.scheme)
                .encodedAuthority(authority)
                .encodedPath(if (normalizedPath.isBlank()) null else normalizedPath)
                .build()
            return ResolvedSite(baseUri, resolveLanguageCode(tempUri, authority))
        }

        private fun resolveLanguageCode(uri: Uri, authority: String): String {
            var resolvedLanguageCode = UriUtil.getLanguageVariantFromUri(uri).ifEmpty {
                authorityToLanguageCode(authority)
            }

            val parentLanguageCode = WikipediaApp.instance.languageState.getDefaultLanguageCode(resolvedLanguageCode)
            var languageVariants = WikipediaApp.instance.languageState.getLanguageVariants(resolvedLanguageCode)
            if (parentLanguageCode != null || languageVariants != null) {
                if (languageVariants == null) {
                    languageVariants = WikipediaApp.instance.languageState.getLanguageVariants(parentLanguageCode)
                }
                resolvedLanguageCode = WikipediaApp.instance.languageState.appLanguageCodes.firstOrNull {
                    languageVariants?.contains(it) == true
                } ?: resolvedLanguageCode
            }

            if (resolvedLanguageCode == Constants.WIKI_CODE_COMMONS) {
                resolvedLanguageCode = WikipediaApp.instance.appOrSystemLanguageCode
            }
            return resolvedLanguageCode
        }

        private fun looksLikeArticlePath(uri: Uri): Boolean {
            if (!uri.getQueryParameter("title").isNullOrBlank()) {
                return true
            }
            val path = uri.path.orEmpty()
            return path.matches("^${UriUtil.WIKI_REGEX}.+".toRegex()) || path.endsWith(".php")
        }

        private data class ResolvedSite(val uri: Uri, val languageCode: String)
    }
}
