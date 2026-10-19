package com.example.poco.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.poco.MainActivity

/** 푸시 알림용 채널 생성 + 화면에 알림 띄우기 (AudioMonitorService 의 낮은 중요도 채널과는 별개). */
object PushNotifications {
    /** 알림 탭 intent 에 푸시 종류를 담는 키. 백그라운드 푸시는 FCM data 의 "type" 키가 그대로 extra 로 온다. */
    const val EXTRA_PUSH_TYPE = "push_type"
    const val EXTRA_FCM_TYPE = "type"
    /** 서버가 위험 감지(DANGER_ALERT)와 SOS 요청(EMERGENCY) 푸시에 붙이는 type 값. */
    val EMERGENCY_PUSH_TYPES = setOf("DANGER_ALERT", "EMERGENCY")

    const val CHANNEL_ID = "poco_push_alerts" // AndroidManifest 의 default_notification_channel_id 와 같아야 함

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "POCO 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "위험 상황 · 이상 징후 · SOS 알림"
            }
        )
    }

    @SuppressLint("MissingPermission") // 아래에서 권한 확인함
    fun show(context: Context, title: String?, body: String?, type: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 앱이 켜져 있을 때 받은 푸시도, 누르면 백그라운드 푸시와 같은 키로 종류를 넘긴다.
            type?.let { putExtra(EXTRA_PUSH_TYPE, it) }
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title ?: "POCO")
            .setContentText(body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
