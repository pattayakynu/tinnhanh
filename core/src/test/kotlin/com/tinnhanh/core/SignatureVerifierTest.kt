package com.tinnhanh.core
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class SignatureVerifierTest {
    // Public key thật (Plan 1)
    private val pub = "aVB3ntCzxkCon9By9JoRi9Mpv1kNO8mL2k3zdxGEIW8="
    // Config đã ký thật từ Plan 1 (verify được bằng pub ở trên)
    private val signedJson = """{"payload":"eyJkb21haW5zIjpbImh0dHBzOi8vYW5jaG9pMS54eHgiLCJodHRwczovL2FuY2hvaTEuY29tIiwiaHR0cHM6Ly9nYWlndS5hYyIsImh0dHBzOi8vYW5jaG9pLmFjIl0sImNkbiI6Imh0dHBzOi8vY2RuLmFuY2hvaTEueHh4Iiwib3V0bGluZSI6WyJzczovL1BMQUNFSE9MREVSX1RPSV9QTEFOXzMiXSwibWluVmVyc2lvbiI6MSwibGF0ZXN0QXBrIjoiaHR0cHM6Ly9jZG4uYW5jaG9pMS54eHgvYW5jaG9pLXYxLmFwayIsInB1c2giOiJodHRwczovL250ZnkuYW5jaG9pMS54eHgvYW5jaG9pIiwiaXNzdWVkQXQiOiIyMDI2LTA3LTAzVDAwOjAwOjAwWiJ9","sig":"c0JJiCarGouBgwHgiS1r8fGR6TwIIEYHg1JDDZ4oqVvevBMSFl9u2/6+drX8o1mJxJqxsHJE8K6G5OQeOpzsAw=="}"""

    @Test fun verifyConfigThat() {
        val cfg = SignatureVerifier(pub).verify(signedJson)
        assertNotNull(cfg)
        assertEquals("https://anchoi1.xxx", cfg.domains.first())
        assertEquals("https://cdn.anchoi1.xxx", cfg.cdn)
    }

    @Test fun rejectSaiChuKy() {
        // đổi 1 ký tự trong sig → phải trả null
        val tampered = signedJson.replace("c0JJ", "d0JJ")
        assertNull(SignatureVerifier(pub).verify(tampered))
    }

    @Test fun rejectSaiPublicKey() {
        val wrongPub = "AAAAntCzxkCon9By9JoRi9Mpv1kNO8mL2k3zdxGEIW8="
        assertNull(SignatureVerifier(wrongPub).verify(signedJson))
    }

    @Test fun rejectJsonRac() {
        assertNull(SignatureVerifier(pub).verify("khong-phai-json"))
    }
}
