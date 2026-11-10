package com.google.antigravity.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.antigravity.mobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Notifications enabled for agent updates", Toast.LENGTH_SHORT).show()
                binding.cardNotifications.alpha = 0.5f
                binding.btnEnableNotifications.isEnabled = false
                binding.btnEnableNotifications.text = "Enabled ✓"
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if opened via deep link (e.g. https://antigravity.google.com/...)
        val targetUrl = intent?.data?.toString() ?: CustomTabsHelper.ANTIGRAVITY_URL

        setupNotificationCard()
        setupListeners(targetUrl)
    }

    private fun setupNotificationCard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                binding.cardNotifications.alpha = 0.5f
                binding.btnEnableNotifications.isEnabled = false
                binding.btnEnableNotifications.text = "Enabled ✓"
            } else {
                binding.btnEnableNotifications.setOnClickListener {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 and below automatically grant notification permissions
            binding.cardNotifications.visibility = android.view.View.GONE
        }
    }

    private fun setupListeners(targetUrl: String) {
        // Mode 1: Custom Tabs (Recommended: seamless Google login, zero WebView auth block)
        binding.btnLaunchCustomTabs.setOnClickListener {
            CustomTabsHelper.openAntigravity(this, targetUrl)
        }
        binding.cardCustomTabs.setOnClickListener {
            CustomTabsHelper.openAntigravity(this, targetUrl)
        }

        // Mode 2: In-App WebView
        binding.btnLaunchWebView.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra(WebViewActivity.EXTRA_URL, targetUrl)
            }
            startActivity(intent)
        }
        binding.cardWebView.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra(WebViewActivity.EXTRA_URL, targetUrl)
            }
            startActivity(intent)
        }
    }
}
