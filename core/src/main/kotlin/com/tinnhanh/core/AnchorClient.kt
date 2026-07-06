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

/** Lấy nội dung config đã ký từ một anchor. Với URL Telegram (chứa "t.me/") thì trích từ HTML. */
class AnchorClient(private val client: OkHttpClient) {
    fun fetch(anchorUrl: String): String? {
        return try {
            val req = Request.Builder().url(anchorUrl).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                if (anchorUrl.contains("t.me/")) extractSignedConfigFromHtml(body) else body.trim()
            }
        } catch (e: Exception) {
            null
        }
    }
}
