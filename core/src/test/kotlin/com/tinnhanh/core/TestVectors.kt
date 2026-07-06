package com.tinnhanh.core
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64

/** Cặp khóa test cố định (KHÔNG phải khóa production) để ký config trong unit test. */
object TestVectors {
    // seed 32 byte cố định
    private val seed = ByteArray(32) { it.toByte() }
    private val priv = Ed25519PrivateKeyParameters(seed, 0)
    val PUBLIC_KEY_B64: String = Base64.getEncoder().encodeToString(priv.generatePublicKey().encoded)

    fun signConfig(domains: List<String>): String {
        val domainsJson = domains.joinToString(",") { "\"$it\"" }
        val payloadJson = """{"domains":[$domainsJson],"cdn":"https://cdn.test","outline":["ss://x"],"minVersion":1,"latestApk":"https://cdn.test/x.apk","push":"https://ntfy.test/t","issuedAt":"2026-07-03T00:00:00Z"}"""
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val signer = Ed25519Signer()
        signer.init(true, priv)
        signer.update(payloadBytes, 0, payloadBytes.size)
        val sig = signer.generateSignature()
        val enc = Base64.getEncoder()
        return """{"payload":"${enc.encodeToString(payloadBytes)}","sig":"${enc.encodeToString(sig)}"}"""
    }
}
