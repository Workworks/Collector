package com.kfaino.diapertracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundLockPolicyTest {
    @Test fun `background timeout locks at exact boundary`() {
        assertFalse(BackgroundLockPolicy.shouldRelock(1_000, 60_999, 60_000))
        assertTrue(BackgroundLockPolicy.shouldRelock(1_000, 61_000, 60_000))
    }

    @Test fun `invalid or reset timestamps do not lock`() {
        assertFalse(BackgroundLockPolicy.shouldRelock(0, 90_000))
        assertFalse(BackgroundLockPolicy.shouldRelock(10_000, 9_000))
    }
}
