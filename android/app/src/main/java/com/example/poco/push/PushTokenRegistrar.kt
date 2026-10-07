package com.example.poco.push

import android.content.Context
import android.util.Log
import com.example.poco.FcmTokenRequest
import com.example.poco.ServerApiClient
import com.example.poco.location.LocationStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FCM 토큰을 발급받아 백엔드(PUT /api/users/{userId}/fcm-token)에 등록한다.
 * 푸시는 서버가 "보호자" 계정의 토큰으로 보내므로, 보호자로 로그인한 기기에서 등록돼야 알림을 받는다.
 */
object PushTokenRegistrar {
    private const val TAG = "PocoFCM"

    /** 현재 토큰 발급 -> Logcat 출력 -> (로그인 상태면) 서버 등록 */
    suspend fun register(context: Context) {
        val token = fetchToken()
        if (token == null) {
            Log.w(TAG, "FCM 토큰을 받지 못했어요 (Google Play 서비스 / google-services.json / 인터넷 확인)")
            return
        }
        Log.d(TAG, "FCM token = $token")
        send(context, token)
    }

    /** 토큰이 갱신(onNewToken)됐을 때도 이걸로 서버에 보낸다. 로그인 전이면 조용히 건너뜀. */
    suspend fun send(context: Context, token: String) {
        val userId = LocationStore(context.applicationContext).currentUserId() ?: return
        val result = ServerApiClient.api.updateFcmToken(userId, FcmTokenRequest(token))
        Log.d(TAG, "fcm-token 등록 결과: success=${result.success} message=${result.message}")
    }

    private suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
