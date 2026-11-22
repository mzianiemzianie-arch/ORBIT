package com.orbit.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import java.net.URLEncoder

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var homeScreen: ScrollView

    private val backgroundColor = Color.rgb(10, 16, 30)
    private val surfaceColor = Color.rgb(21, 31, 49)
    private val accentColor = Color.rgb(0, 210, 190)
    private val secondaryText = Color.rgb(155, 170, 190)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun makeAction(
        label: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(surfaceColor, 16)
            setPadding(dp(15), dp(12), dp(15), dp(12))
            setOnClickListener { action() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        // TOP HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }

        val logo = TextView(this).apply {
            text = "◉"
            textSize = 29f
            setTextColor(accentColor)
            gravity = Gravity.CENTER
        }

        val logoText = TextView(this).apply {
            text = "ORBIT"
            textSize = 21f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), 0, dp(12), 0)
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(logo, LinearLayout.LayoutParams(dp(30), dp(42)))
            addView(logoText)
        }

        addressBar = EditText(this).apply {
            hint = "Search or enter website"
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(secondaryText)
            setPadding(dp(14), 0, dp(12), 0)
            imeOptions = EditorInfo.IME_ACTION_GO
            setBackground(rounded(surfaceColor, 18))
            setSelectAllOnFocus(false)
        }

        val searchButton = TextView(this).apply {
            text = "➜"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(backgroundColor)
            background = rounded(accentColor, 16)
            setOnClickListener { navigate() }
        }

        header.addView(brand)
        header.addView(
            addressBar,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        header.addView(
            searchButton,
            LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                leftMargin = dp(8)
            }
        )

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
            visibility = View.GONE
        }

        // HOME SCREEN
        homeScreen = ScrollView(this).apply {
            setFillViewport(true)
            isVerticalScrollBarEnabled = false
            setBackgroundColor(backgroundColor)
        }

        val homeContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(30), dp(20), dp(24))
        }

        val welcome = makeText("Welcome to", 15f, secondaryText)
        val title = makeText("Your world. Your ORBIT.", 25f, Color.WHITE, true)
        val subtitle = makeText(
            "Explore the internet your way.",
            14f,
            secondaryText
        )

        homeContent.addView(welcome)
        homeContent.addView(title)
        homeContent.addView(subtitle)

        val sectionTitle = makeText(
            "QUICK ACCESS",
            12f,
            accentColor,
            true
        ).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(38), 0, dp(15))
        }

        homeContent.addView(sectionTitle)

        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rowOne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val rowTwo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun addShortcut(
            row: LinearLayout,
            symbol: String,
            name: String,
            url: String
        ) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(surfaceColor, 20)
                setPadding(dp(8), dp(18), dp(8), dp(18))
                isClickable = true
                isFocusable = true
                setOnClickListener { openUrl(url) }
            }

            val icon = makeText(symbol, 25f, accentColor, true).apply {
                gravity = Gravity.CENTER
            }

            val label = makeText(name, 13f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }

            card.addView(icon)
            card.addView(label)

            row.addView(
                card,
                LinearLayout.LayoutParams(0, dp(105), 1f).apply {
                    marginEnd = dp(8)
                }
            )
        }

        addShortcut(rowOne, "D", "DuckDuckGo", "https://duckduckgo.com")
        addShortcut(rowOne, "▶", "YouTube", "https://www.youtube.com")
        addShortcut(rowTwo, "f", "Facebook", "https://www.facebook.com")
        addShortcut(rowTwo, "W", "Wikipedia", "https://www.wikipedia.org")

        shortcuts.addView(rowOne)
        shortcuts.addView(
            rowTwo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        )

        homeContent.addView(shortcuts)

        val privacyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surfaceColor, 20)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val privacyTitle = makeText(
            "✦  Browse with ORBIT",
            16f,
            Color.WHITE,
            true
        )

        val privacyDescription = makeText(
            "A new way to explore the web is waiting for you.",
            13f,
            secondaryText
        )

        privacyCard.addView(privacyTitle)
        privacyCard.addView(
            privacyDescription,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        homeContent.addView(
            privacyCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
            }
        )

        homeScreen.addView(homeContent)

        // WEB VIEW
        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.safeBrowsingEnabled = true

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: android.graphics.Bitmap?
                ) {
                    homeScreen.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    addressBar.setText(url ?: "")
                    progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    addressBar.setText(url ?: "")
                    progressBar.visibility = View.GONE

                    if (url?.contains("duckduckgo.com") == true) {
                        view?.evaluateJavascript("""
                            (function() {
                                if (window.location.hostname.indexOf('duckduckgo.com') === -1) return;
                                if (document.getElementById('orbit-search-header')) return;

                                var style = document.createElement('style');
                                style.id = 'orbit-search-style';
                                style.textContent = `
                                    #header, header, .header-wrap, .header--mobile {
                                        display: none !important;
                                    }
                                    #orbit-search-header {
                                        background: #0a101e;
                                        color: white;
                                        padding: 16px 14px 10px;
                                        font-family: Arial, sans-serif;
                                        border-bottom: 1px solid #26364d;
                                    }
                                    #orbit-search-header .orbit-brand {
                                        color: #00d2be;
                                        font-size: 23px;
                                        font-weight: bold;
                                        margin-bottom: 13px;
                                    }
                                    #orbit-search-header form {
                                        display: flex;
                                        gap: 8px;
                                    }
                                    #orbit-search-header input {
                                        flex: 1;
                                        min-width: 0;
                                        border: 0;
                                        outline: 0;
                                        border-radius: 14px;
                                        padding: 13px;
                                        background: #202e43;
                                        color: white;
                                        font-size: 16px;
                                    }
                                    #orbit-search-header button {
                                        border: 0;
                                        border-radius: 13px;
                                        padding: 0 18px;
                                        background: #00d2be;
                                        color: #0a101e;
                                        font-size: 19px;
                                        font-weight: bold;
                                    }
                                    #orbit-search-header .orbit-tabs {
                                        display: flex;
                                        justify-content: space-around;
                                        gap: 8px;
                                        margin-top: 15px;
                                    }
                                    #orbit-search-header .orbit-tabs a {
                                        color: #aab8ca;
                                        text-decoration: none;
                                        font-size: 13px;
                                        padding: 7px 3px;
                                    }
                                    #orbit-search-header .orbit-tabs a:first-child {
                                        color: #00d2be;
                                        border-bottom: 2px solid #00d2be;
                                    }
                                `;
                                document.head.appendChild(style);

                                var bar = document.createElement('div');
                                bar.id = 'orbit-search-header';
                                bar.innerHTML =
                                    '<div class="orbit-brand">◉ ORBIT</div>' +
                                    '<form action="https://duckduckgo.com/" method="get">' +
                                    '<input name="q" type="search" aria-label="Search" placeholder="ابحث في الإنترنت...">' +
                                    '<button type="submit">➜</button>' +
                                    '</form>' +
                                    '<nav class="orbit-tabs">' +
                                    '<a data-type="" href="#">الكل</a>' +
                                    '<a data-type="images" href="#">الصور</a>' +
                                    '<a data-type="videos" href="#">الفيديو</a>' +
                                    '<a data-type="news" href="#">الأخبار</a>' +
                                    '</nav>';

                                var params = new URLSearchParams(window.location.search);
                                var query = params.get('q') || '';
                                bar.querySelector('input').value = query;

                                bar.querySelectorAll('.orbit-tabs a').forEach(function(link) {
                                    var target = new URL('https://duckduckgo.com/');
                                    target.searchParams.set('q', query);
                                    if (link.dataset.type) {
                                        target.searchParams.set('ia', link.dataset.type);
                                    }
                                    link.href = target.toString();
                                });

                                document.body.insertBefore(bar, document.body.firstChild);
                            })();
                        """.trimIndent(), null)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, progress: Int) {
                    progressBar.progress = progress
                    progressBar.visibility =
                        if (progress >= 100) View.GONE else View.VISIBLE
                }
            }

            visibility = View.GONE
        }

        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigate()
                true
            } else {
                false
            }
        }

        val contentFrame = FrameLayout(this).apply {
            addView(
                homeScreen,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            addView(
                webView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        // BOTTOM NAVIGATION
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.rgb(14, 22, 37))
        }

        val homeButton = makeAction("⌂") {
            showHome()
        }

        val backButton = makeAction("‹") {
            if (webView.canGoBack()) webView.goBack()
        }

        val forwardButton = makeAction("›") {
            if (webView.canGoForward()) webView.goForward()
        }

        val refreshButton = makeAction("↻") {
            if (webView.visibility == View.VISIBLE) {
                webView.reload()
            } else {
                showHome()
            }
        }

        listOf(homeButton, backButton, forwardButton, refreshButton).forEach {
            bottomBar.addView(
                it,
                LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            )
        }

        root.addView(header)
        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(3)
            )
        )
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(bottomBar)

        setContentView(root)

        if (savedInstanceState != null) {
            val restored = webView.restoreState(savedInstanceState)
            if (restored == null) {
                showHome()
            } else {
                homeScreen.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        } else {
            showHome()
        }
    }

    private fun showHome() {
        homeScreen.visibility = View.VISIBLE
        webView.visibility = View.GONE
        addressBar.setText("")
        addressBar.clearFocus()
    }

    private fun openUrl(url: String) {
        addressBar.setText(url)
        navigate()
    }

    private fun navigate() {
        val input = addressBar.text.toString().trim()
        if (input.isEmpty()) return

        val url = if (
            input.contains(" ") ||
            (!input.contains(".") && !input.startsWith("http"))
        ) {
            "https://duckduckgo.com/?q=" +
                URLEncoder.encode(input, "UTF-8")
        } else {
            if (
                input.startsWith("http://") ||
                input.startsWith("https://")
            ) input else "https://$input"
        }

        val keyboard = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        keyboard.hideSoftInputFromWindow(addressBar.windowToken, 0)

        addressBar.clearFocus()
        homeScreen.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        } else if (webView.visibility == View.VISIBLE) {
            showHome()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
