package com.project.poco.service;

import com.project.poco.entity.Alert;
import com.project.poco.entity.AlertType;
import com.project.poco.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final PushNotificationService pushNotificationService;

    public Alert save(Alert alert) {
        Alert saved = alertRepository.save(alert);

        // 저장은 항상 하고, 푸시는 "덤"이라 실패해도 저장 자체는 영향 없게 분리.
        String title = "이상 징후 감지";
        String body = buildBody(saved);
        Map<String, String> data = Map.of(
                "type", "ALERT",
                "alertType", saved.getType() != null ? saved.getType().name() : "",
                "deviceId", String.valueOf(saved.getDeviceId()),
                "alertId", String.valueOf(saved.getId())
        );
        pushNotificationService.notifyGuardians(
                saved.getDeviceId(), PushNotificationService.Kind.ACTIVITY_ANOMALY, title, body, data);

        return saved;
    }

    private String buildBody(Alert alert) {
        if (alert.getType() == AlertType.MEAL_IRREGULAR) {
            return "식사가 불규칙한 패턴이 감지되었어요. 앱에서 확인해주세요.";
        }
        if (alert.getType() == AlertType.OUTING_DECREASE) {
            return "외출 빈도가 줄었어요. 앱에서 확인해주세요.";
        }
        if (alert.getType() == AlertType.COGNITIVE_DECREASE) {
            return "인지 저하 가능성이 감지되었어요. 앱에서 확인해주세요.";
        }
        return "이상 징후가 감지되었어요. 앱에서 확인해주세요.";
    }

    public List<Alert> findByDeviceId(Long deviceId) {
        return alertRepository.findByDeviceId(deviceId);
    }
}
