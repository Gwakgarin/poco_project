package com.project.poco.repository;

import com.project.poco.entity.OutingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutingEventRepository extends JpaRepository<OutingEvent, Long> {
    List<OutingEvent> findByDeviceId(Long deviceId);
}
