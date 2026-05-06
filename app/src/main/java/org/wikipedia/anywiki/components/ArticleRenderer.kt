package org.wikipedia.anywiki.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleRenderer(
    modifier: Modifier = Modifier,
    baseUrl: String,
    htmlContent: String? = null,
    fallbackUrl: String? = null,
    fontScale: Float,
    darkMode: Boolean,
    onLinkClicked: (String) -> Boolean
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.apply {
                    cacheMode = WebSettings.LOAD_DEFAULT
                    javaScriptEnabled = false
                    builtInZoomControls = false
                    displayZoomControls = false
                    loadsImagesAutomatically = true
                    allowFileAccess = false
                    domStorageEnabled = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        return if (url.isBlank()) false else onLinkClicked(url)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        val target = url.orEmpty()
                        return if (target.isBlank()) false else onLinkClicked(target)
                    }
                }
            }
        },
        update = { webView ->
            val payloadSignature = listOf(
                baseUrl,
                htmlContent.orEmpty(),
                fallbackUrl.orEmpty(),
                fontScale.toString(),
                darkMode.toString()
            ).joinToString("|")
            if (webView.tag == payloadSignature) {
                return@AndroidView
            }
            webView.tag = payloadSignature

            if (!fallbackUrl.isNullOrBlank()) {
                webView.loadUrl(fallbackUrl)
            } else if (!htmlContent.isNullOrBlank()) {
                webView.loadDataWithBaseURL(
                    baseUrl,
                    wrapArticleHtml(htmlContent, fontScale, darkMode),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    )
}

private fun wrapArticleHtml(content: String, fontScale: Float, darkMode: Boolean): String {
    val background = if (darkMode) "#121212" else "#FFFFFF"
    val text = if (darkMode) "#ECECEC" else "#161616"
    val link = if (darkMode) "#93C5FD" else "#1D4ED8"
    val muted = if (darkMode) "#A3A3A3" else "#666666"
    val scaledPercent = (fontScale * 100).toInt().coerceIn(80, 160)

    return """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <style>
                body {
                    margin: 0;
                    padding: 16px;
                    background: $background;
                    color: $text;
                    font-size: ${scaledPercent}%;
                    line-height: 1.6;
                    word-break: break-word;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                table {
                    max-width: 100%;
                    display: block;
                    overflow-x: auto;
                }
                a { color: $link; text-decoration: none; }
                p, li, td, th, span { color: $text; }
                .reference, .mw-editsection { color: $muted; }
            </style>
        </head>
        <body>$content</body>
        </html>
    """.trimIndent()
}
