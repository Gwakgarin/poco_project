package com.project.poco.service;

import com.project.poco.entity.BehaviorSession;
import com.project.poco.repository.BehaviorSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BehaviorSessionService {

    private final BehaviorSessionRepository behaviorSessionRepository;

    public BehaviorSession save(BehaviorSession behaviorSession) {
        return behaviorSessionRepository.save(behaviorSession);
    }

    public List<BehaviorSession> findAll() {
        return behaviorSessionRepository.findAll();
    }

    public List<BehaviorSession> findByDeviceId(String deviceId) {
        return behaviorSessionRepository.findByDeviceId(deviceId);
    }
}