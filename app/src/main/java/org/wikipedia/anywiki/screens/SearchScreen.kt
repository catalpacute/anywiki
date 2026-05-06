package org.wikipedia.anywiki.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.anywiki.components.EmptyState
import org.wikipedia.anywiki.mediawiki.WikiSearchResult
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun SearchScreen(
    activeWiki: WikiSiteConfig?,
    query: String,
    searching: Boolean,
    results: List<WikiSearchResult>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenArticle: (String) -> Unit
) {
    if (activeWiki == null) {
        EmptyState(
            title = "未选择来源",
            message = "请先在“来源”里选择一个 Wiki 站点。"
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜索 ${activeWiki.siteName}") },
                singleLine = true
            )
            androidx.compose.material3.Button(onClick = onSearch, enabled = !searching) {
                Text("搜索")
            }
        }

        if (searching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (results.isEmpty()) {
            EmptyState(
                title = "暂无搜索结果",
                message = "可以尝试更短的关键词，或检查当前来源是否可访问。"
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results, key = { it.title }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenArticle(item.title) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = item.title, color = WikipediaTheme.colors.primaryColor)
                        if (!item.snippet.isNullOrBlank()) {
                            Text(
                                text = item.snippet,
                                color = WikipediaTheme.colors.secondaryColor,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SearchScreenPreview() {
    BaseTheme {
        SearchScreen(
            activeWiki = WikiSiteConfig(
                id = "1",
                siteName = "MediaWiki.org",
                baseUrl = "https://www.mediawiki.org",
                origin = "https://www.mediawiki.org",
                apiUrl = "https://www.mediawiki.org/w/api.php"
            ),
            query = "reader",
            searching = false,
            results = listOf(
                WikiSearchResult("AnyWiki Reader", "A sample snippet")
            ),
            onQueryChange = {},
            onSearch = {},
            onOpenArticle = {}
        )
    }
}
