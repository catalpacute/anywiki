package org.wikipedia.anywiki.storage

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig

class WikiStore(context: Context) {
    private val prefs = context.getSharedPreferences(ANYWIKI_PREFS, Context.MODE_PRIVATE)

    fun getWikis(): List<WikiSiteConfig> {
        val raw = prefs.getString(KEY_WIKIS, null) ?: return emptyList()
        return runCatching {
            storageJson.decodeFromString(ListSerializer(WikiSiteConfig.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun saveWikis(wikis: List<WikiSiteConfig>) {
        val payload = storageJson.encodeToString(ListSerializer(WikiSiteConfig.serializer()), wikis)
        prefs.edit().putString(KEY_WIKIS, payload).apply()
    }

    fun getActiveWikiId(): String? = prefs.getString(KEY_ACTIVE_WIKI_ID, null)

    fun setActiveWikiId(wikiId: String?) {
        prefs.edit().putString(KEY_ACTIVE_WIKI_ID, wikiId).apply()
    }

    fun addWiki(config: WikiSiteConfig): List<WikiSiteConfig> {
        val current = getWikis().toMutableList()
        if (current.none { it.apiUrl.equals(config.apiUrl, ignoreCase = true) }) {
            current.add(config)
            saveWikis(current)
        }
        if (getActiveWikiId().isNullOrBlank()) {
            setActiveWikiId(config.id)
        }
        return current
    }

    fun removeWiki(wikiId: String): List<WikiSiteConfig> {
        val updated = getWikis().filterNot { it.id == wikiId }
        saveWikis(updated)
        if (getActiveWikiId() == wikiId) {
            setActiveWikiId(updated.firstOrNull()?.id)
        }
        return updated
    }

    companion object {
        private const val KEY_WIKIS = "wikis"
        private const val KEY_ACTIVE_WIKI_ID = "active_wiki_id"
    }
}
