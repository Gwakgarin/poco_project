package com.project.poco.service;

import com.project.poco.entity.LatestLocation;
import com.project.poco.repository.LatestLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LatestLocationService {

    private final LatestLocationRepository repository;

    // deviceId 기준으로 이미 있으면 덮어쓰고(update), 없으면 새로 만듦(upsert)
    public LatestLocation update(LatestLocation incoming) {
        LatestLocation entity = repository.findByDeviceId(incoming.getDeviceId())
                .orElseGet(LatestLocation::new);

        entity.setDeviceId(incoming.getDeviceId());
        entity.setLatitude(incoming.getLatitude());
        entity.setLongitude(incoming.getLongitude());
        entity.setAccuracyMeters(incoming.getAccuracyMeters());
        entity.setHomeState(incoming.getHomeState());
        entity.setMeasuredAt(incoming.getMeasuredAt());

        return repository.save(entity);
    }

    public LatestLocation findByDeviceId(Long deviceId) {
        return repository.findByDeviceId(deviceId).orElse(null);
    }
}
