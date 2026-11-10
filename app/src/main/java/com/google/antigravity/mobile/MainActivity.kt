package com.google.antigravity.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.antigravity.mobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private var currentInstance = "Remote Agents (Google Cloud)"
    private val instances = arrayOf(
        "Remote Agents (Google Cloud)",
        "Workstation Primary (Remote Control)",
        "Custom Machine URL..."
    )

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

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Antigravity push alerts enabled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val targetUrl = intent?.data?.toString() ?: CustomTabsHelper.ANTIGRAVITY_URL

        setupNotificationPermissions()
        setupWebView()
        setupUIListeners()
        setupBackNavigation()

        loadUrl(targetUrl)
    }

    private fun setupNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            // Set a modern mobile user agent with Google Antigravity identifier
            val defaultUserAgent = settings.userAgentString
            settings.userAgentString = defaultUserAgent

            webChromeClient = AntigravityChromeClient()
            webViewClient = AntigravityWebClient()
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)
    }

    private fun setupUIListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }

        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }

        binding.btnRetry.setOnClickListener {
            binding.layoutOffline.visibility = View.GONE
            binding.swipeRefresh.visibility = View.VISIBLE
            binding.webView.reload()
        }

        // Instance Switcher Chip
        binding.chipInstance.setOnClickListener {
            showInstanceSwitcher()
        }

        // More options
        binding.btnMoreOptions.setOnClickListener {
            showOptionsMenu()
        }

        // Quick Slash Commands
        binding.chipDiff.setOnClickListener { injectSlashCommand("/diff") }
        binding.chipGoal.setOnClickListener { injectSlashCommand("/goal") }
        binding.chipPlan.setOnClickListener { injectSlashCommand("/plan") }
        binding.chipTeamwork.setOnClickListener { injectSlashCommand("/teamwork") }
        binding.chipGrillMe.setOnClickListener { injectSlashCommand("/grill-me") }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_agents -> {
                    loadUrl(CustomTabsHelper.ANTIGRAVITY_URL)
                    true
                }
                R.id.nav_artifacts -> {
                    loadUrl("https://antigravity.google.com/remote")
                    true
                }
                R.id.nav_settings -> {
                    showInstanceSwitcher()
                    true
                }
                else -> false
            }
        }
    }

    private fun showInstanceSwitcher() {
        val checkedItem = when (currentInstance) {
            instances[0] -> 0
            instances[1] -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Switch Antigravity Instance")
            .setSingleChoiceItems(instances, checkedItem) { dialog, which ->
                currentInstance = instances[which]
                binding.chipInstance.text = when (which) {
                    0 -> "☁️ Remote Agents"
                    1 -> "💻 Workstation"
                    else -> "🔗 Custom Host"
                }
                dialog.dismiss()
                binding.webView.reload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOptionsMenu() {
        val options = arrayOf(
            "Clear Cache & Reload",
            "Switch Layout (Software / Knowledge)",
            "Open in Chrome Custom Tabs",
            "About Google Antigravity Mobile"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Antigravity Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        binding.webView.clearCache(true)
                        binding.webView.reload()
                    }
                    1 -> {
                        Toast.makeText(this, "Opening Layout Settings...", Toast.LENGTH_SHORT).show()
                        loadUrl("https://antigravity.google.com/settings")
                    }
                    2 -> {
                        val currentUrl = binding.webView.url ?: CustomTabsHelper.ANTIGRAVITY_URL
                        CustomTabsHelper.openAntigravity(this, currentUrl)
                    }
                    3 -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Google Antigravity Mobile")
                            .setMessage("Official mobile companion app for Google Antigravity (antigravity.google.com).\n\nVersion: 1.1.0\nTarget: Agent-first development")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun injectSlashCommand(command: String) {
        val js = """
            (function() {
                var input = document.querySelector('textarea, [contenteditable="true"], input[type="text"]');
                if (input) {
                    input.focus();
                    input.value = '$command ' + (input.value || '');
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                }
            })();
        """.trimIndent()
        binding.webView.evaluateJavascript(js, null)
        Toast.makeText(this, "Inserted $command", Toast.LENGTH_SHORT).show()
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

        override fun onPermissionRequest(request: PermissionRequest?) {
            // Support voice mode / audio permissions
            request?.grant(request.resources)
        }
    }

    private inner class AntigravityWebClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false

            // Google OAuth delegation to prevent "403: disallowed_useragent"
            if (url.contains("accounts.google.com") || url.contains("oauth2") || url.contains("signin/v2")) {
                CustomTabsHelper.openAntigravity(this@MainActivity, url)
                return true
            }

            if (url.startsWith("https://antigravity.google.com") || url.startsWith("https://antigravity.google")) {
                return false
            }

            CustomTabsHelper.openAntigravity(this@MainActivity, url)
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
