package com.google.antigravity.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.antigravity.mobile.databinding.ActivityWebviewBinding

class WebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_target_url"
    }

    private lateinit var binding: ActivityWebviewBinding
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val results = when {
                    intent?.data != null -> arrayOf(intent.data!!)
                    intent?.clipData != null -> {
                        val clip = intent.clipData!!
                        Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                    }
                    else -> null
                }
                fileUploadCallback?.onReceiveValue(results)
            } else {
                fileUploadCallback?.onReceiveValue(null)
            }
            fileUploadCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }

        val startUrl = intent.getStringExtra(EXTRA_URL) ?: CustomTabsHelper.ANTIGRAVITY_URL

        setupWebView()
        setupListeners()
        setupBackNavigation()

        loadUrl(startUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // Enable caching for fast reload
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            // Enable mobile viewport
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            // Setup custom clients
            webChromeClient = AntigravityChromeClient()
            webViewClient = AntigravityWebClient()
        }

        // Enable cookies & third-party cookies for Google authentication
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }

        binding.btnRetry.setOnClickListener {
            binding.layoutOffline.visibility = View.GONE
            binding.swipeRefresh.visibility = View.VISIBLE
            binding.webView.reload()
        }

        binding.btnWebBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }

        binding.btnWebForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }

        binding.btnWebRefresh.setOnClickListener {
            binding.webView.reload()
        }

        binding.btnOpenCustomTabs.setOnClickListener {
            val currentUrl = binding.webView.url ?: CustomTabsHelper.ANTIGRAVITY_URL
            CustomTabsHelper.openAntigravity(this, currentUrl)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun loadUrl(url: String) {
        binding.layoutOffline.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
    }

    private inner class AntigravityChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress < 100) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.progress = newProgress
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = filePathCallback

            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            try {
                filePickerLauncher.launch(intent)
            } catch (e: Exception) {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = null
                return false
            }
            return true
        }
    }

    private inner class AntigravityWebClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false

            // CRITICAL: Google OAuth explicitly blocks embedded WebViews with "403: disallowed_useragent".
            // If the user triggers Google Authentication or an external service, route to Chrome Custom Tabs!
            if (url.contains("accounts.google.com") || url.contains("oauth2") || url.contains("signin/v2")) {
                CustomTabsHelper.openAntigravity(this@WebViewActivity, url)
                return true
            }

            // Keep internal antigravity.google.com and antigravity.google links inside the WebView
            if (url.startsWith("https://antigravity.google.com") || url.startsWith("https://antigravity.google")) {
                return false
            }

            // Open external external links (e.g. GitHub repos, documentation) in Custom Tabs
            CustomTabsHelper.openAntigravity(this@WebViewActivity, url)
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressBar.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                binding.swipeRefresh.visibility = View.GONE
                binding.layoutOffline.visibility = View.VISIBLE
            }
        }
    }
}
