package org.wikipedia.anywiki.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wikipedia.anywiki.components.EmptyState
import org.wikipedia.anywiki.components.WikiCard
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig

@Composable
fun WikiListScreen(
    wikis: List<WikiSiteConfig>,
    activeWikiId: String?,
    addInput: String,
    isAdding: Boolean,
    onAddInputChange: (String) -> Unit,
    onAddWiki: () -> Unit,
    onSelectWiki: (String) -> Unit,
    onRemoveWiki: (String) -> Unit
) {
    if (wikis.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AddWikiScreen(
                input = addInput,
                isLoading = isAdding,
                onInputChange = onAddInputChange,
                onSubmit = onAddWiki
            )
            EmptyState(
                title = "还没有来源",
                message = "先添加一个 MediaWiki 站点，就可以开始搜索和阅读了。"
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AddWikiScreen(
                input = addInput,
                isLoading = isAdding,
                onInputChange = onAddInputChange,
                onSubmit = onAddWiki
            )
        }
        items(wikis, key = { it.id }) { wiki ->
            WikiCard(
                wiki = wiki,
                isActive = wiki.id == activeWikiId,
                onSelect = { onSelectWiki(wiki.id) },
                onRemove = { onRemoveWiki(wiki.id) }
            )
        }
    }
}
