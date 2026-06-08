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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
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
        if (requestCode == REQUEST_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioMonitorService()
        }
    }

    private fun startAudioMonitorService() {
        val intent = Intent(this, AudioMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private companion object {
        const val REQUEST_RECORD_AUDIO = 1
    }
}

@Composable
fun PocoScreen() {
    var eventList by remember { mutableStateOf(listOf<SoundEventResponse>()) }
    var monitorStatus by remember { mutableStateOf("마이크 감시 중") }
    var lastResult by remember { mutableStateOf("아직 분류된 소리 없음") }
    var serverStatus by remember { mutableStateOf("서버 대기 중") }
    var currentDb by remember { mutableStateOf<Double?>(null) }
    var currentPeak by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

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

                val error = intent.getStringExtra(AudioMonitorService.EXTRA_ERROR)
                if (error != null) {
                    monitorStatus = "분류 실패"
                    lastResult = error
                    return
                }

                val label = intent.getStringExtra(AudioMonitorService.EXTRA_LABEL) ?: "unknown"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
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

        Text("조회된 소리 이벤트", fontSize = 20.sp)

        Spacer(modifier = Modifier.height(12.dp))

        if (eventList.isEmpty()) {
            Text("아직 조회된 이벤트 없음")
        } else {
            eventList.forEachIndexed { index, event ->
                Text(
                    text = "${index + 1}. ${event.predLabel} / ${event.predScore} / ${event.startSec}~${event.endSec}초",
                    fontSize = 16.sp
                )
            }
        }

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
