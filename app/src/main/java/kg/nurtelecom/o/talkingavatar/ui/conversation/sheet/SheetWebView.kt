package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

// AndroidView(WebView) не участвует в Compose nested-scroll — ModalBottomSheet (свайп на
// закрытие/ресайз работает поверх сырых pointer-событий, а не только через NestedScrollConnection)
// перехватывает драг поверх вебвью, и скроллится шит вместо страницы. requestDisallowIntercept
// на каждое касание — стандартный Android-приём (тот же, что для WebView/RecyclerView внутри
// ViewPager) — говорит родителям не трогать жест, пока он идёт внутри WebView.
private fun WebView.disallowParentInterceptOnTouch() {
    setOnTouchListener { view, _ ->
        view.parent?.requestDisallowInterceptTouchEvent(true)
        false // событие всё равно обрабатывает сам WebView
    }
}

// Полная (десктопная) версия сайта. Десктопный UA сам по себе не хватает: адаптивные сайты
// решают мобильная/десктопная вёрстка не только по UA, а по CSS-медиа-запросам от ширины
// viewport'а — а useWideViewPort/loadWithOverviewMode лишь переигрывают ЕГО СОБСТВЕННЫЙ
// <meta name=viewport>, который на адаптивной странице обычно и есть "width=device-width"
// (т.е. подстроится под узкий экран несмотря на наши настройки). Поэтому после загрузки
// страницы ещё и подменяем сам meta-тег на фиксированную широкую ширину через JS — ровно то,
// что делает "Версия для ПК" в Chrome.
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/125.0.0.0 Safari/537.36"
private const val DESKTOP_VIEWPORT_WIDTH = 1280
private val FORCE_DESKTOP_VIEWPORT_JS = """
    (function() {
        var meta = document.querySelector('meta[name="viewport"]');
        if (!meta) {
            meta = document.createElement('meta');
            meta.name = 'viewport';
            document.getElementsByTagName('head')[0].appendChild(meta);
        }
        meta.setAttribute('content', 'width=$DESKTOP_VIEWPORT_WIDTH, initial-scale=0.1');
    })();
""".trimIndent()

// Деталь по ссылке — единственное место, где реально нужен настоящий WebView (страница
// произвольного стороннего сайта). HTML-ответ самого Нурай рендерится нативно, см. AnswerText.kt.
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun UrlWebView(url: String, modifier: Modifier = Modifier) {
    // Compose Preview (layoutlib) использует урезанный android.jar без реальной WebView —
    // WebView.getSettings() кидает NoSuchMethodError и валит весь превью. Плейсхолдер вместо неё.
    if (LocalInspectionMode.current) {
        Box(modifier.background(Color(0xFFEEEEEE)))
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.userAgentString = DESKTOP_USER_AGENT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript(FORCE_DESKTOP_VIEWPORT_JS, null)
                    }
                }
                disallowParentInterceptOnTouch()
            }
        },
        update = { if (it.url != url) it.loadUrl(url) },
    )
}
