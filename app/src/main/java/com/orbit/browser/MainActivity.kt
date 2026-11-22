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
import android.content.res.ColorStateList
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.ScrollView
import java.net.URLEncoder

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var homeScreen: ScrollView

    // ORBIT Minimal Premium palette
    private val backgroundColor = Color.rgb(9, 11, 15)
    private val surfaceColor = Color.rgb(20, 23, 30)
    private val elevatedColor = Color.rgb(27, 31, 39)
    private val accentColor = Color.rgb(0, 210, 190)
    private val primaryText = Color.rgb(245, 247, 250)
    private val secondaryText = Color.rgb(145, 153, 166)
    private val borderColor = Color.rgb(43, 48, 58)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        radius: Int,
        strokeColor: Int? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null) {
                setStroke(dp(1), strokeColor)
            }
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
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(primaryText)
            background = rounded(surfaceColor, 16)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        // TOP BRAND HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(13))
        }

        val logoMark = TextView(this).apply {
            text = "◉"
            textSize = 27f
            setTextColor(accentColor)
            gravity = Gravity.CENTER
        }

        val logoText = TextView(this).apply {
            text = "ORBIT"
            textSize = 20f
            setTextColor(primaryText)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), 0, dp(14), 0)
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(logoMark, LinearLayout.LayoutParams(dp(30), dp(44)))
            addView(logoText)
        }

        addressBar = EditText(this).apply {
            hint = "Search or enter address"
            textSize = 13.5f
            setSingleLine(true)
            setTextColor(primaryText)
            setHintTextColor(secondaryText)
            setPadding(dp(14), 0, dp(12), 0)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
            background = rounded(surfaceColor, 15, borderColor)
            setSelectAllOnFocus(false)
            maxLines = 1
        }

        val searchButton = TextView(this).apply {
            text = "→"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(backgroundColor)
            background = rounded(primaryText, 15)
            isClickable = true
            isFocusable = true
            setOnClickListener { navigate() }
        }

        header.addView(brand)
        header.addView(
            addressBar,
            LinearLayout.LayoutParams(0, dp(47), 1f)
        )
        header.addView(
            searchButton,
            LinearLayout.LayoutParams(dp(47), dp(47)).apply {
                marginStart = dp(8)
            }
        )

        // PAGE LOADING INDICATOR
        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressTintList = ColorStateList.valueOf(accentColor)
            progressBackgroundTintList =
                ColorStateList.valueOf(surfaceColor)
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
            setPadding(dp(21), dp(37), dp(21), dp(28))
        }

        val welcome = makeText(
            "WELCOME TO",
            11f,
            secondaryText,
            true
        ).apply {
            letterSpacing = 0.2f
        }

        val title = makeText(
            "Your world.",
            34f,
            primaryText,
            true
        )

        val titleSecond = makeText(
            "Your ORBIT.",
            34f,
            primaryText,
            true
        )

        val subtitle = makeText(
            "Explore the internet your way.",
            14f,
            secondaryText
        ).apply {
            setPadding(0, dp(9), 0, 0)
        }

        homeContent.addView(welcome)
        homeContent.addView(title)
        homeContent.addView(titleSecond)
        homeContent.addView(subtitle)

        // QUICK ACCESS SECTION
        val sectionHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(39), 0, dp(15))
        }

        val sectionTitle = makeText(
            "QUICK ACCESS",
            11f,
            secondaryText,
            true
        ).apply {
            letterSpacing = 0.14f
        }

        sectionHeader.addView(
            sectionTitle,
            LinearLayout.LayoutParams(0, dp(25), 1f)
        )

        val sectionLine = View(this).apply {
            setBackgroundColor(borderColor)
        }

        sectionHeader.addView(
            sectionLine,
            LinearLayout.LayoutParams(dp(75), dp(1))
        )

        homeContent.addView(sectionHeader)

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
                background = rounded(surfaceColor, 18, borderColor)
                setPadding(dp(8), dp(16), dp(8), dp(15))
                isClickable = true
                isFocusable = true
                setOnClickListener { openUrl(url) }
            }

            val iconBox = TextView(this).apply {
                text = symbol
                textSize = 22f
                setTypeface(null, Typeface.BOLD)
                setTextColor(accentColor)
                gravity = Gravity.CENTER
                background = rounded(elevatedColor, 14)
            }

            val label = makeText(
                name,
                12f,
                primaryText,
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
            }

            card.addView(
                iconBox,
                LinearLayout.LayoutParams(dp(47), dp(47))
            )
            card.addView(label)

            row.addView(
                card,
                LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                    marginEnd = dp(7)
                }
            )
        }

        addShortcut(
            rowOne,
            "D",
            "DuckDuckGo",
            "https://duckduckgo.com"
        )
        addShortcut(
            rowOne,
            "▶",
            "YouTube",
            "https://www.youtube.com"
        )
        addShortcut(
            rowTwo,
            "f",
            "Facebook",
            "https://www.facebook.com"
        )
        addShortcut(
            rowTwo,
            "W",
            "Wikipedia",
            "https://www.wikipedia.org"
        )

        shortcuts.addView(rowOne)
        shortcuts.addView(
            rowTwo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(9)
            }
        )

        homeContent.addView(shortcuts)

        // ORBIT INFORMATION CARD
        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surfaceColor, 19, borderColor)
            setPadding(dp(18), dp(17), dp(18), dp(17))
        }

        val infoTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val infoDot = TextView(this).apply {
            text = "●"
            textSize = 12f
            setTextColor(accentColor)
            gravity = Gravity.CENTER
        }

        val infoTitle = makeText(
            "A better way to browse",
            15f,
            primaryText,
            true
        ).apply {
            setPadding(dp(9), 0, 0, 0)
        }

        infoTop.addView(infoDot)
        infoTop.addView(infoTitle)
        infoCard.addView(infoTop)

        val infoDescription = makeText(
            "Discover the web with a clean and simple experience.",
            12f,
            secondaryText
        ).apply {
            setPadding(0, dp(9), 0, 0)
        }

        infoCard.addView(infoDescription)

        homeContent.addView(
            infoCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(25)
            }
        )

        val footer = makeText(
            "ORBIT  •  YOUR WORLD, YOUR WAY",
            10f,
            secondaryText,
            true
        ).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
            setPadding(0, dp(26), 0, dp(12))
        }

        homeContent.addView(footer)
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

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
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
                                        background: #090b0f;
                                        color: white;
                                        padding: 16px 14px 10px;
                                        font-family: Arial, sans-serif;
                                        border-bottom: 1px solid #292e37;
                                    }
                                    #orbit-search-header .orbit-brand {
                                        color: #00d2be;
                                        font-size: 22px;
                                        font-weight: bold;
                                        letter-spacing: 2px;
                                        margin-bottom: 13px;
                                    }
                                    #orbit-search-header form {
                                        display: flex;
                                        gap: 8px;
                                    }
                                    #orbit-search-header input {
                                        flex: 1;
                                        min-width: 0;
                                        border: 1px solid #343a45;
                                        outline: 0;
                                        border-radius: 13px;
                                        padding: 13px;
                                        background: #171b23;
                                        color: white;
                                        font-size: 16px;
                                    }
                                    #orbit-search-header button {
                                        border: 0;
                                        border-radius: 13px;
                                        padding: 0 18px;
                                        background: #f5f7fa;
                                        color: #090b0f;
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
                                        color: #a1a9b6;
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
                override fun onProgressChanged(
                    view: WebView?,
                    progress: Int
                ) {
                    progressBar.progress = progress
                    progressBar.visibility =
                        if (progress >= 100) View.GONE else View.VISIBLE
                }
            }

            visibility = View.GONE
        }

        // KEYBOARD SEARCH
        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEARCH
            ) {
                navigate()
                true
            } else {
                false
            }
        }

        // MAIN CONTENT FRAME
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
            setPadding(dp(9), dp(8), dp(9), dp(8))
            setBackgroundColor(Color.rgb(13, 16, 21))
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

        listOf(
            homeButton,
            backButton,
            forwardButton,
            refreshButton
        ).forEach {
            bottomBar.addView(
                it,
                LinearLayout.LayoutParams(0, dp(47), 1f).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            )
        }

        // ASSEMBLE SCREEN
        root.addView(header)

        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(2)
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
        progressBar.visibility = View.GONE
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
            ) {
                input
            } else {
                "https://$input"
            }
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
        if (
            webView.visibility == View.VISIBLE &&
            webView.canGoBack()
        ) {
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
