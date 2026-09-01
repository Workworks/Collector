package com.kfaino.diapertracker

/** Pure timeout policy for clearing an authenticated app-lock session after backgrounding. */
object BackgroundLockPolicy {
    const val DEFAULT_TIMEOUT_MS = 60_000L

    fun shouldRelock(backgroundAtMs: Long, foregroundAtMs: Long, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        if (backgroundAtMs <= 0L || foregroundAtMs < backgroundAtMs) return false
        return foregroundAtMs - backgroundAtMs >= timeoutMs.coerceAtLeast(0L)
    }
}
