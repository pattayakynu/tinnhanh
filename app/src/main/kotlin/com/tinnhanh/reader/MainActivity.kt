package com.tinnhanh.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.tinnhanh.core.Discovery
import com.tinnhanh.core.DohDns
import com.tinnhanh.core.SignatureVerifier
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val bg = Executors.newSingleThreadExecutor()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Discovery chạy nền, xong thì load domain sống vào WebView (main thread).
        bg.execute {
            val target = runDiscovery()
            runOnUiThread { webView.loadUrl(target) }
        }
    }

    private fun runDiscovery(): String {
        return try {
            val bootstrap = OkHttpClient()
            val dohClient = OkHttpClient.Builder().dns(DohDns(bootstrap)).build()
            val discovery = Discovery(
                anchorClientHttp = dohClient,
                probeHttp = OkHttpClient(),
                verifier = SignatureVerifier(Config.PUBLIC_KEY_B64),
                anchors = Config.ANCHORS,
            )
            discovery.discover()?.liveDomain ?: Config.FALLBACK_DOMAIN
        } catch (e: Exception) {
            Config.FALLBACK_DOMAIN
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
