package com.tinnhanh.core
import okhttp3.OkHttpClient
import okhttp3.Request

data class DiscoveryResult(val liveDomain: String?, val config: AppConfig)

/**
 * anchorClientHttp: OkHttp có DoH (đọc anchor kể cả khi bị chặn DNS).
 * probeHttp: OkHttp DNS thường (kiểm tra domain có vào được KIỂU WEBVIEW không).
 */
class Discovery(
    anchorClientHttp: OkHttpClient,
    private val probeHttp: OkHttpClient,
    private val verifier: SignatureVerifier,
    private val anchors: List<String>,
) {
    private val anchorClient = AnchorClient(anchorClientHttp)

    fun discover(): DiscoveryResult? {
        for (anchor in anchors) {
            val text = anchorClient.fetch(anchor) ?: continue
            val config = verifier.verify(text) ?: continue
            val live = config.domains.firstOrNull { isReachable(it) }
            return DiscoveryResult(live, config)
        }
        return null
    }

    private fun isReachable(domain: String): Boolean {
        return try {
            val req = Request.Builder().url(domain).head().header("User-Agent", "Mozilla/5.0").build()
            probeHttp.newCall(req).execute().use { it.isSuccessful || it.code in 300..399 }
        } catch (e: Exception) {
            false
        }
    }
}
