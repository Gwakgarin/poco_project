package com.project.poco.service;

import com.project.poco.entity.DangerAlert;
import com.project.poco.repository.DangerAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DangerAlertService {

    private final DangerAlertRepository repository;

    public DangerAlert save(DangerAlert dangerAlert) {
        return repository.save(dangerAlert);
    }

    public List<DangerAlert> findByDeviceId(String deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    public List<DangerAlert> findAll() {
        return repository.findAll();
    }
}
