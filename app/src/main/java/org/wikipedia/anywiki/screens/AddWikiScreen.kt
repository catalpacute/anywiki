package org.wikipedia.anywiki.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme

@Composable
fun AddWikiScreen(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = input,
            onValueChange = onInputChange,
            label = { Text("Wiki 站点地址") },
            placeholder = { Text("例如 https://example.com 或 https://example.com/wiki") },
            singleLine = true
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubmit,
            enabled = !isLoading
        ) {
            Text(if (isLoading) "正在验证..." else "添加来源")
        }
    }
}

@Preview
@Composable
private fun AddWikiScreenPreview() {
    BaseTheme {
        AddWikiScreen(
            input = "https://example.com/wiki",
            isLoading = false,
            onInputChange = {},
            onSubmit = {}
        )
    }
}
