package org.wikipedia.anywiki.storage

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import org.wikipedia.anywiki.mediawiki.CachedArticle
import org.wikipedia.anywiki.mediawiki.WikiArticle

class ArticleCache(context: Context) {
    private val prefs = context.getSharedPreferences(ANYWIKI_PREFS, Context.MODE_PRIVATE)

    fun get(siteId: String, title: String): WikiArticle? {
        return getEntries()
            .firstOrNull { it.siteId == siteId && it.title == title }
            ?.article
    }

    fun put(siteId: String, title: String, article: WikiArticle) {
        val latest = CachedArticle(
            siteId = siteId,
            title = title,
            article = article,
            cachedAt = System.currentTimeMillis()
        )
        val updated = buildList {
            add(latest)
            addAll(getEntries().filterNot { it.siteId == siteId && it.title == title })
        }.take(MAX_CACHED_ARTICLES)
        saveEntries(updated)
    }

    private fun getEntries(): List<CachedArticle> {
        val raw = prefs.getString(KEY_ARTICLE_CACHE, null) ?: return emptyList()
        return runCatching {
            storageJson.decodeFromString(ListSerializer(CachedArticle.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun saveEntries(entries: List<CachedArticle>) {
        val payload = storageJson.encodeToString(ListSerializer(CachedArticle.serializer()), entries)
        prefs.edit().putString(KEY_ARTICLE_CACHE, payload).apply()
    }

    companion object {
        private const val KEY_ARTICLE_CACHE = "article_cache"
        private const val MAX_CACHED_ARTICLES = 30
    }
}
