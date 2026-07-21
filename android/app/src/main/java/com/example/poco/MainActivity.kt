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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.poco.ui.PocoNavHost
import com.example.poco.ui.screens.UserHomeUiState
import com.example.poco.ui.theme.POCOTheme
import java.text.SimpleDateFormat
import java.util.Locale

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
            POCOTheme {
                PocoRoot()
            }
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

private val lastCheckedFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)

@Composable
private fun PocoRoot() {
    val context = LocalContext.current
    var monitorStatus by remember { mutableStateOf("마이크 감시 중") }
    var lastCheckedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryPercent by remember { mutableStateOf(readBatteryPercent(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != AudioMonitorService.ACTION_RESULT) return

                intent.getStringExtra(AudioMonitorService.EXTRA_MONITOR_STATUS)?.let {
                    monitorStatus = it
                }
                if (intent.getStringExtra(AudioMonitorService.EXTRA_ERROR) != null) {
                    monitorStatus = "분류 실패"
                }
                lastCheckedAt = System.currentTimeMillis()
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

    val micOn = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val isDetecting = monitorStatus != "분류 실패"
    val statusLabel = if (isDetecting) "양호" else "확인 필요"

    PocoNavHost(
        homeUiState = UserHomeUiState(
            isDetecting = isDetecting,
            statusLabel = statusLabel,
            lastCheckedLabel = "마지막 확인 · ${lastCheckedFormat.format(lastCheckedAt)}",
            micOn = micOn,
            gpsOn = isGpsEnabled(context),
            batteryPercent = batteryPercent
        )
    )
}

private fun readBatteryPercent(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

private fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}
