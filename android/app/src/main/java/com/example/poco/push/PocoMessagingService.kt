package com.example.poco.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FCM 수신 서비스.
 * - onNewToken: 토큰이 새로 발급/갱신되면 서버에 다시 등록
 * - onMessageReceived: 앱이 "켜져 있을 때" 도착한 푸시를 직접 알림으로 띄움
 *   (앱이 백그라운드/종료 상태면 시스템이 알아서 알림 트레이에 띄워줌)
 */
class PocoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { PushTokenRegistrar.send(applicationContext, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        PushNotifications.show(applicationContext, title, body)
    }
}
