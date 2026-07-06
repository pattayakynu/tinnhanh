package com.tinnhanh.core
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnchorClientTest {
    @Test fun trichJsonTuHtmlTelegram() {
        // HTML Telegram escape dấu " thành &quot;
        val html = """<div class="tgme_widget_message_text">{&quot;payload&quot;:&quot;eyJhIjoxfQ==&quot;,&quot;sig&quot;:&quot;abc123&quot;}</div>"""
        val out = extractSignedConfigFromHtml(html)
        assertEquals("""{"payload":"eyJhIjoxfQ==","sig":"abc123"}""", out)
    }

    @Test fun htmlKhongCoConfigTraNull() {
        assertNull(extractSignedConfigFromHtml("<div>không có gì</div>"))
    }

    @Test fun fetchAnchorThuong() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"payload":"p","sig":"s"}"""))
        server.start()
        val url = server.url("/app-config.json").toString()
        val client = OkHttpClient()
        val text = AnchorClient(client).fetch(url)
        assertEquals("""{"payload":"p","sig":"s"}""", text)
        server.shutdown()
    }

    @Test fun fetchAnchorTelegramTrichJson() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""<div class="tgme_widget_message_text">{&quot;payload&quot;:&quot;p&quot;,&quot;sig&quot;:&quot;s&quot;}</div>"""))
        server.start()
        val url = server.url("/s/anchorap").toString()
        val text = AnchorClient(OkHttpClient()).fetch(url)
        assertEquals("""{"payload":"p","sig":"s"}""", text)
        server.shutdown()
    }
}
