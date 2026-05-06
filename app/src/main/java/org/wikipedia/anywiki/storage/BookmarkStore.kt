package org.wikipedia.anywiki.storage

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import org.wikipedia.anywiki.mediawiki.SavedArticle

class BookmarkStore(context: Context) {
    private val prefs = context.getSharedPreferences(ANYWIKI_PREFS, Context.MODE_PRIVATE)

    fun getBookmarks(): List<SavedArticle> {
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            storageJson.decodeFromString(ListSerializer(SavedArticle.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun toggle(article: SavedArticle): List<SavedArticle> {
        val current = getBookmarks().toMutableList()
        val existingIndex = current.indexOfFirst { it.siteId == article.siteId && it.title == article.title }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(0, article)
        }
        save(current)
        return current
    }

    fun contains(siteId: String, title: String): Boolean {
        return getBookmarks().any { it.siteId == siteId && it.title == title }
    }

    private fun save(bookmarks: List<SavedArticle>) {
        val payload = storageJson.encodeToString(ListSerializer(SavedArticle.serializer()), bookmarks)
        prefs.edit().putString(KEY_BOOKMARKS, payload).apply()
    }

    companion object {
        private const val KEY_BOOKMARKS = "bookmarks"
    }
}
