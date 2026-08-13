package com.seamhealth.elsrt.ui.screens.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.seamhealth.elsrt.R
import com.seamhealth.elsrt.util.NotificationPermissionManager
import com.seamhealth.elsrt.util.StorageHelper

class BrowserActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SEAM_HREF = "seam_href"
        private const val FILE_CHOOSER_CODE = 3712
        private const val PUSH_PROMPT_DELAY_MS = 20_000L
    }

    private lateinit var seamWeb: WebView
    private lateinit var loadIndicator: ProgressBar
    private var pendingUploadSink: ValueCallback<Array<Uri>>? = null
    private var awaitingFirstPaint = true

    private val mainHandler = Handler(Looper.getMainLooper())
    private val storage by lazy { StorageHelper(this) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        NotificationPermissionManager.openNotificationSettingsAfterPermission(this)
    }

    private val pushPromptRunnable = Runnable {
        if (isFinishing || isDestroyed) return@Runnable
        if (storage.isNotificationDialogShown()) return@Runnable
        storage.setNotificationDialogShown()
        NotificationPermissionManager.showPrePermissionDialog(this) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        val targetHref = intent.getStringExtra(EXTRA_SEAM_HREF) ?: run {
            finish()
            return
        }

        assembleHostSurface()
        configureSeamEngine()
        beginNavigation(targetHref)
        bindBackStack()
        scheduleNotifyPrompt()
    }

    private fun scheduleNotifyPrompt() {
        if (storage.isNotificationDialogShown()) return
        mainHandler.removeCallbacks(pushPromptRunnable)
        mainHandler.postDelayed(pushPromptRunnable, PUSH_PROMPT_DELAY_MS)
    }

    private fun assembleHostSurface() {
        val activityBg = ContextCompat.getColor(this, R.color.browser_activity_background)
        val webViewBg = ContextCompat.getColor(this, R.color.browser_webview_background)
        val loaderColor = ContextCompat.getColor(this, R.color.browser_loader_color)

        val rootPane = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(activityBg)
        }

        seamWeb = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            id = View.generateViewId()
            setBackgroundColor(webViewBg)
        }

        loadIndicator = ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            indeterminateTintList = ColorStateList.valueOf(loaderColor)
            visibility = View.VISIBLE
        }

        rootPane.addView(seamWeb)
        rootPane.addView(loadIndicator)
        setContentView(rootPane)

        applyImmersiveChrome()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSeamEngine() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(seamWeb, true)
        }

        seamWeb.apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }

        seamWeb.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            blockNetworkImage = false
            setSupportMultipleWindows(false)
            safeBrowsingEnabled = false
            userAgentString = userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
        }

        seamWeb.setDownloadListener { href, agent, disposition, mime, _ ->
            enqueueDownload(href, agent, disposition, mime)
        }

        seamWeb.webViewClient = buildClientBridge()
        seamWeb.webChromeClient = buildChromeFacade()
    }

    private fun buildClientBridge(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (awaitingFirstPaint) {
                    loadIndicator.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (awaitingFirstPaint) {
                    awaitingFirstPaint = false
                    loadIndicator.visibility = View.GONE
                }
                CookieManager.getInstance().flush()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                CookieManager.getInstance().flush()
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme ?: return false

                if (scheme in listOf("http", "https")) {
                    return false
                }

                return try {
                    val outbound = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }

                    openExternalTarget(view?.context ?: return true, outbound)
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (!isFinishing && !isDestroyed) {
                    recreate()
                }
                return true
            }
        }
    }

    private fun buildChromeFacade(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                pendingUploadSink?.onReceiveValue(null)
                pendingUploadSink = callback

                val acceptTypes = params?.acceptTypes ?: arrayOf("*/*")
                val mimeType = acceptTypes.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "*/*"

                val picker = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mimeType
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(picker, FILE_CHOOSER_CODE)
                    true
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this@BrowserActivity, "File manager not found", Toast.LENGTH_SHORT).show()
                    callback?.onReceiveValue(null)
                    pendingUploadSink = null
                    false
                }
            }
        }
    }

    private fun beginNavigation(href: String) {
        seamWeb.loadUrl(href)
    }

    private fun bindBackStack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (seamWeb.canGoBack()) {
                    seamWeb.goBack()
                }
            }
        })
    }

    private fun enqueueDownload(
        downloadHref: String,
        agent: String,
        disposition: String,
        mimeType: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadHref))
            request.setMimeType(mimeType)

            val cookies = CookieManager.getInstance().getCookie(downloadHref)
            if (!cookies.isNullOrEmpty()) {
                request.addRequestHeader("Cookie", cookies)
            }
            request.addRequestHeader("User-Agent", agent)

            val fileName = URLUtil.guessFileName(downloadHref, disposition, mimeType)
            request.setTitle(fileName)
            request.setDescription("Downloading file...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Download started: $fileName", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalTarget(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is ComponentActivity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveChrome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_CODE) {
            absorbPickerResult(resultCode, data)
        }
    }

    private fun absorbPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()

            data?.data?.let { uri -> uris.add(uri) }

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> uris.add(uri) }
                }
            }

            pendingUploadSink?.onReceiveValue(uris.toTypedArray())
        } else {
            pendingUploadSink?.onReceiveValue(null)
        }

        pendingUploadSink = null
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pushPromptRunnable)
        super.onDestroy()
    }
}
