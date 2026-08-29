package com.project.poco.repository;

import com.project.poco.entity.DangerAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DangerAlertRepository extends JpaRepository<DangerAlert, Long> {
    List<DangerAlert> findByDeviceId(Long deviceId);
}
