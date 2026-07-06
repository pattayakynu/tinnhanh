package com.tinnhanh.core
import okhttp3.OkHttpClient
import okhttp3.Request

private val SIGNED_RE = Regex("""\{"payload":"[^"]+","sig":"[^"]+"\}""")

/** Gỡ HTML-escape rồi trích khối {"payload":..,"sig":..} đầu tiên. Null nếu không có. */
fun extractSignedConfigFromHtml(html: String): String? {
    val unescaped = html
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#47;", "/")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
    return SIGNED_RE.find(unescaped)?.value
}

/**
 * Lấy nội dung config đã ký từ một anchor.
 * Nhận diện theo NỘI DUNG (không theo URL): body là JSON (bắt đầu '{') → trả thẳng;
 * là HTML (vd trang Telegram) → trích khối {"payload":..,"sig":..}.
 */
class AnchorClient(private val client: OkHttpClient) {
    fun fetch(anchorUrl: String): String? {
        return try {
            val req = Request.Builder().url(anchorUrl).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val trimmed = body.trim()
                if (trimmed.startsWith("{")) trimmed else extractSignedConfigFromHtml(body)
            }
        } catch (e: Exception) {
            null
        }
    }
}
