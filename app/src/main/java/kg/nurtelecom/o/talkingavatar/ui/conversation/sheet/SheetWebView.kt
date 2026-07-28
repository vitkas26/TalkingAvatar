package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// HTML-ответ Нурай. Ссылки (<a href>) НЕ открываются внутри — перехватываем и отдаём наверх
// (onLinkClick), чтобы открыть их отдельным webview в шите (см. SheetContent.Web).
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlAnswerWebView(html: String, onLinkClick: (String) -> Unit, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        onLinkClick(request.url.toString())
                        return true // не грузим внутри — открываем в отдельном webview
                    }
                }
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
    )
}

// Деталь по ссылке. Обычный webview, грузит url целиком (навигация внутри разрешена).
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UrlWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
            }
        },
        update = { if (it.url != url) it.loadUrl(url) },
    )
}
