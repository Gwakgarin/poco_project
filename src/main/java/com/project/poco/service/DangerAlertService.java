package com.project.poco.service;

import com.project.poco.entity.DangerAlert;
import com.project.poco.repository.DangerAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DangerAlertService {

    private final DangerAlertRepository repository;
    private final PushNotificationService pushNotificationService;

    public DangerAlert save(DangerAlert dangerAlert) {
        DangerAlert saved = repository.save(dangerAlert);

        // 저장은 항상 하고, 푸시는 "덤"이라 실패해도 저장 자체는 영향 없게 분리.
        String title = "위험 상황 감지";
        String body = buildBody(saved);
        Map<String, String> data = Map.of(
                "type", "DANGER_ALERT",
                "deviceId", String.valueOf(saved.getDeviceId()),
                "alertId", String.valueOf(saved.getId())
        );
        pushNotificationService.notifyGuardians(
                saved.getDeviceId(), PushNotificationService.Kind.EMERGENCY, title, body, data);

        return saved;
    }

    private String buildBody(DangerAlert alert) {
        if (alert.getSoundLabel() != null && !alert.getSoundLabel().isBlank()) {
            return alert.getSoundLabel() + " 소리가 감지되었어요. 앱에서 확인해주세요.";
        }
        return "위험 소리가 감지되었어요. 앱에서 확인해주세요.";
    }

    public List<DangerAlert> findByDeviceId(Long deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    public List<DangerAlert> findAll() {
        return repository.findAll();
    }
}
