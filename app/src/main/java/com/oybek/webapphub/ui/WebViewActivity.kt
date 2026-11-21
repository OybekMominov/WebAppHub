package com.oybek.webapphub.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.oybek.webapphub.R
import com.oybek.webapphub.data.AppPrefs
import com.oybek.webapphub.databinding.ActivityWebviewBinding
import com.oybek.webapphub.model.SniffedMedia
import com.oybek.webapphub.util.AdBlocker
import com.oybek.webapphub.util.MediaSniffer
import com.oybek.webapphub.util.Strings
import kotlin.math.abs

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding
    private lateinit var prefs: AppPrefs

    private var siteName: String = "WebApp Hub"
    private var initialUrl: String = "https://www.google.com"
    private var adBlockActive: Boolean = true
    private var desktopActive: Boolean = false

    private val sniffedList = mutableListOf<SniffedMedia>()
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val mobileUserAgent =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    private val desktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris = if (result.resultCode == Activity.RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            } else {
                null
            }
            fileUploadCallback?.onReceiveValue(uris)
            fileUploadCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        siteName = intent.getStringExtra("site_name") ?: "WebApp Hub"
        initialUrl = intent.getStringExtra("site_url") ?: "https://www.google.com"
        adBlockActive = intent.getBooleanExtra("site_adblock", prefs.globalAdBlock)
        desktopActive = intent.getBooleanExtra("site_desktop", false)

        setupWebView()
        setupFloatingMenu()
        setupVideoGestures()
        setupBackNavigation()

        binding.webView.loadUrl(initialUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = desktopActive
            loadWithOverviewMode = desktopActive
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = if (desktopActive) desktopUserAgent else mobileUserAgent
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }

        binding.webView.setDownloadListener { url, userAgent, _, mimeType, _ ->
            val fileName = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() }
                ?: "dosya_${System.currentTimeMillis()}"
            val media = SniffedMedia(
                url = url,
                fileName = fileName,
                mimeType = mimeType ?: "*/*",
                isVideoOrAudio = (mimeType ?: "").startsWith("video") || (mimeType ?: "").startsWith("audio")
            )
            addSniffedMedia(media)
            MediaSniffer.sendToIdmOrSystem(this, media, userAgent)
        }

        binding.webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                if (adBlockActive && AdBlocker.isAdUrl(reqUrl)) {
                    return AdBlocker.emptyResponse()
                }

                MediaSniffer.inspectUrl(reqUrl)?.let { detected ->
                    runOnUiThread { addSniffedMedia(detected) }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (adBlockActive && AdBlocker.isAdUrl(url)) {
                    return true
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {
                    }
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                CookieManager.getInstance().flush()

                if (!url.isNullOrBlank()) {
                    prefs.addHistory(view?.title ?: siteName, url)
                }

                if (adBlockActive) {
                    view?.evaluateJavascript(AdBlocker.COSMETIC_FILTER_JS, null)
                }

                if (prefs.backgroundAudioEnabled) {
                    val bgScript = "(function(){try{Object.defineProperty(document,'hidden',{get:function(){return false;}});Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';}});}catch(e){}})();"
                    view?.evaluateJavascript(bgScript, null)
                }

                scanDomMediaLinks()
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (adBlockActive && !isUserGesture) {
                    return false
                }
                val href = view?.handler?.obtainMessage()
                view?.requestFocusNodeHref(href)
                val targetUrl = href?.data?.getString("url")
                if (!targetUrl.isNullOrBlank()) {
                    if (adBlockActive && AdBlocker.isAdUrl(targetUrl)) return false
                    binding.webView.loadUrl(targetUrl)
                }
                return false
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback
                val intent = fileChooserParams?.createIntent() ?: return false
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (_: Exception) {
                    fileUploadCallback = null
                    false
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                binding.fullscreenContainer.addView(view)
                binding.fullscreenContainer.visibility = View.VISIBLE
                binding.floatingPill.visibility = View.GONE
                binding.expandedMenu.visibility = View.GONE
                if (prefs.videoGesturesEnabled) {
                    binding.gestureOverlay.visibility = View.VISIBLE
                }

                if (prefs.autoLandscapeFullscreen) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                setSystemBarsVisible(false)
            }

            override fun onHideCustomView() {
                exitFullscreenVideo()
            }
        }
    }

    private fun exitFullscreenVideo() {
        if (customView == null) return
        binding.fullscreenContainer.removeAllViews()
        binding.fullscreenContainer.visibility = View.GONE
        binding.gestureOverlay.visibility = View.GONE
        binding.floatingPill.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setSystemBarsVisible(true)
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun scanDomMediaLinks() {
        val js = "(function(){var urls=[];document.querySelectorAll('video, source, audio').forEach(function(el){if(el.src&&el.src.startsWith('http'))urls.push(el.src);});return JSON.stringify(urls);})();"
        binding.webView.evaluateJavascript(js) { result ->
            if (result != null && result != "null") {
                try {
                    val cleaned = result.trim('"').replace("\"", """)
                    val arr = org.json.JSONArray(cleaned)
                    for (i in 0 until arr.length()) {
                        val u = arr.getString(i)
                        val media = MediaSniffer.inspectUrl(u) ?: SniffedMedia(
                            url = u,
                            fileName = Uri.parse(u).lastPathSegment ?: "video_stream.mp4",
                            mimeType = "video/*",
                            isVideoOrAudio = true
                        )
                        addSniffedMedia(media)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun addSniffedMedia(media: SniffedMedia) {
        if (sniffedList.any { it.url == media.url }) return
        sniffedList.add(0, media)
        updateSnifferCounters()
    }

    private fun updateSnifferCounters() {
        val currentLang = prefs.language
        val itemCount = sniffedList.size
        binding.btnOpenSniffer.text = "${Strings.get(currentLang, "idm_download")} ($itemCount)"
        if (itemCount > 0) {
            binding.tvMediaCounterPill.visibility = View.VISIBLE
            binding.tvMediaCounterPill.text = itemCount.toString()
        } else {
            binding.tvMediaCounterPill.visibility = View.GONE
        }
    }

    private fun setupFloatingMenu() {
        val currentLang = prefs.language
        binding.tvMenuSiteTitle.text = siteName
        binding.swMenuDesktop.text = Strings.get(currentLang, "desktop_mode")
        binding.swMenuAdBlock.text = Strings.get(currentLang, "ad_blocker_toggle")
        binding.btnNavHome.text = Strings.get(currentLang, "home")
        binding.swMenuDesktop.isChecked = desktopActive
        binding.swMenuAdBlock.isChecked = adBlockActive
        updateSnifferCounters()

        binding.floatingPill.setOnClickListener {
            binding.floatingPill.visibility = View.GONE
            binding.expandedMenu.visibility = View.VISIBLE
        }

        binding.btnCloseMenu.setOnClickListener {
            binding.expandedMenu.visibility = View.GONE
            binding.floatingPill.visibility = View.VISIBLE
        }

        binding.btnNavBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }

        binding.btnNavForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }

        binding.btnNavRefresh.setOnClickListener {
            binding.webView.reload()
        }

        binding.swMenuDesktop.setOnCheckedChangeListener { _, isChecked ->
            desktopActive = isChecked
            binding.webView.settings.apply {
                userAgentString = if (isChecked) desktopUserAgent else mobileUserAgent
                useWideViewPort = isChecked
                loadWithOverviewMode = isChecked
            }
            binding.webView.reload()
        }

        binding.swMenuAdBlock.setOnCheckedChangeListener { _, isChecked ->
            adBlockActive = isChecked
            binding.webView.reload()
        }

        binding.btnOpenSniffer.setOnClickListener {
            scanDomMediaLinks()
            showSnifferBottomSheet()
        }

        binding.btnNavHome.setOnClickListener {
            finish()
        }
    }

    private fun showSnifferBottomSheet() {
        val currentLang = prefs.language
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_sniffer, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvSnifferTitle)
        val tvSub = view.findViewById<TextView>(R.id.tvSnifferSubtitle)
        val rv = view.findViewById<RecyclerView>(R.id.rvSniffedMedia)
        val tvEmpty = view.findViewById<TextView>(R.id.tvSnifferEmpty)
        val btnClear = view.findViewById<MaterialButton>(R.id.btnClearSniffer)

        tvTitle.text = Strings.get(currentLang, "sniffer_title")
        tvSub.text = Strings.get(currentLang, "sniffer_sub")
        tvEmpty.text = Strings.get(currentLang, "sniffer_empty")
        btnClear.text = Strings.get(currentLang, "clear_list")

        val ua = if (desktopActive) desktopUserAgent else mobileUserAgent
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SniffedMediaAdapter(
            items = sniffedList,
            lang = currentLang,
            onDownloadIdm = { MediaSniffer.sendToIdmOrSystem(this, it, ua) },
            onOpenPlayer = { MediaSniffer.openInExternalPlayer(this, it) },
            onCopyLink = { MediaSniffer.copyLink(this, it.url, currentLang) }
        )

        tvEmpty.visibility = if (sniffedList.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (sniffedList.isEmpty()) View.GONE else View.VISIBLE

        btnClear.setOnClickListener {
            sniffedList.clear()
            updateSnifferCounters()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupVideoGestures() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val hideHandler = Handler(Looper.getMainLooper())
        val hideRunnable = Runnable { binding.tvGestureFeedback.visibility = View.GONE }

        fun showFeedback(text: String) {
            binding.tvGestureFeedback.text = text
            binding.tvGestureFeedback.visibility = View.VISIBLE
            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 1100)
        }

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val width = binding.gestureOverlay.width.coerceAtLeast(1)
                if (e.x < width / 2f) {
                    binding.webView.evaluateJavascript(
                        "document.querySelectorAll('video').forEach(v => v.currentTime = Math.max(0, v.currentTime - 10));",
                        null
                    )
                    showFeedback("⏪ -10s")
                } else {
                    binding.webView.evaluateJavascript(
                        "document.querySelectorAll('video').forEach(v => v.currentTime = Math.min(v.duration || (v.currentTime + 10), v.currentTime + 10));",
                        null
                    )
                    showFeedback("⏩ +10s")
                }
                return true
            }
        })

        var startX = 0f
        var startY = 0f
        var startBrightness = 0.5f
        var startVolume = 0
        var isVerticalScroll = false

        binding.gestureOverlay.setOnTouchListener { v, event ->
            detector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isVerticalScroll = false
                    val lp = window.attributes
                    startBrightness = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - startX
                    val dy = startY - event.y
                    if (!isVerticalScroll && abs(dy) > 28 && abs(dy) > abs(dx)) {
                        isVerticalScroll = true
                    }
                    if (isVerticalScroll) {
                        val height = v.height.coerceAtLeast(1).toFloat()
                        val width = v.width.coerceAtLeast(1).toFloat()
                        val deltaRatio = dy / (height * 0.75f)
                        val gestureLang = prefs.language

                        if (startX < width / 2f) {
                            val newBright = (startBrightness + deltaRatio).coerceIn(0.05f, 1.0f)
                            val lp = window.attributes
                            lp.screenBrightness = newBright
                            window.attributes = lp
                            val brightPct = (newBright * 100).toInt()
                            showFeedback("☀ ${Strings.get(gestureLang, "brightness")}: %$brightPct")
                        } else {
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val newVol = (startVolume + (deltaRatio * maxVol).toInt()).coerceIn(0, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                            val volPct = if (maxVol > 0) (newVol * 100) / maxVol else 0
                            showFeedback("🔊 ${Strings.get(gestureLang, "volume")}: %$volPct")
                        }
                        return@setOnTouchListener true
                    }
                }
            }

            if (!isVerticalScroll) {
                binding.fullscreenContainer.dispatchTouchEvent(event)
            }
            true
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    exitFullscreenVideo()
                } else if (binding.expandedMenu.visibility == View.VISIBLE) {
                    binding.expandedMenu.visibility = View.GONE
                    binding.floatingPill.visibility = View.VISIBLE
                } else if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        if (!prefs.backgroundAudioEnabled) {
            binding.webView.onPause()
            binding.webView.evaluateJavascript(
                "document.querySelectorAll('video,audio').forEach(m => m.pause());",
                null
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
