package com.tinnhanh.core
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress

/** Trích danh sách IPv4 (type=1) từ JSON trả về của DoH (application/dns-json). */
fun parseDohAnswer(body: String): List<String> {
    val root = Json.parseToJsonElement(body).jsonObject
    val answer = root["Answer"] ?: return emptyList()
    return answer.jsonArray.mapNotNull { el ->
        val obj = el.jsonObject
        val type = obj["type"]?.jsonPrimitive?.content?.toIntOrNull()
        if (type == 1) obj["data"]?.jsonPrimitive?.content else null
    }
}

/**
 * OkHttp Dns dùng DoH (bỏ qua DNS hệ thống bị đầu độc).
 * bootstrapClient: client thường (DNS hệ thống) chỉ để gọi TỚI máy chủ DoH.
 * dohUrls: danh sách endpoint DoH JSON, thử lần lượt (Cloudflare rồi Google).
 */
class DohDns(
    private val bootstrapClient: OkHttpClient,
    private val dohUrls: List<String> = listOf(
        "https://cloudflare-dns.com/dns-query",
        "https://dns.google/resolve",
    ),
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        // Nếu hostname vốn là IP literal thì trả thẳng, khỏi hỏi DoH.
        if (hostname.matches(Regex("^[0-9.]+$"))) {
            return listOf(InetAddress.getByName(hostname))
        }
        for (url in dohUrls) {
            try {
                val req = Request.Builder()
                    .url("$url?name=$hostname&type=A")
                    .header("Accept", "application/dns-json")
                    .build()
                bootstrapClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val ips = parseDohAnswer(resp.body?.string() ?: "")
                    if (ips.isNotEmpty()) {
                        return ips.map { InetAddress.getByName(it) }
                    }
                }
            } catch (e: Exception) {
                // thử endpoint DoH kế
            }
        }
        // hết cách: rơi về DNS hệ thống (còn hơn ném lỗi cứng)
        return Dns.SYSTEM.lookup(hostname)
    }
}
