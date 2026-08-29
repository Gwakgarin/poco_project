package com.project.poco.service;

import com.project.poco.entity.OutingEvent;
import com.project.poco.repository.OutingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutingEventService {

    private final OutingEventRepository outingEventRepository;

    public OutingEvent save(OutingEvent outingEvent) {
        return outingEventRepository.save(outingEvent);
    }

    public List<OutingEvent> findByDeviceId(Long deviceId) {
        return outingEventRepository.findByDeviceId(deviceId);
    }
}
