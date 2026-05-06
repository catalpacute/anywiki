package org.wikipedia.anywiki.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wikipedia.anywiki.components.ArticleRenderer
import org.wikipedia.anywiki.components.EmptyState
import org.wikipedia.anywiki.mediawiki.WikiArticle
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun ArticleScreen(
    article: WikiArticle?,
    fallbackUrl: String?,
    loading: Boolean,
    articleError: String?,
    fontScale: Float,
    darkMode: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDismissArticleError: () -> Unit,
    onLinkClicked: (String) -> Boolean,
    onOpenExternalFallback: (String) -> Unit
) {
    var tocExpanded by remember { mutableStateOf(false) }
    val title = article?.displayTitle ?: "文章"

    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = title,
                onNavigationClick = onBack,
                actions = {
                    IconButton(onClick = onToggleBookmark) {
                        Text(if (isBookmarked) "已收藏" else "收藏")
                    }
                    if (!article?.sections.isNullOrEmpty()) {
                        IconButton(onClick = { tocExpanded = true }) {
                            Text("目录")
                        }
                    }
                    DropdownMenu(expanded = tocExpanded, onDismissRequest = { tocExpanded = false }) {
                        article?.sections.orEmpty().forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.line) },
                                onClick = {
                                    tocExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!articleError.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = articleError,
                        color = WikipediaTheme.colors.secondaryColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (!fallbackUrl.isNullOrBlank()) {
                        Button(onClick = { onOpenExternalFallback(fallbackUrl) }) {
                            Text("浏览器打开")
                        }
                    }
                    Button(
                        onClick = onDismissArticleError,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("关闭")
                    }
                }
            }

            if (loading) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (article == null && fallbackUrl.isNullOrBlank()) {
                EmptyState(title = "无法加载文章", message = "请返回搜索页后重试。")
                return@Column
            }

            ArticleRenderer(
                modifier = Modifier.fillMaxSize(),
                baseUrl = fallbackUrl ?: article?.canonicalUrl.orEmpty(),
                htmlContent = article?.html,
                fallbackUrl = if (article == null) fallbackUrl else null,
                fontScale = fontScale,
                darkMode = darkMode,
                onLinkClicked = onLinkClicked
            )
        }
    }
}
