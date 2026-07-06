package com.tinnhanh.core
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64

class SignatureVerifier(publicKeyB64: String) {
    private val publicKey: ByteArray = Base64.getDecoder().decode(publicKeyB64)
    private val json = Json { ignoreUnknownKeys = true }

    /** Verify chữ ký; trả AppConfig nếu hợp lệ, null nếu sai/không parse được. */
    fun verify(signedConfigJson: String): AppConfig? {
        val signed = try {
            json.decodeFromString(SignedConfig.serializer(), signedConfigJson)
        } catch (e: Exception) {
            return null
        }
        val payloadBytes = try {
            Base64.getDecoder().decode(signed.payload)
        } catch (e: Exception) {
            return null
        }
        val sigBytes = try {
            Base64.getDecoder().decode(signed.sig)
        } catch (e: Exception) {
            return null
        }
        if (publicKey.size != 32) return null
        val ok = try {
            val params = Ed25519PublicKeyParameters(publicKey, 0)
            val verifier = Ed25519Signer()
            verifier.init(false, params)
            verifier.update(payloadBytes, 0, payloadBytes.size)
            verifier.verifySignature(sigBytes)
        } catch (e: Exception) {
            false
        }
        if (!ok) return null
        return try {
            json.decodeFromString(AppConfig.serializer(), String(payloadBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }
}
