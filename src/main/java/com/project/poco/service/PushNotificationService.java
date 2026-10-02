package com.project.poco.service;

import com.project.poco.entity.Device;
import com.project.poco.entity.NotificationSettings;
import com.project.poco.entity.User;
import com.project.poco.entity.UserLink;
import com.project.poco.repository.DeviceRepository;
import com.project.poco.repository.NotificationSettingsRepository;
import com.project.poco.repository.UserLinkRepository;
import com.project.poco.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * "이 기기에서 이런 알림이 발생했는데, 누구한테 보내야 하나"를 판단해서
 * 실제 발송(FcmService)까지 연결하는 역할.
 *
 * 흐름: device_id -> 소유 피보호자(users.id) -> user_links로 연동된 보호자 목록
 *      -> 각 보호자의 notification_settings에서 해당 종류 알림이 켜져있는지 확인
 *      -> 켜져있고 fcmToken이 있으면 발송
 *
 * 보호자 본인이 알림을 받는 구조라서, 피보호자에게는 보내지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final DeviceRepository deviceRepository;
    private final UserLinkRepository userLinkRepository;
    private final UserRepository userRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final FcmService fcmService;

    public enum Kind {
        EMERGENCY(NotificationSettings::getEmergencyAlert),
        ACTIVITY_ANOMALY(NotificationSettings::getActivityAnomalyAlert),
        LOW_BATTERY(NotificationSettings::getLowBatteryAlert),
        DAILY_SUMMARY(NotificationSettings::getDailySummaryAlert);

        private final Function<NotificationSettings, Boolean> flagGetter;

        Kind(Function<NotificationSettings, Boolean> flagGetter) {
            this.flagGetter = flagGetter;
        }

        boolean isEnabled(NotificationSettings settings) {
            Boolean enabled = flagGetter.apply(settings);
            return enabled != null && enabled;
        }
    }

    /**
     * @param deviceId 알림이 발생한 기기
     * @param kind     어떤 종류의 알림인지 (notification_settings의 어느 플래그를 볼지 결정)
     * @param title    알림 제목
     * @param body     알림 본문
     * @param data     앱에 같이 보낼 추가 데이터 (nullable)
     *
     * 알림 발송은 "덤" 기능이라, 여기서 어떤 예외가 나도 호출한 쪽(알림/이벤트 저장 로직)은
     * 절대 실패하면 안 된다. 그래서 메서드 전체를 try-catch로 감싸고, 보호자 한 명한테
     * 보내다가 실패해도 다른 보호자한테는 계속 보낸다.
     */
    public void notifyGuardians(Long deviceId, Kind kind, String title, String body, Map<String, String> data) {
        try {
            doNotify(deviceId, kind, title, body, data);
        } catch (Exception e) {
            log.error("[FCM] 알림 발송 처리 중 예상치 못한 오류 (device {}): {}", deviceId, e.getMessage(), e);
        }
    }

    private void doNotify(Long deviceId, Kind kind, String title, String body, Map<String, String> data) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            log.warn("[FCM] device {}를 찾을 수 없어 알림을 보내지 않습니다.", deviceId);
            return;
        }

        List<UserLink> links = userLinkRepository.findByUserId(device.getUserId());
        if (links.isEmpty()) {
            log.info("[FCM] device {} (user {})에 연동된 보호자가 없습니다.", deviceId, device.getUserId());
            return;
        }

        for (UserLink link : links) {
            try {
                sendToGuardianIfEnabled(link.getGuardianId(), kind, title, body, data);
            } catch (Exception e) {
                // 보호자 한 명한테 보내다 실패해도 나머지 보호자한테는 계속 보냄
                log.error("[FCM] 보호자 {} 발송 중 오류: {}", link.getGuardianId(), e.getMessage(), e);
            }
        }
    }

    private void sendToGuardianIfEnabled(Long guardianId, Kind kind, String title, String body, Map<String, String> data) {
        NotificationSettings settings = notificationSettingsRepository.findById(guardianId).orElse(null);
        if (settings == null || !kind.isEnabled(settings)) {
            return; // 설정이 없거나 꺼져있으면 스킵
        }

        User guardian = userRepository.findById(guardianId).orElse(null);
        if (guardian == null || guardian.getFcmToken() == null || guardian.getFcmToken().isBlank()) {
            return; // 토큰이 아직 등록 안 된 보호자
        }

        fcmService.send(guardian.getFcmToken(), title, body, data);
    }
}
