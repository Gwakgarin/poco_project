package com.project.poco.service;

import com.project.poco.entity.Device;
import com.project.poco.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    public Optional<Device> findByUserId(Long userId) {
        return deviceRepository.findByUserId(userId);
    }
}
