package org.wikipedia.anywiki.storage

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import org.wikipedia.anywiki.mediawiki.SavedArticle

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(ANYWIKI_PREFS, Context.MODE_PRIVATE)

    fun getHistory(): List<SavedArticle> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            storageJson.decodeFromString(ListSerializer(SavedArticle.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun add(article: SavedArticle): List<SavedArticle> {
        val updated = buildList {
            add(article)
            addAll(getHistory().filterNot { it.siteId == article.siteId && it.title == article.title })
        }.take(MAX_HISTORY_COUNT)
        save(updated)
        return updated
    }

    private fun save(history: List<SavedArticle>) {
        val payload = storageJson.encodeToString(ListSerializer(SavedArticle.serializer()), history)
        prefs.edit().putString(KEY_HISTORY, payload).apply()
    }

    companion object {
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY_COUNT = 60
    }
}
