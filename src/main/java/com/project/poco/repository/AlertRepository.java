package com.project.poco.repository;

import com.project.poco.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByDeviceId(Long deviceId);
}
