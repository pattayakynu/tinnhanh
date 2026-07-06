package com.tinnhanh.core
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class DiscoveryTest {
    private val testPub = TestVectors.PUBLIC_KEY_B64
    private fun signedFor(vararg domains: String): String = TestVectors.signConfig(domains.toList())

    @Test fun chonAnchorHopLeVaDomainSongDauTien() {
        val live = MockWebServer(); live.enqueue(MockResponse().setResponseCode(200)); live.start()
        val liveUrl = live.url("/").toString().trimEnd('/')

        val anchor = MockWebServer()
        anchor.enqueue(MockResponse().setBody(signedFor("http://127.0.0.1:1/dead", liveUrl)))
        anchor.start()

        val client = OkHttpClient()
        val d = Discovery(client, client, SignatureVerifier(testPub), listOf(anchor.url("/cfg").toString()))
        val result = d.discover()
        assertNotNull(result)
        assertEquals(liveUrl, result.liveDomain)
        anchor.shutdown(); live.shutdown()
    }

    @Test fun anchorDauChetThiNhayAnchorSau() {
        val live = MockWebServer(); live.enqueue(MockResponse().setResponseCode(200)); live.start()
        val liveUrl = live.url("/").toString().trimEnd('/')
        val goodAnchor = MockWebServer(); goodAnchor.enqueue(MockResponse().setBody(signedFor(liveUrl))); goodAnchor.start()

        val client = OkHttpClient()
        val deadAnchorUrl = "http://127.0.0.1:1/cfg"
        val d = Discovery(client, client, SignatureVerifier(testPub), listOf(deadAnchorUrl, goodAnchor.url("/cfg").toString()))
        val result = d.discover()
        assertNotNull(result)
        assertEquals(liveUrl, result.liveDomain)
        goodAnchor.shutdown(); live.shutdown()
    }

    @Test fun configSaiChuKyThiBoQua() {
        val anchor = MockWebServer()
        anchor.enqueue(MockResponse().setBody("""{"payload":"eyJhIjoxfQ==","sig":"YmFk"}"""))
        anchor.start()
        val client = OkHttpClient()
        val d = Discovery(client, client, SignatureVerifier(testPub), listOf(anchor.url("/cfg").toString()))
        assertNull(d.discover())
        anchor.shutdown()
    }

    @Test fun configHopLeNhungKhongDomainNaoSong() {
        val anchor = MockWebServer()
        anchor.enqueue(MockResponse().setBody(signedFor("http://127.0.0.1:1/a", "http://127.0.0.1:2/b")))
        anchor.start()
        val client = OkHttpClient()
        val d = Discovery(client, client, SignatureVerifier(testPub), listOf(anchor.url("/cfg").toString()))
        val result = d.discover()
        // Trả result có config nhưng liveDomain null → Plan 3 (Outline) sẽ lo
        assertNotNull(result)
        assertNull(result.liveDomain)
        anchor.shutdown()
    }
}
