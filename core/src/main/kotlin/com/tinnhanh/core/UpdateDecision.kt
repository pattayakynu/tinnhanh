package com.tinnhanh.core

enum class UpdateAction { NONE, SOFT, FORCE }

/** So version hiện tại với config. FORCE nếu dưới minVersion; SOFT nếu có bản mới hơn; NONE nếu đủ mới. */
fun decideUpdate(currentVersion: Int, config: AppConfig): UpdateAction {
    return when {
        currentVersion < config.minVersion -> UpdateAction.FORCE
        currentVersion < config.latestVersion -> UpdateAction.SOFT
        else -> UpdateAction.NONE
    }
}
