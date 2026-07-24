package com.example.poco

import com.example.poco.location.HomeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/** scream과 반복 car_horn 위험 정책이 명세대로 동작하는지 확인한다. */
class DangerPolicyTest {
    @Test
    fun `scream alerts regardless of home state`() {
        val policy = DangerPolicy()

        assertEquals(DangerLevel.DANGER, policy.evaluate("scream", HomeState.HOME, 1L)?.level)
        assertEquals(DangerLevel.DANGER, policy.evaluate("scream", HomeState.OUTSIDE, 2L)?.level)
    }

    @Test
    fun `car horn does not alert at home`() {
        val policy = DangerPolicy(hornRepeatCount = 3)

        repeat(3) { index ->
            assertNull(policy.evaluate("car_horn", HomeState.HOME, index * 1_000L))
        }
    }

    @Test
    fun `repeated car horn alerts only while outside`() {
        val policy = DangerPolicy(hornRepeatCount = 3, hornWindowMs = 30_000L)

        assertNull(policy.evaluate("car_horn", HomeState.OUTSIDE, 0L))
        assertNull(policy.evaluate("car_horn", HomeState.OUTSIDE, 10_000L))
        assertNotNull(policy.evaluate("car_horn", HomeState.OUTSIDE, 20_000L))
    }

    @Test
    fun `old car horn detections expire`() {
        val policy = DangerPolicy(hornRepeatCount = 3, hornWindowMs = 30_000L)

        assertNull(policy.evaluate("car_horn", HomeState.OUTSIDE, 0L))
        assertNull(policy.evaluate("car_horn", HomeState.OUTSIDE, 10_000L))
        assertNull(policy.evaluate("car_horn", HomeState.OUTSIDE, 40_001L))
    }
}
