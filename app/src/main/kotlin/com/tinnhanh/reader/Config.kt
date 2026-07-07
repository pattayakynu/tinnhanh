package com.tinnhanh.reader

object Config {
    const val PUBLIC_KEY_B64 = "aVB3ntCzxkCon9By9JoRi9Mpv1kNO8mL2k3zdxGEIW8="

    val ANCHORS = listOf(
        "https://anchoi-config.nguoidepgai123.workers.dev/app-config.json",
        "https://raw.githubusercontent.com/pattayakynu/app-config/refs/heads/main/app-config.signed.json",
        "https://t.me/s/anchorap",
    )

    // domain load tạm khi discovery chưa xong / thất bại (gương ưu tiên nhất)
    const val FALLBACK_DOMAIN = "https://anchoi2.xxx"

    // TẠM: ép luôn đi qua proxy (Lớp 2) để test trên máy thật. Gỡ về false sau khi test.
    const val FORCE_PROXY_TEST = true
}
