package com.tinnhanh.reader

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import mobileproxy.Mobileproxy
import mobileproxy.Proxy
import java.util.concurrent.Executor

/**
 * Lớp 2: bật local HTTP proxy (Outline mobileproxy) từ ss:// key và trỏ WebView qua đó.
 * Chỉ gọi khi Lớp 1 (discovery) không tìm được gương sống.
 */
object ProxyManager {
    private var proxy: Proxy? = null

    /** Khởi động proxy từ ss key. Trả địa chỉ local "127.0.0.1:PORT" hoặc null nếu lỗi. */
    fun start(ssKey: String): String? {
        return try {
            val dialer = Mobileproxy.newStreamDialerFromConfig(ssKey)
            val p = Mobileproxy.runProxy("127.0.0.1:0", dialer)
            proxy = p
            p.address()
        } catch (e: Throwable) {
            null
        }
    }

    /** Trỏ toàn bộ WebView của app qua proxy local. Gọi onReady khi đã áp xong. */
    fun applyToWebView(address: String, executor: Executor, onReady: () -> Unit): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return false
        val config = ProxyConfig.Builder()
            .addProxyRule("http://$address")
            .build()
        ProxyController.getInstance().setProxyOverride(config, executor, Runnable { onReady() })
        return true
    }

    fun stop() {
        try { proxy?.stop(1L) } catch (_: Throwable) {}
        proxy = null
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride({ it.run() }, Runnable {})
        }
    }
}
