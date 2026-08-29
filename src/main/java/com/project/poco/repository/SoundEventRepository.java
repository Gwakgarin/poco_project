package com.project.poco.repository;

import com.project.poco.entity.SoundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SoundEventRepository extends JpaRepository<SoundEvent, Long> {
    List<SoundEvent> findByDeviceId(Long deviceId);
}
