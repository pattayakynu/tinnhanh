package com.tinnhanh.core
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateDecisionTest {
    private fun cfg(min: Int, latest: Int) = AppConfig(
        domains = listOf("https://a.xxx"), cdn = "https://cdn.a.xxx", outline = listOf("ss://k"),
        minVersion = min, latestVersion = latest, latestApk = "https://x/app.apk",
        push = "https://ntfy/x", issuedAt = "2026-07-07T00:00:00Z",
    )

    @Test fun duMoiThiKhongCanUpdate() {
        assertEquals(UpdateAction.NONE, decideUpdate(5, cfg(min = 1, latest = 5)))
    }

    @Test fun coBanMoiHonThiNhacMem() {
        assertEquals(UpdateAction.SOFT, decideUpdate(4, cfg(min = 1, latest = 5)))
    }

    @Test fun duoiMinThiBatBuoc() {
        assertEquals(UpdateAction.FORCE, decideUpdate(2, cfg(min = 3, latest = 5)))
    }

    @Test fun configCuThieuLatestVersionThiKhongNhacSai() {
        // latestVersion mặc định 0 → không bao giờ SOFT sai
        assertEquals(UpdateAction.NONE, decideUpdate(1, cfg(min = 1, latest = 0)))
    }
}
