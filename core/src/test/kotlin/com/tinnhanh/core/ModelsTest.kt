package com.tinnhanh.core
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun parseAppConfig() {
        val raw = """{"domains":["https://a.xxx","https://b.com"],"cdn":"https://cdn.a.xxx","outline":["ss://k"],"minVersion":3,"latestApk":"https://cdn.a.xxx/x.apk","push":"https://ntfy.a.xxx/t","issuedAt":"2026-07-03T00:00:00Z"}"""
        val cfg = json.decodeFromString(AppConfig.serializer(), raw)
        assertEquals(listOf("https://a.xxx", "https://b.com"), cfg.domains)
        assertEquals("https://cdn.a.xxx", cfg.cdn)
        assertEquals(3, cfg.minVersion)
    }

    @Test fun parseSignedConfig() {
        val raw = """{"payload":"eyJhIjoxfQ==","sig":"abc"}"""
        val s = json.decodeFromString(SignedConfig.serializer(), raw)
        assertEquals("eyJhIjoxfQ==", s.payload)
        assertEquals("abc", s.sig)
    }
}
