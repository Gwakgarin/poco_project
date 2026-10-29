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
    fun `low confidence scream does not alert`() {
        val policy = DangerPolicy()

        assertNull(policy.evaluate("scream", HomeState.HOME, 1L, score = 0.4f))
        assertNull(policy.evaluate("scream", HomeState.OUTSIDE, 2L, score = 0.69f))
        assertEquals(DangerLevel.DANGER, policy.evaluate("scream", HomeState.HOME, 3L, score = 0.7f)?.level)
        assertEquals(DangerLevel.DANGER, policy.evaluate("scream", HomeState.HOME, 4L, score = 0.97f)?.level)
    }

    @Test
    fun `quiet scream does not alert when db gate is set`() {
        val policy = DangerPolicy(screamMinDb = 65.0)

        assertNull(policy.evaluate("scream", HomeState.HOME, 1L, score = 0.95f, segmentDb = 60.0))
        assertEquals(DangerLevel.DANGER, policy.evaluate("scream", HomeState.HOME, 2L, score = 0.95f, segmentDb = 65.0)?.level)
        assertNull(policy.evaluate("scream", HomeState.HOME, 3L, score = 0.5f, segmentDb = 80.0))
    }

    @Test
    fun `default db gate does not block scream`() {
        val policy = DangerPolicy()

        assertEquals(DangerLevel.DANGER, policy.evaluate("scream", HomeState.HOME, 1L, score = 0.9f, segmentDb = 10.0)?.level)
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
