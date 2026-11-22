package com.orbit.browser

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.net.URL
import java.util.concurrent.Executor
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var homeScreen: ScrollView
    private lateinit var onboarding: FrameLayout
    private lateinit var prefs: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private var accountDialog: AlertDialog? = null
    private lateinit var recentList: LinearLayout
    private lateinit var jumpRow: LinearLayout
    private lateinit var jumpSection: View
    private lateinit var recentSection: View

    // ORBIT Purple Night palette
    private val backgroundColor = Color.rgb(8, 8, 13)
    private val surfaceColor = Color.rgb(19, 19, 28)
    private val elevatedColor = Color.rgb(27, 27, 40)
    private val accentColor = Color.rgb(139, 92, 246)
    private val accentLight = Color.rgb(168, 85, 247)
    private val primaryText = Color.rgb(245, 245, 250)
    private val secondaryText = Color.rgb(154, 158, 175)
    private val borderColor = Color.rgb(42, 42, 58)

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
            strokeColor?.let { setStroke(dp(1), it) }
        }
    }

    private fun gradient(
        color1: Int,
        color2: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color1, color2)
        ).apply {
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
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(primaryText)
            background = rounded(surfaceColor, 15)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun sectionTitle(text: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(22), 0, dp(13))

            addView(
                makeText(text, 15f, primaryText, true),
                LinearLayout.LayoutParams(0, dp(25), 1f)
            )

            addView(
                View(context).apply {
                    setBackgroundColor(borderColor)
                },
                LinearLayout.LayoutParams(dp(90), dp(1))
            )
        }
    }

    // HISTORY
    private fun loadHistory(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()

        return try {
            val json = prefs.getString("history", "[]") ?: "[]"
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url")
                val title = item.optString("title")

                if (url.isNotBlank()) {
                    result.add(url to title)
                }
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun addToHistory(url: String, title: String) {
        if (url.isBlank()) return

        val cleanTitle = title.ifBlank { hostOf(url) }
        val items = listOf(url to cleanTitle) +
            loadHistory().filter { it.first != url }

        val array = JSONArray()

        items.take(10).forEach { item ->
            val obj = JSONObject()
            obj.put("url", item.first)
            obj.put("title", item.second)
            array.put(obj)
        }

        prefs.edit()
            .putString("history", array.toString())
            .apply()

        rebuildHomeLists()
    }

    private fun hostOf(url: String): String {
        return try {
            android.net.Uri.parse(url).host
                ?.removePrefix("www.")
                ?: url
        } catch (_: Exception) {
            url
        }
    }

    private fun rebuildHomeLists() {
        if (!::recentList.isInitialized || !::jumpRow.isInitialized) return

        val items = loadHistory()

        recentList.removeAllViews()
        jumpRow.removeAllViews()

        jumpSection.visibility =
            if (items.isEmpty()) View.GONE else View.VISIBLE

        recentSection.visibility =
            if (items.isEmpty()) View.GONE else View.VISIBLE

        items.take(6).forEach { (url, title) ->

            // Recently visited item
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(surfaceColor, 14)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener { openUrl(url) }
            }

            val host = hostOf(url)
            val firstLetter = host.firstOrNull()?.uppercase() ?: "O"

            val icon = TextView(this).apply {
                text = firstLetter
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = gradient(accentColor, accentLight, 12)
            }

            row.addView(
                icon,
                LinearLayout.LayoutParams(dp(38), dp(38))
            )

            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)

                addView(
                    makeText(title.take(34), 13.5f, primaryText, true)
                )

                addView(
                    makeText(host, 11f, secondaryText)
                )
            }

            row.addView(
                texts,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            recentList.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            )

            // Jump back in card
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(surfaceColor, 16, borderColor)
                isClickable = true
                isFocusable = true
                setOnClickListener { openUrl(url) }
            }

            val preview = TextView(this).apply {
                text = firstLetter
                textSize = 28f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = gradient(
                    Color.rgb(30, 27, 60),
                    Color.rgb(60, 35, 100),
                    16
                )
            }

            card.addView(
                preview,
                LinearLayout.LayoutParams(dp(150), dp(84))
            )

            val caption = makeText(
                title.take(18),
                11.5f,
                primaryText
            ).apply {
                setPadding(dp(10), dp(7), dp(10), dp(9))
            }

            card.addView(caption)

            jumpRow.addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(10)
                }
            )
        }
    }

    private fun showAccountDialog() {
        val user = firebaseAuth.currentUser

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(25), dp(24), dp(24))
            background = rounded(surfaceColor, 24, borderColor)
        }

        panel.addView(
            makeText(
                if (user == null) "Your ORBIT account" else "Your profile",
                20f,
                primaryText,
                true
            ).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(38)
            )
        )

        if (user == null) {
            panel.addView(
                makeText(
                    "Sign in to ORBIT with your Google account.",
                    13f,
                    secondaryText
                ).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(55)
                )
            )

            val signInButton = TextView(this).apply {
                text = "G   Continue with Google"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = gradient(accentColor, accentLight, 18)
                isClickable = true
                isFocusable = true
            }

            panel.addView(
                signInButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
                ).apply {
                    topMargin = dp(12)
                }
            )

            signInButton.setOnClickListener {
                accountDialog?.dismiss()
                startGoogleSignIn()
            }
        } else {
            val avatar = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(accentColor)
                }
                clipToOutline = true
            }

            panel.addView(
                avatar,
                LinearLayout.LayoutParams(dp(82), dp(82)).apply {
                    topMargin = dp(15)
                    bottomMargin = dp(12)
                }
            )

            val photoUrl = user.photoUrl?.toString()
            if (!photoUrl.isNullOrBlank()) {
                Thread {
                    try {
                        val bitmap = URL(photoUrl).openStream().use {
                            BitmapFactory.decodeStream(it)
                        }
                        if (bitmap != null) {
                            runOnUiThread {
                                avatar.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }.start()
            }

            panel.addView(
                makeText(
                    user.displayName ?: "ORBIT user",
                    18f,
                    primaryText,
                    true
                ).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(35)
                )
            )

            panel.addView(
                makeText(
                    user.email ?: "",
                    13f,
                    secondaryText
                ).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(30)
                )
            )

            val signOutButton = TextView(this).apply {
                text = "Sign out"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = rounded(elevatedColor, 16, borderColor)
                isClickable = true
                isFocusable = true
            }

            panel.addView(
                signOutButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48)
                ).apply {
                    topMargin = dp(20)
                }
            )

            signOutButton.setOnClickListener {
                accountDialog?.dismiss()
                signOutFromGoogle()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()

        accountDialog = dialog
        dialog.setOnDismissListener {
            if (accountDialog === dialog) accountDialog = null
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(
            rounded(Color.TRANSPARENT, 24)
        )
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun startGoogleSignIn() {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val mainExecutor = Executor { command ->
                runOnUiThread(command)
            }

            credentialManager.getCredentialAsync(
                this,
                request,
                null,
                mainExecutor,
                object : CredentialManagerCallback<
                    GetCredentialResponse,
                    GetCredentialException
                > {
                    override fun onResult(result: GetCredentialResponse) {
                        val credential = result.credential

                        if (
                            credential is CustomCredential &&
                            credential.type ==
                            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            try {
                                val googleCredential =
                                    GoogleIdTokenCredential.createFrom(
                                        credential.data
                                    )

                                val firebaseCredential =
                                    GoogleAuthProvider.getCredential(
                                        googleCredential.idToken,
                                        null
                                    )

                                firebaseAuth.signInWithCredential(firebaseCredential)
                                    .addOnCompleteListener(this@MainActivity) { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Welcome to ORBIT!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showAccountDialog()
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                task.exception?.localizedMessage
                                                    ?: "Google sign-in failed",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@MainActivity,
                                    e.localizedMessage ?: "Could not read Google account",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Google account credential was not received",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onError(e: GetCredentialException) {
                        Toast.makeText(
                            this@MainActivity,
                            e.localizedMessage ?: "Google sign-in was cancelled",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                e.localizedMessage ?: "Could not start Google sign-in",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun signOutFromGoogle() {
        firebaseAuth.signOut()

        val mainExecutor = Executor { command ->
            runOnUiThread(command)
        }

        try {
            credentialManager.clearCredentialStateAsync(
                ClearCredentialStateRequest(),
                null,
                mainExecutor,
                object : CredentialManagerCallback<Void?, ClearCredentialException> {
                    override fun onResult(result: Void?) {
                        showAccountDialog()
                    }

                    override fun onError(e: ClearCredentialException) {
                        showAccountDialog()
                    }
                }
            )
        } catch (_: Exception) {
            showAccountDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        prefs = getSharedPreferences("orbit", Context.MODE_PRIVATE)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(backgroundColor)
        }

        // MAIN SCREEN
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        // TOP BAR
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(10))
        }

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 20f
            setTextColor(primaryText)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
        }

        topBar.addView(
            menuButton,
            LinearLayout.LayoutParams(dp(40), dp(42))
        )

        val logoIcon = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.orbit_logo)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        val logoText = TextView(this).apply {
            text = "ORBIT"
            textSize = 19f
            setTextColor(primaryText)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            letterSpacing = 0.08f
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                logoIcon,
                LinearLayout.LayoutParams(dp(26), dp(42))
            )

            addView(
                logoText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(42)
                )
            )
        }

        topBar.addView(brand)

        addressBar = EditText(this).apply {
            hint = "Search or enter address"
            textSize = 13f
            setSingleLine(true)
            setTextColor(primaryText)
            setHintTextColor(secondaryText)
            setPadding(dp(14), 0, dp(12), 0)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_URI
            background = rounded(surfaceColor, 15, borderColor)
            maxLines = 1
        }

        topBar.addView(
            addressBar,
            LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginStart = dp(10)
            }
        )

        val accountButton = TextView(this).apply {
            text = "♙"
            textSize = 23f
            setTextColor(accentLight)
            gravity = Gravity.CENTER
            background = rounded(surfaceColor, 14, borderColor)
            isClickable = true
            isFocusable = true
            contentDescription = "Account"
            setOnClickListener {
                showAccountDialog()
            }
        }

        topBar.addView(
            accountButton,
            LinearLayout.LayoutParams(dp(42), dp(44)).apply {
                marginStart = dp(7)
            }
        )

        main.addView(topBar)

        // PROGRESS BAR
        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progressTintList =
                android.content.res.ColorStateList.valueOf(accentLight)
            progressBackgroundTintList =
                android.content.res.ColorStateList.valueOf(surfaceColor)
            visibility = View.GONE
        }

        main.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(2)
            )
        )

        // HOME SCREEN
        homeScreen = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(backgroundColor)
        }

        val homeContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(24))
        }

        // PERSONAL SPACE ROW
        val personalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        personalRow.addView(
            makeText("Your personal space", 12f, secondaryText),
            LinearLayout.LayoutParams(0, dp(34), 1f)
        )

        val personalBadge = TextView(this).apply {
            text = "ORBIT"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = gradient(accentColor, accentLight, 15)
        }

        personalRow.addView(
            personalBadge,
            LinearLayout.LayoutParams(dp(78), dp(34))
        )

        homeContent.addView(personalRow)

        // SHORTCUTS
        homeContent.addView(sectionTitle("Shortcuts"))

        val shortcutsGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun shortcutRow(): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
        }

        fun addShortcut(
            row: LinearLayout,
            letter: String,
            name: String,
            url: String,
            colorA: Int,
            colorB: Int
        ) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(surfaceColor, 16, borderColor)
                setPadding(dp(8), dp(14), dp(8), dp(13))
                isClickable = true
                isFocusable = true
                setOnClickListener { openUrl(url) }
            }

            val icon = TextView(this).apply {
                text = letter
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = gradient(colorA, colorB, 30)
            }

            card.addView(
                icon,
                LinearLayout.LayoutParams(dp(46), dp(46))
            )

            card.addView(
                makeText(name, 12f, primaryText, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(9), 0, 0)
                }
            )

            row.addView(
                card,
                LinearLayout.LayoutParams(0, dp(105), 1f).apply {
                    marginEnd = dp(8)
                }
            )
        }

        val row1 = shortcutRow()

        addShortcut(
            row1,
            "f",
            "Facebook",
            "https://www.facebook.com",
            Color.rgb(24, 60, 130),
            Color.rgb(66, 103, 178)
        )

        addShortcut(
            row1,
            "W",
            "Wikipedia",
            "https://www.wikipedia.org",
            Color.rgb(60, 60, 65),
            Color.rgb(110, 110, 115)
        )

        shortcutsGrid.addView(row1)

        val row2 = shortcutRow()

        addShortcut(
            row2,
            "▶",
            "YouTube",
            "https://www.youtube.com",
            Color.rgb(150, 20, 20),
            Color.rgb(220, 40, 40)
        )

        addShortcut(
            row2,
            "D",
            "DuckDuckGo",
            "https://duckduckgo.com",
            Color.rgb(170, 60, 30),
            Color.rgb(220, 100, 50)
        )

        shortcutsGrid.addView(
            row2,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(9)
            }
        )

        homeContent.addView(shortcutsGrid)

        // JUMP BACK IN
        jumpSection = sectionTitle("Jump back in")
        homeContent.addView(jumpSection)

        val jumpScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        jumpRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, dp(8), 0)
        }

        jumpScroll.addView(jumpRow)
        homeContent.addView(jumpScroll)

        // RECENTLY VISITED
        recentSection = sectionTitle("Recently visited")
        homeContent.addView(recentSection)

        recentList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        homeContent.addView(recentList)

        homeScreen.addView(homeContent)
        rebuildHomeLists()

        // WEB VIEW
        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                settings.safeBrowsingEnabled = true
            }

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

                    val pageTitle = view?.title ?: ""
                    if (!url.isNullOrBlank()) {
                        addToHistory(url, pageTitle)
                    }

                    if (url?.contains("duckduckgo.com") == true) {
                        view?.evaluateJavascript("""
                            (function() {
                                if (window.location.hostname.indexOf('duckduckgo.com') === -1) return;
                                if (document.getElementById('orbit-header')) return;

                                var style = document.createElement('style');
                                style.id = 'orbit-style';
                                style.textContent = `
                                    #header, header, .header-wrap, .header--mobile {
                                        display: none !important;
                                    }
                                    #orbit-header {
                                        background: #08080d;
                                        color: #f5f5fa;
                                        padding: 12px 14px 0;
                                        font-family: Arial, sans-serif;
                                        border-bottom: 1px solid #2a2a3a;
                                        position: sticky;
                                        top: 0;
                                        z-index: 99999;
                                    }
                                    #orbit-header .orbit-top {
                                        display: flex;
                                        align-items: center;
                                        gap: 10px;
                                    }
                                    #orbit-header .orbit-logo {
                                        color: #a855f7;
                                        font-size: 20px;
                                        font-weight: bold;
                                        letter-spacing: 1px;
                                        white-space: nowrap;
                                    }
                                    #orbit-header form {
                                        flex: 1;
                                        display: flex;
                                        gap: 8px;
                                        min-width: 0;
                                    }
                                    #orbit-header input {
                                        flex: 1;
                                        min-width: 0;
                                        border: 1px solid #3a2a5a;
                                        outline: 0;
                                        border-radius: 14px;
                                        padding: 11px 13px;
                                        background: #13131c;
                                        color: #f5f5fa;
                                        font-size: 15px;
                                    }
                                    #orbit-header button {
                                        border: 0;
                                        border-radius: 14px;
                                        padding: 0 16px;
                                        background: linear-gradient(135deg,#8b5cf6,#a855f7);
                                        color: #fff;
                                        font-size: 14px;
                                        font-weight: bold;
                                    }
                                    #orbit-header .orbit-tabs {
                                        display: flex;
                                        justify-content: space-around;
                                        gap: 6px;
                                        margin-top: 12px;
                                        overflow-x: auto;
                                    }
                                    #orbit-header .orbit-tabs a {
                                        color: #9a9eaf;
                                        text-decoration: none;
                                        font-size: 13px;
                                        padding: 8px 4px;
                                        white-space: nowrap;
                                    }
                                    #orbit-header .orbit-tabs a.active {
                                        color: #a855f7;
                                        border-bottom: 2px solid #a855f7;
                                    }
                                `;
                                document.head.appendChild(style);

                                var bar = document.createElement('div');
                                bar.id = 'orbit-header';
                                bar.innerHTML =
                                    '<div class="orbit-top">' +
                                    '<div class="orbit-logo">◍ ORBIT</div>' +
                                    '<form action="https://duckduckgo.com/" method="get">' +
                                    '<input name="q" type="search" aria-label="Search" placeholder="Search the web">' +
                                    '<button type="submit">➜</button>' +
                                    '</form>' +
                                    '</div>' +
                                    '<nav class="orbit-tabs">' +
                                    '<a data-type="" class="active" href="#">All</a>' +
                                    '<a data-type="images" href="#">Images</a>' +
                                    '<a data-type="videos" href="#">Videos</a>' +
                                    '<a data-type="news" href="#">News</a>' +
                                    '</nav>';

                                var params = new URLSearchParams(window.location.search);
                                var query = params.get('q') || '';
                                bar.querySelector('input').value = query;

                                var current = params.get('ia') || '';

                                bar.querySelectorAll('.orbit-tabs a').forEach(function(link) {
                                    if ((link.dataset.type || '') === current) {
                                        link.classList.add('active');
                                    } else {
                                        link.classList.remove('active');
                                    }

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

        // ADDRESS BAR SEARCH
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

        // CONTENT FRAME
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

        main.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // ORBIT FLOATING BOTTOM NAVIGATION
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(9), dp(16), dp(12))
            setBackgroundColor(backgroundColor)
        }

        val navPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(6), dp(7), dp(6))
            background = rounded(
                Color.rgb(18, 17, 27),
                24,
                borderColor
            )
            elevation = dp(8).toFloat()
        }

        val homeAction = makeAction("⌂") { showHome() }.apply {
            textSize = 25f
            background = gradient(accentColor, accentLight, 18)
            setTextColor(Color.WHITE)
        }

        val backAction = makeAction("‹") {
            if (webView.canGoBack()) webView.goBack()
        }.apply {
            textSize = 29f
            background = rounded(Color.TRANSPARENT, 18)
        }

        val forwardAction = makeAction("›") {
            if (webView.canGoForward()) webView.goForward()
        }.apply {
            textSize = 29f
            background = rounded(Color.TRANSPARENT, 18)
        }

        val refreshAction = makeAction("↻") {
            if (webView.visibility == View.VISIBLE) {
                webView.reload()
            } else {
                showHome()
            }
        }.apply {
            textSize = 24f
            background = rounded(Color.TRANSPARENT, 18)
        }

        listOf(
            homeAction,
            backAction,
            forwardAction,
            refreshAction
        ).forEach { button ->
            navPill.addView(
                button,
                LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    marginStart = dp(3)
                    marginEnd = dp(3)
                }
            )
        }

        bottomBar.addView(
            navPill,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        main.addView(bottomBar)
        root.addView(
            main,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ONBOARDING OVERLAY
        onboarding = FrameLayout(this).apply {
            setBackgroundColor(backgroundColor)
            visibility = View.GONE
        }

        val obContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), 0, dp(28), 0)
        }

        val obScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(
                obContent,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val logoCircle = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.orbit_logo)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        obContent.addView(
            logoCircle,
            LinearLayout.LayoutParams(dp(110), dp(110)).apply {
                topMargin = dp(100)
            }
        )

        obContent.addView(
            makeText("Welcome to ORBIT", 24f, primaryText, true).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
            ).apply {
                topMargin = dp(30)
            }
        )

        obContent.addView(
            makeText(
                "Fast and secure web browsing.\nYour world, your way.",
                13.5f,
                secondaryText
            ).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(60)
            ).apply {
                topMargin = dp(10)
            }
        )

        obContent.addView(
            makeText(
                "By continuing, you agree to the ORBIT Terms of Service and Privacy Policy.",
                11f,
                secondaryText
            ).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                topMargin = dp(45)
            }
        )

        val continueButton = TextView(this).apply {
            text = "Continue"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = gradient(accentColor, accentLight, 22)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                prefs.edit()
                    .putBoolean("seen_onboarding", true)
                    .apply()

                onboarding.visibility = View.GONE
            }
        }

        obContent.addView(
            continueButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                topMargin = dp(20)
            }
        )

        val dots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        repeat(3) { index ->
            dots.addView(
                View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(
                            if (index == 0) accentLight else borderColor
                        )
                    }
                },
                LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            )
        }

        obContent.addView(
            dots,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(20)
            ).apply {
                topMargin = dp(28)
                bottomMargin = dp(60)
            }
        )

        onboarding.addView(
            obScroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // MENU OPENS WELCOME SCREEN
        menuButton.setOnClickListener {
            onboarding.visibility = View.VISIBLE
        }

        // Overlay must be added after main
        root.addView(
            onboarding,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        if (savedInstanceState != null) {
            if (webView.restoreState(savedInstanceState) == null) {
                showHome()
            } else {
                homeScreen.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        } else {
            showHome()

            if (!prefs.getBoolean("seen_onboarding", false)) {
                onboarding.visibility = View.VISIBLE
            }
        }
    }

    private fun showHome() {
        homeScreen.visibility = View.VISIBLE
        webView.visibility = View.GONE
        addressBar.setText("")
        addressBar.clearFocus()
        progressBar.visibility = View.GONE
        rebuildHomeLists()
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
        } else if (
            input.startsWith("http://") ||
            input.startsWith("https://")
        ) {
            input
        } else {
            "https://$input"
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
        if (onboarding.visibility == View.VISIBLE) {
            prefs.edit()
                .putBoolean("seen_onboarding", true)
                .apply()

            onboarding.visibility = View.GONE
        } else if (
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
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }
}
