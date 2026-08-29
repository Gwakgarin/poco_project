package com.project.poco.service;

import com.project.poco.entity.SleepWakeEvent;
import com.project.poco.repository.SleepWakeEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SleepWakeEventService {

    private final SleepWakeEventRepository sleepWakeEventRepository;

    public SleepWakeEvent save(SleepWakeEvent sleepWakeEvent) {
        return sleepWakeEventRepository.save(sleepWakeEvent);
    }

    public List<SleepWakeEvent> findByDeviceId(Long deviceId) {
        return sleepWakeEventRepository.findByDeviceId(deviceId);
    }
}
