package com.example.poco.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GPS 정확도를 포함한 HOME/OUTSIDE/경계 판정 규칙을 확인한다. */
class HomeStateMachineTest {
    private val home = HomeZone(GeoPoint(37.5665, 126.9780), radiusMeters = 100.0)

    @Test
    fun `inside including accuracy changes state to home`() {
        val machine = HomeStateMachine()
        val result = machine.update(sample(latitudeOffsetMeters = 50.0, accuracy = 10f), home)

        assertEquals(HomeState.HOME, result.state)
        assertTrue(result.changed)
        assertTrue(result.isCertain)
    }

    @Test
    fun `outside excluding accuracy changes state to outside`() {
        val machine = HomeStateMachine(HomeState.HOME)
        val result = machine.update(sample(latitudeOffsetMeters = 150.0, accuracy = 20f), home)

        assertEquals(HomeState.OUTSIDE, result.state)
        assertTrue(result.changed)
    }

    @Test
    fun `uncertain reading keeps previous state`() {
        val machine = HomeStateMachine(HomeState.HOME)
        val result = machine.update(sample(latitudeOffsetMeters = 105.0, accuracy = 20f), home)

        assertEquals(HomeState.HOME, result.state)
        assertFalse(result.changed)
        assertFalse(result.isCertain)
    }

    @Test
    fun `uncertain first reading remains unknown`() {
        val machine = HomeStateMachine()
        val result = machine.update(sample(latitudeOffsetMeters = 95.0, accuracy = 20f), home)

        assertEquals(HomeState.UNKNOWN, result.state)
        assertFalse(result.isCertain)
    }

    private fun sample(latitudeOffsetMeters: Double, accuracy: Float): LocationSample {
        val latitudeOffsetDegrees = latitudeOffsetMeters / 111_320.0
        return LocationSample(
            latitude = home.center.latitude + latitudeOffsetDegrees,
            longitude = home.center.longitude,
            accuracyMeters = accuracy,
            measuredAtEpochMs = 1L
        )
    }
}
