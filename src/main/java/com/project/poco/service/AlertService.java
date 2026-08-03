package com.project.poco.service;

import com.project.poco.entity.Alert;
import com.project.poco.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public Alert save(Alert alert) {
        return alertRepository.save(alert);
    }

    public List<Alert> findByDeviceId(Long deviceId) {
        return alertRepository.findByDeviceId(deviceId);
    }
}
