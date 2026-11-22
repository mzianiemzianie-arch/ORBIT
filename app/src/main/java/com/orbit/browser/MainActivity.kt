package com.orbit.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.ProgressBar
import java.net.URLEncoder

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 24, 39))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val logo = TextView(this).apply {
            text = "ORBIT"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 12, 0)
        }

        addressBar = EditText(this).apply {
            hint = "Search or enter website"
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8)
            imeOptions = EditorInfo.IME_ACTION_GO
            setBackgroundColor(Color.rgb(39, 50, 67))
        }

        val goButton = Button(this).apply {
            text = "GO"
            setOnClickListener { navigate() }
        }

        header.addView(logo)
        header.addView(addressBar, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ))
        header.addView(goButton)

        progressBar = ProgressBar(this, null,
            android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }

        webView = WebView(this).apply {
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
                    addressBar.setText(url ?: "")
                    progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    addressBar.setText(url ?: "")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, progress: Int) {
                    progressBar.progress = progress
                    progressBar.visibility =
                        if (progress >= 100) View.GONE else View.VISIBLE
                }
            }
        }

        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigate()
                true
            } else false
        }

        root.addView(header)
        root.addView(progressBar)
        root.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl("https://www.google.com")
        }
    }

    private fun navigate() {
        val input = addressBar.text.toString().trim()
        if (input.isEmpty()) return

        val url = if (input.contains(" ") ||
            (!input.contains(".") && !input.startsWith("http"))) {
            "https://www.google.com/search?q=" +
                URLEncoder.encode(input, "UTF-8")
        } else {
            if (input.startsWith("http://") ||
                input.startsWith("https://")) input
            else "https://$input"
        }

        webView.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
