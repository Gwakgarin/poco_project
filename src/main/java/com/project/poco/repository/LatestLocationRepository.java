package com.project.poco.repository;

import com.project.poco.entity.LatestLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LatestLocationRepository extends JpaRepository<LatestLocation, Long> {
    Optional<LatestLocation> findByDeviceId(Long deviceId);
}
