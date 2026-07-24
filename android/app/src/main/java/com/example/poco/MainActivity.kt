package com.example.poco

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.poco.location.GeoPoint
import com.example.poco.location.HomeState
import com.example.poco.location.HomeZone
import com.example.poco.location.LocationSample
import com.example.poco.location.LocationStore
import com.example.poco.ui.PocoNavHost
import com.example.poco.ui.screens.UserHomeUiState
import com.example.poco.ui.theme.POCOTheme
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

data class SoundEventResponse(
    val id: Long,
    val rawFile: String,
    val splitFile: String,
    val predLabel: String,
    val predScore: Double,
    val segIndex: Int,
    val startSec: Int,
    val endSec: Int,
    val smoothedLabel: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val missingPermissions = buildList {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            val hasLocationPermission =
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasLocationPermission) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (missingPermissions.isEmpty()) {
            startAudioMonitorService()
        } else {
            requestPermissions(missingPermissions.toTypedArray(), REQUEST_MONITOR_PERMISSIONS)
        }

        setContent {
            POCOTheme { PocoRoot() }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MONITOR_PERMISSIONS &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioMonitorService()
        }
    }

    private fun startAudioMonitorService() {
        ContextCompat.startForegroundService(this, Intent(this, AudioMonitorService::class.java))
    }

    private companion object {
        const val REQUEST_MONITOR_PERMISSIONS = 1
    }
}

private val lastCheckedFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)

@Composable
private fun PocoRoot() {
    val context = LocalContext.current
    val locationStore = remember(context) { LocationStore(context) }
    val initialLocation = remember { locationStore.getLatest() }
    var monitorError by remember { mutableStateOf<String?>(null) }
    var lastAudioSignalAt by remember { mutableStateOf<Long?>(null) }
    var currentDb by remember { mutableStateOf<Double?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryPercent by remember { mutableStateOf(readBatteryPercent(context)) }
    var latestLocation by remember { mutableStateOf(initialLocation?.first) }
    var homeState by remember { mutableStateOf(initialLocation?.second ?: HomeState.UNKNOWN) }
    var homeDistanceMeters by remember { mutableStateOf<Double?>(null) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != AudioMonitorService.ACTION_RESULT) return
                intent.getStringExtra(AudioMonitorService.EXTRA_ERROR)?.let {
                    monitorError = it
                }
                if (intent.hasExtra(AudioMonitorService.EXTRA_DB)) {
                    currentDb = intent.getDoubleExtra(AudioMonitorService.EXTRA_DB, 0.0)
                    lastAudioSignalAt = System.currentTimeMillis()
                    monitorError = null
                }
                if (intent.hasExtra(AudioMonitorService.EXTRA_LATITUDE)) {
                    latestLocation = LocationSample(
                        latitude = intent.getDoubleExtra(AudioMonitorService.EXTRA_LATITUDE, 0.0),
                        longitude = intent.getDoubleExtra(AudioMonitorService.EXTRA_LONGITUDE, 0.0),
                        accuracyMeters = intent.getFloatExtra(AudioMonitorService.EXTRA_ACCURACY_METERS, 0f),
                        measuredAtEpochMs = intent.getLongExtra(AudioMonitorService.EXTRA_LOCATION_TIME, 0L)
                    )
                    homeState = runCatching {
                        HomeState.valueOf(intent.getStringExtra(AudioMonitorService.EXTRA_HOME_STATE).orEmpty())
                    }.getOrDefault(HomeState.UNKNOWN)
                    homeDistanceMeters = if (intent.hasExtra(AudioMonitorService.EXTRA_HOME_DISTANCE_METERS)) {
                        intent.getDoubleExtra(AudioMonitorService.EXTRA_HOME_DISTANCE_METERS, 0.0)
                    } else null
                }
            }
        }
        val filter = IntentFilter(AudioMonitorService.ACTION_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                batteryPercent = readBatteryPercent(context ?: return)
            }
        }
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
            context.unregisterReceiver(batteryReceiver)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val micOn = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val locationPermissionGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val audioSignalAge = lastAudioSignalAt?.let { currentTime - it }
    val isDetecting = micOn && monitorError == null && audioSignalAge != null &&
        audioSignalAge <= AUDIO_SIGNAL_TIMEOUT_MS

    PocoNavHost(
        homeUiState = UserHomeUiState(
            isDetecting = isDetecting,
            statusLabel = if (isDetecting) "실제 감지 중" else "감지 확인 필요",
            detectionLabel = when {
                monitorError != null -> "오디오 오류"
                !micOn -> "마이크 권한 필요"
                lastAudioSignalAt == null -> "오디오 신호 대기 중"
                audioSignalAge != null && audioSignalAge > AUDIO_SIGNAL_TIMEOUT_MS -> "오디오 신호 끊김"
                else -> "실시간 소리 감지 중"
            },
            soundLevelLabel = currentDb?.let { "현재 입력 · %.1f dB".format(it) }
                ?: "아직 측정된 소리가 없습니다",
            lastCheckedLabel = lastAudioSignalAt?.let { "마지막 오디오 신호 · ${lastCheckedFormat.format(it)}" }
                ?: "서비스에서 오디오 신호를 기다리는 중입니다",
            micOn = micOn,
            gpsOn = locationPermissionGranted && isGpsEnabled(context),
            batteryPercent = batteryPercent,
            homeStateLabel = when (homeState) {
                HomeState.HOME -> "집 안"
                HomeState.OUTSIDE -> "외출 중"
                HomeState.UNKNOWN -> "위치 미확인"
            },
            locationSummary = latestLocation?.let { sample ->
                val distance = homeDistanceMeters?.let { " · 집에서 %.0fm".format(it) }.orEmpty()
                "정확도 ±%.0fm%s".format(sample.accuracyMeters, distance)
            } ?: "아직 수집된 위치가 없습니다",
            canSaveHome = latestLocation != null
        ),
        onSaveCurrentLocationAsHome = {
            latestLocation?.let { sample ->
                val zone = HomeZone(GeoPoint(sample.latitude, sample.longitude))
                locationStore.saveHomeZone(zone)
                val nextState = if (sample.accuracyMeters <= zone.radiusMeters) HomeState.HOME else HomeState.UNKNOWN
                locationStore.saveLatest(sample, nextState)
                homeState = nextState
                homeDistanceMeters = 0.0
            }
        }
    )
}

private const val AUDIO_SIGNAL_TIMEOUT_MS = 3_000L

private fun readBatteryPercent(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

private fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}
