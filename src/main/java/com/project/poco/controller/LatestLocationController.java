package com.project.poco.controller;

import com.project.poco.entity.LatestLocation;
import com.project.poco.service.LatestLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// Android ServerApi.kt 의 SoundEventApi.updateLatestLocation / getLatestLocation 과 짝이 맞는 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/latest-locations")
public class LatestLocationController {

    private final LatestLocationService service;

    @PostMapping
    public void update(@RequestBody LatestLocation request) {
        service.update(request);
    }

    @GetMapping
    public LatestLocation get(@RequestParam Long deviceId) {
        return service.findByDeviceId(deviceId);
    }
}
