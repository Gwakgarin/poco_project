package com.project.poco.repository;

import com.project.poco.entity.EmergencyDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyDispatchRepository extends JpaRepository<EmergencyDispatch, Long> {
    List<EmergencyDispatch> findByDeviceId(Long deviceId);
}
