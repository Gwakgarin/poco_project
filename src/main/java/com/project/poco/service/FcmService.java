package com.project.poco.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.project.poco.config.FirebaseConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * FCM 발송 최하단 레이어. "누구한테 보낼지" 판단은 여기가 아니라
 * PushNotificationService가 하고, 여기는 토큰 하나 받아서 실제로 보내기만 한다.
 */
@Service
@RequiredArgsConstructor
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final FirebaseConfig firebaseConfig;

    /**
     * @param fcmToken 받는 사람의 FCM 토큰
     * @param title    알림 제목
     * @param body     알림 본문
     * @param data     앱이 알림 눌렀을 때 쓸 추가 데이터 (예: type=EMERGENCY, deviceId=3). null 가능.
     * @return 발송 성공 여부
     */
    public boolean send(String fcmToken, String title, String body, Map<String, String> data) {
        if (!firebaseConfig.isInitialized()) {
            log.warn("[FCM] Firebase가 초기화되지 않아 발송을 건너뜁니다. title={}", title);
            return false;
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("[FCM] 토큰이 없어 발송을 건너뜁니다. title={}", title);
            return false;
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null) {
            messageBuilder.putAllData(data);
        }

        try {
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("[FCM] 발송 성공: {} (messageId={})", title, response);
            return true;
        } catch (FirebaseMessagingException e) {
            // 토큰이 만료/무효(UNREGISTERED)된 경우가 흔함 - 앱 재설치, 로그아웃 등.
            // 지금은 로그만 남기고 넘어가지만, 나중에 여유 있으면 호출부에서
            // 무효 토큰을 user.fcmToken=null 로 지워주는 처리를 추가하면 좋음.
            log.error("[FCM] 발송 실패: {} - {}", title, e.getMessage());
            return false;
        }
    }
}
