package com.example.poco

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.poco.location.GeoPoint
import com.example.poco.location.HomeState
import com.example.poco.location.HomeZone
import com.example.poco.location.LocationSample
import com.example.poco.location.LocationStore
import kotlinx.coroutines.launch

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

        val missingPermissions = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions += Manifest.permission.RECORD_AUDIO
        }
        val hasLocationPermission =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) {
            missingPermissions += Manifest.permission.ACCESS_COARSE_LOCATION
            missingPermissions += Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), REQUEST_MONITOR_PERMISSIONS)
        } else {
            startAudioMonitorService()
        }

        setContent {
            PocoScreen()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MONITOR_PERMISSIONS &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioMonitorService()
        }
    }

    private fun startAudioMonitorService() {
        val intent = Intent(this, AudioMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private companion object {
        const val REQUEST_MONITOR_PERMISSIONS = 1
    }
}

@Composable
fun PocoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationStore = remember(context) { LocationStore(context) }
    val initialLocation = remember { locationStore.getLatest() }
    var eventList by remember { mutableStateOf(listOf<SoundEventResponse>()) }
    var monitorStatus by remember { mutableStateOf("마이크 감시 중") }
    var lastResult by remember { mutableStateOf("아직 분류된 소리 없음") }
    var serverStatus by remember { mutableStateOf("서버 대기 중") }
    var currentDb by remember { mutableStateOf<Double?>(null) }
    var currentPeak by remember { mutableStateOf<Int?>(null) }
    var latestLocation by remember { mutableStateOf(initialLocation?.first) }
    var homeState by remember { mutableStateOf(initialLocation?.second ?: HomeState.UNKNOWN) }
    var homeDistanceMeters by remember { mutableStateOf<Double?>(null) }
    var homeStateCertain by remember { mutableStateOf<Boolean?>(null) }
    var locationServerStatus by remember { mutableStateOf("위치 서버 대기 중") }
    var homeZone by remember { mutableStateOf(locationStore.getHomeZone()) }
    val scope = rememberCoroutineScope()
    val visibleEventList = eventList
        .sortedByDescending { it.id }
        .take(50)

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != AudioMonitorService.ACTION_RESULT) return

                intent.getStringExtra(AudioMonitorService.EXTRA_MONITOR_STATUS)?.let {
                    monitorStatus = it
                }
                if (intent.hasExtra(AudioMonitorService.EXTRA_DB)) {
                    currentDb = intent.getDoubleExtra(AudioMonitorService.EXTRA_DB, 0.0)
                }
                if (intent.hasExtra(AudioMonitorService.EXTRA_PEAK)) {
                    currentPeak = intent.getIntExtra(AudioMonitorService.EXTRA_PEAK, 0)
                }
                if (intent.hasExtra(AudioMonitorService.EXTRA_LATITUDE)) {
                    latestLocation = LocationSample(
                        latitude = intent.getDoubleExtra(AudioMonitorService.EXTRA_LATITUDE, 0.0),
                        longitude = intent.getDoubleExtra(AudioMonitorService.EXTRA_LONGITUDE, 0.0),
                        accuracyMeters = intent.getFloatExtra(AudioMonitorService.EXTRA_ACCURACY_METERS, 0f),
                        measuredAtEpochMs = intent.getLongExtra(AudioMonitorService.EXTRA_LOCATION_TIME, 0L)
                    )
                    homeState = runCatching {
                        HomeState.valueOf(
                            intent.getStringExtra(AudioMonitorService.EXTRA_HOME_STATE)
                                ?: HomeState.UNKNOWN.name
                        )
                    }.getOrDefault(HomeState.UNKNOWN)
                    homeDistanceMeters = if (intent.hasExtra(AudioMonitorService.EXTRA_HOME_DISTANCE_METERS)) {
                        intent.getDoubleExtra(AudioMonitorService.EXTRA_HOME_DISTANCE_METERS, 0.0)
                    } else {
                        null
                    }
                    homeStateCertain = if (intent.hasExtra(AudioMonitorService.EXTRA_HOME_STATE_CERTAIN)) {
                        intent.getBooleanExtra(AudioMonitorService.EXTRA_HOME_STATE_CERTAIN, false)
                    } else {
                        null
                    }
                }
                intent.getStringExtra(AudioMonitorService.EXTRA_LOCATION_SERVER_STATUS)?.let {
                    locationServerStatus = it
                }

                val error = intent.getStringExtra(AudioMonitorService.EXTRA_ERROR)
                if (error != null) {
                    monitorStatus = "분류 실패"
                    lastResult = error
                    return
                }

                if (!intent.hasExtra(AudioMonitorService.EXTRA_LABEL)) {
                    return
                }

                val label = intent.getStringExtra(AudioMonitorService.EXTRA_LABEL) ?: return
                val score = intent.getFloatExtra(AudioMonitorService.EXTRA_SCORE, 0f)
                val wavPath = intent.getStringExtra(AudioMonitorService.EXTRA_WAV_PATH).orEmpty()
                monitorStatus = "마이크 감시 중"
                lastResult = "$label / ${"%.3f".format(score)}"
                intent.getStringExtra(AudioMonitorService.EXTRA_SERVER_STATUS)?.let {
                    serverStatus = it
                }
                if (wavPath.isNotBlank()) {
                    lastResult += "\n$wavPath"
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

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        item {
        Text("POCO 소리 감지", fontSize = 28.sp)

        Spacer(modifier = Modifier.height(28.dp))

        Text("감시 상태", fontSize = 20.sp)
        Text(monitorStatus, fontSize = 22.sp)
        Text(
            text = "dB ${currentDb?.let { "%.1f".format(it) } ?: "-"} / peak ${currentPeak ?: "-"}",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("마지막 분류 결과", fontSize = 20.sp)
        Text(lastResult, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(20.dp))

        Text("서버 상태", fontSize = 20.sp)
        Text(serverStatus, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(28.dp))

        Text("GPS / Home Zone", fontSize = 20.sp)
        Text("상태: ${homeState.name}", fontSize = 22.sp)
        Text(
            text = latestLocation?.let {
                "위치: %.6f, %.6f / 정확도 ±%.1fm".format(
                    it.latitude,
                    it.longitude,
                    it.accuracyMeters
                )
            } ?: "아직 수집된 위치 없음",
            fontSize = 16.sp
        )
        homeDistanceMeters?.let { distance ->
            Text(
                "집에서 %.1fm / 판정 ${if (homeStateCertain == true) "확실" else "경계(기존 상태 유지)"}",
                fontSize = 16.sp
            )
        }
        Text(
            homeZone?.let { "Home Zone: 반경 ${it.radiusMeters.toInt()}m 설정됨" }
                ?: "Home Zone이 설정되지 않음",
            fontSize = 16.sp
        )
        Text(locationServerStatus, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = latestLocation != null,
            onClick = {
                latestLocation?.let { sample ->
                    val newHomeZone = HomeZone(
                        center = GeoPoint(sample.latitude, sample.longitude),
                        radiusMeters = HomeZone.DEFAULT_RADIUS_METERS
                    )
                    locationStore.saveHomeZone(newHomeZone)
                    homeZone = newHomeZone
                    homeDistanceMeters = 0.0
                    val newState = if (sample.accuracyMeters <= newHomeZone.radiusMeters) {
                        HomeState.HOME
                    } else {
                        HomeState.UNKNOWN
                    }
                    homeState = newState
                    locationStore.saveLatest(sample, newState)
                    homeStateCertain = sample.accuracyMeters <= newHomeZone.radiusMeters
                }
            }
        ) {
            Text("현재 위치를 집으로 저장 (100m)")
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text("조회된 소리 이벤트", fontSize = 20.sp)

        Spacer(modifier = Modifier.height(12.dp))
        }

        if (visibleEventList.isEmpty()) {
            item {
                Text("아직 조회된 이벤트 없음")
            }
        } else {
            itemsIndexed(visibleEventList) { index, event ->
                Text(
                    text = "${index + 1}. ${event.predLabel} / ${event.predScore} / ${event.startSec}~${event.endSec}초",
                    fontSize = 16.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        serverStatus = "서버 조회 중..."

                        try {
                            val result = ServerApiClient.api.getSoundEvents()
                            eventList = result
                            serverStatus = "서버 조회 성공"
                        } catch (e: Exception) {
                            serverStatus = "서버 조회 실패: ${e.message ?: e::class.java.simpleName}"
                        }
                    }
                }
            ) {
                Text("서버 소리 이벤트 조회")
            }
        }
    }
}
