package com.tinnhanh.core
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val domains: List<String>,
    val cdn: String,
    val outline: List<String>,
    val minVersion: Int,
    val latestApk: String,
    val push: String,
    val issuedAt: String,
)

@Serializable
data class SignedConfig(
    val payload: String,
    val sig: String,
)
