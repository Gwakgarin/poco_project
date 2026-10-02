package com.project.poco.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Firebase Admin SDK 초기화.
 *
 * fcm.credentials-path (= 환경변수 FCM_CREDENTIALS_PATH) 로 서비스 계정 JSON 키 경로를 받는다.
 * 팀원 전체가 이 키를 갖고 있는 게 아니라서, 경로가 비어있거나 파일이 없으면
 * 그냥 "FCM 비활성화" 상태로 넘어가고 서버는 평소대로 뜬다 (앱이 통째로 안 뜨면 안 되니까).
 * 이 상태에서 PushNotificationService가 발송을 시도하면 조용히 스킵된다.
 */
@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("[FCM] fcm.credentials-path(FCM_CREDENTIALS_PATH)가 설정되지 않았습니다. FCM 발송 기능이 비활성화됩니다.");
            return;
        }

        Path keyPath = Path.of(credentialsPath);
        if (!Files.exists(keyPath)) {
            log.warn("[FCM] 서비스 계정 키 파일을 찾을 수 없습니다: {}. FCM 발송 기능이 비활성화됩니다.", credentialsPath);
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
            log.info("[FCM] Firebase Admin SDK 초기화 완료.");
        } catch (IOException e) {
            log.error("[FCM] Firebase 초기화 실패: {}", e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
