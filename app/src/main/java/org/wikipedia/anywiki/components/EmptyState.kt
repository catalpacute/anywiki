package org.wikipedia.anywiki.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, color = WikipediaTheme.colors.primaryColor)
        Text(
            text = message,
            color = WikipediaTheme.colors.secondaryColor,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    BaseTheme {
        EmptyState(
            title = "暂无内容",
            message = "请先添加一个 Wiki 来源",
            actionLabel = "添加来源"
        )
    }
}
