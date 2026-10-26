package com.aurapulse.studio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile private var instance: MainActivity? = null

        /** Runs a JavaScript snippet inside the player page (used by the notification / lock-screen buttons). */
        fun js(code: String) {
            val a = instance ?: return
            a.runOnUiThread { a.web.evaluateJavascript(code, null) }
        }
    }

    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        web = WebView(this)
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    fileChooser.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        // Back button goes to the page, which decides: go back one screen or show the exit dialog.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("typeof handleAuraBack==='function'&&handleAuraBack()", null)
            }
        })
    }

    // NOTE: the WebView is deliberately NOT paused in onPause(), so music keeps playing
    // when the app is in the background or the screen is locked.
    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopService(Intent(this, NowPlayingService::class.java))
        }
        instance = null
        web.destroy()
        super.onDestroy()
    }

    /** Functions the player page can call as `Android.xxx(...)`. */
    inner class Bridge {
        @JavascriptInterface
        fun updateNowPlaying(title: String, artist: String, playing: Boolean, posMs: Double, durMs: Double, artDataUrl: String) {
            NowPlayingService.title = title
            NowPlayingService.artist = artist
            NowPlayingService.playing = playing
            NowPlayingService.posMs = posMs.toLong().coerceAtLeast(0)
            NowPlayingService.durMs = durMs.toLong().coerceAtLeast(0)
            if (artDataUrl.isNotEmpty()) {
                try {
                    val raw = Base64.decode(artDataUrl.substringAfter(','), Base64.DEFAULT)
                    val bmp: Bitmap? = BitmapFactory.decodeByteArray(raw, 0, raw.size)
                    if (bmp != null) NowPlayingService.art = bmp
                } catch (_: Exception) { }
            }
            val svc = NowPlayingService.instance
            if (svc != null) {
                svc.refresh()
            } else if (title.isNotEmpty()) {
                try {
                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, NowPlayingService::class.java))
                } catch (_: Exception) { }
            }
        }

        @JavascriptInterface
        fun openCast() {
            runOnUiThread {
                try {
                    startActivity(Intent(Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { }
                }
            }
        }

        @JavascriptInterface
        fun exitApp() {
            runOnUiThread {
                stopService(Intent(this@MainActivity, NowPlayingService::class.java))
                finishAndRemoveTask()
            }
        }
    }
}
