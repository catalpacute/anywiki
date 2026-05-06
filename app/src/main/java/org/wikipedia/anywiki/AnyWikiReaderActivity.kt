package org.wikipedia.anywiki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.anywiki.components.EmptyState
import org.wikipedia.anywiki.mediawiki.ReaderThemeMode
import org.wikipedia.anywiki.mediawiki.SavedArticle
import org.wikipedia.anywiki.screens.ArticleScreen
import org.wikipedia.anywiki.screens.SearchScreen
import org.wikipedia.anywiki.screens.WikiListScreen
import org.wikipedia.anywiki.storage.ReaderSettingsStore
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

class AnyWikiReaderActivity : BaseActivity() {
    private val viewModel by viewModels<AnyWikiViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val darkMode = isDarkMode(state.themeMode)
            val appTheme = if (darkMode) Theme.DARK else Theme.LIGHT

            LaunchedEffect(state.message) {
                val message = state.message ?: return@LaunchedEffect
                snackbarHostState.showSnackbar(message)
                viewModel.dismissMessage()
            }

            BaseTheme(currentTheme = appTheme) {
                val articleOpen = state.currentArticle != null || state.articleFallbackUrl != null || state.loadingArticle
                if (articleOpen) {
                    ArticleScreen(
                        article = state.currentArticle,
                        fallbackUrl = state.articleFallbackUrl,
                        loading = state.loadingArticle,
                        articleError = state.articleErrorMessage,
                        fontScale = state.fontScale,
                        darkMode = darkMode,
                        isBookmarked = viewModel.isCurrentArticleBookmarked(),
                        onBack = { viewModel.closeArticle() },
                        onToggleBookmark = { viewModel.toggleBookmarkForCurrentArticle() },
                        onDismissArticleError = { viewModel.dismissArticleError() },
                        onLinkClicked = { url ->
                            when {
                                url.startsWith("#") -> false
                                viewModel.onArticleLinkClicked(url) -> true
                                else -> {
                                    openExternalLink(url)
                                    true
                                }
                            }
                        },
                        onOpenExternalFallback = { url -> openExternalLink(url) }
                    )
                } else {
                    ReaderScaffold(
                        state = state,
                        snackbarHostState = snackbarHostState,
                        onTabChange = viewModel::setActiveTab,
                        onAddWikiInputChange = viewModel::updateAddWikiInput,
                        onAddWiki = viewModel::addWiki,
                        onSelectWiki = viewModel::selectWiki,
                        onRemoveWiki = viewModel::removeWiki,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onSearch = viewModel::search,
                        onOpenArticle = { title -> viewModel.openArticle(title) },
                        onOpenSavedArticle = viewModel::openSavedArticle,
                        onThemeModeChange = viewModel::setThemeMode,
                        onFontScaleChange = viewModel::setFontScale
                    )
                }
            }
        }
    }

    private fun openExternalLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    @Composable
    private fun isDarkMode(themeMode: ReaderThemeMode): Boolean {
        val systemDark = isSystemInDarkTheme()
        return when (themeMode) {
            ReaderThemeMode.SYSTEM -> systemDark
            ReaderThemeMode.LIGHT -> false
            ReaderThemeMode.DARK -> true
        }
    }
}

@Composable
private fun ReaderScaffold(
    state: AnyWikiUiState,
    snackbarHostState: SnackbarHostState,
    onTabChange: (ReaderTab) -> Unit,
    onAddWikiInputChange: (String) -> Unit,
    onAddWiki: () -> Unit,
    onSelectWiki: (String) -> Unit,
    onRemoveWiki: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenSavedArticle: (SavedArticle) -> Unit,
    onThemeModeChange: (ReaderThemeMode) -> Unit,
    onFontScaleChange: (Float) -> Unit
) {
    val activeWiki = state.wikis.firstOrNull { it.id == state.activeWikiId }
    val title = when (state.activeTab) {
        ReaderTab.SOURCES -> "Sources"
        ReaderTab.SEARCH -> "Search"
        ReaderTab.BOOKMARKS -> "Bookmarks"
        ReaderTab.HISTORY -> "History"
        ReaderTab.SETTINGS -> "Settings"
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AnyWiki Reader - $title") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            NavigationBar {
                ReaderTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.activeTab == tab,
                        onClick = { onTabChange(tab) },
                        icon = { Text(tab.shortLabel()) },
                        label = { Text(tab.fullLabel()) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.activeTab) {
                ReaderTab.SOURCES -> WikiListScreen(
                    wikis = state.wikis,
                    activeWikiId = state.activeWikiId,
                    addInput = state.addWikiInput,
                    isAdding = state.isDiscoveringWiki,
                    onAddInputChange = onAddWikiInputChange,
                    onAddWiki = onAddWiki,
                    onSelectWiki = onSelectWiki,
                    onRemoveWiki = onRemoveWiki
                )
                ReaderTab.SEARCH -> SearchScreen(
                    activeWiki = activeWiki,
                    query = state.searchQuery,
                    searching = state.isSearching,
                    results = state.searchResults,
                    onQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onOpenArticle = onOpenArticle
                )
                ReaderTab.BOOKMARKS -> SavedArticleListScreen(
                    entries = state.bookmarks,
                    emptyTitle = "No bookmarks yet",
                    emptyMessage = "Bookmark an article while reading to pin it here.",
                    onOpenSavedArticle = onOpenSavedArticle
                )
                ReaderTab.HISTORY -> SavedArticleListScreen(
                    entries = state.history,
                    emptyTitle = "No reading history",
                    emptyMessage = "Opened articles will appear here automatically.",
                    onOpenSavedArticle = onOpenSavedArticle
                )
                ReaderTab.SETTINGS -> ReaderSettingsScreen(
                    fontScale = state.fontScale,
                    themeMode = state.themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onFontScaleChange = onFontScaleChange
                )
            }
        }
    }
}

@Composable
private fun SavedArticleListScreen(
    entries: List<SavedArticle>,
    emptyTitle: String,
    emptyMessage: String,
    onOpenSavedArticle: (SavedArticle) -> Unit
) {
    if (entries.isEmpty()) {
        EmptyState(title = emptyTitle, message = emptyMessage)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(entries, key = { "${it.siteId}:${it.title}" }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSavedArticle(item) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = item.displayTitle.ifBlank { item.title }, color = WikipediaTheme.colors.primaryColor)
                    Text(
                        text = item.siteName,
                        color = WikipediaTheme.colors.secondaryColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsScreen(
    fontScale: Float,
    themeMode: ReaderThemeMode,
    onThemeModeChange: (ReaderThemeMode) -> Unit,
    onFontScaleChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Font size", color = WikipediaTheme.colors.primaryColor)
        Slider(
            value = fontScale,
            valueRange = ReaderSettingsStore.MIN_FONT_SCALE..ReaderSettingsStore.MAX_FONT_SCALE,
            onValueChange = onFontScaleChange
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeModeButton(
                text = "System",
                selected = themeMode == ReaderThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ReaderThemeMode.SYSTEM) }
            )
            ThemeModeButton(
                text = "Light",
                selected = themeMode == ReaderThemeMode.LIGHT,
                onClick = { onThemeModeChange(ReaderThemeMode.LIGHT) }
            )
            ThemeModeButton(
                text = "Dark",
                selected = themeMode == ReaderThemeMode.DARK,
                onClick = { onThemeModeChange(ReaderThemeMode.DARK) }
            )
        }
    }
}

@Composable
private fun ThemeModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "* $text" else "o $text",
            color = WikipediaTheme.colors.primaryColor
        )
    }
}

private fun ReaderTab.shortLabel(): String = when (this) {
    ReaderTab.SOURCES -> "S"
    ReaderTab.SEARCH -> "Q"
    ReaderTab.BOOKMARKS -> "B"
    ReaderTab.HISTORY -> "H"
    ReaderTab.SETTINGS -> "Cfg"
}

private fun ReaderTab.fullLabel(): String = when (this) {
    ReaderTab.SOURCES -> "Source"
    ReaderTab.SEARCH -> "Search"
    ReaderTab.BOOKMARKS -> "Bookmark"
    ReaderTab.HISTORY -> "History"
    ReaderTab.SETTINGS -> "Settings"
}
