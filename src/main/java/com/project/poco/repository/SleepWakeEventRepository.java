package com.project.poco.repository;

import com.project.poco.entity.SleepWakeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SleepWakeEventRepository extends JpaRepository<SleepWakeEvent, Long> {
    List<SleepWakeEvent> findByDeviceId(Long deviceId);
}
