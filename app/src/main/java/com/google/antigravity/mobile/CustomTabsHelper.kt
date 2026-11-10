package com.google.antigravity.mobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

object CustomTabsHelper {

    const val ANTIGRAVITY_URL = "https://antigravity.google.com/"

    fun openAntigravity(context: Context, url: String = ANTIGRAVITY_URL) {
        val darkBgColor = ContextCompat.getColor(context, R.color.antigravity_bg)
        val blueColor = ContextCompat.getColor(context, R.color.antigravity_blue)

        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(darkBgColor)
            .setSecondaryToolbarColor(darkBgColor)
            .setNavigationBarColor(darkBgColor)
            .build()

        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorSchemeParams)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()

        try {
            intent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            // Fallback to standard browser intent if Custom Tabs fails
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
        }
    }
}
