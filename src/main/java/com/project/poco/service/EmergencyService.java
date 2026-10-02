package com.project.poco.service;

import com.project.poco.entity.EmergencyDispatch;
import com.project.poco.entity.ResponseStatus;
import com.project.poco.repository.EmergencyDispatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final EmergencyDispatchRepository emergencyDispatchRepository;
    private final PushNotificationService pushNotificationService;

    public EmergencyDispatch dispatch(Long deviceId, Long dispatchedBy) {
        EmergencyDispatch dispatch = new EmergencyDispatch();
        dispatch.setDeviceId(deviceId);
        dispatch.setDispatchedBy(dispatchedBy);
        dispatch.setResponseStatus(ResponseStatus.REQUESTED);
        EmergencyDispatch saved = emergencyDispatchRepository.save(dispatch);

        // 저장은 항상 하고, 푸시는 "덤"이라 실패해도 저장 자체는 영향 없게 분리.
        String title = "SOS 긴급 요청";
        String body = "SOS 요청이 발생했어요. 즉시 확인해주세요.";
        Map<String, String> data = Map.of(
                "type", "EMERGENCY",
                "deviceId", String.valueOf(saved.getDeviceId()),
                "dispatchId", String.valueOf(saved.getId())
        );
        pushNotificationService.notifyGuardians(
                saved.getDeviceId(), PushNotificationService.Kind.EMERGENCY, title, body, data);

        return saved;
    }

    public List<EmergencyDispatch> findByDeviceId(Long deviceId) {
        return emergencyDispatchRepository.findByDeviceId(deviceId);
    }
}
