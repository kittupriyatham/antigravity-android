# Keep JavascriptInterface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebKit and Browser CustomTabs classes
-keep class androidx.browser.customtabs.** { *; }
-keep class androidx.webkit.** { *; }
