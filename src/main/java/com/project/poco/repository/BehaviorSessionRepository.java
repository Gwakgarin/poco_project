package com.project.poco.repository;

import com.project.poco.entity.BehaviorSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BehaviorSessionRepository extends JpaRepository<BehaviorSession, Long> {
    List<BehaviorSession> findByDeviceId(String deviceId);
}