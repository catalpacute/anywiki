package org.wikipedia.anywiki.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun WikiCard(
    wiki: WikiSiteConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WikipediaTheme.colors.paperColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wiki.siteName,
                    color = WikipediaTheme.colors.primaryColor
                )
                wiki.logoUrl?.let { logo ->
                    AsyncImage(
                        model = logo,
                        contentDescription = wiki.siteName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = wiki.baseUrl,
                color = WikipediaTheme.colors.secondaryColor,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = wiki.apiUrl,
                color = WikipediaTheme.colors.secondaryColor,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActive) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                        Text("当前来源")
                    }
                } else {
                    Button(onClick = onSelect, modifier = Modifier.weight(1f)) {
                        Text("设为当前")
                    }
                }
                OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) {
                    Text("移除")
                }
            }
        }
    }
}

@Preview
@Composable
private fun WikiCardPreview() {
    BaseTheme {
        WikiCard(
            wiki = WikiSiteConfig(
                id = "sample",
                siteName = "MediaWiki.org (示例)",
                baseUrl = "https://www.mediawiki.org",
                origin = "https://www.mediawiki.org",
                apiUrl = "https://www.mediawiki.org/w/api.php"
            ),
            isActive = true,
            onSelect = {},
            onRemove = {}
        )
    }
}
