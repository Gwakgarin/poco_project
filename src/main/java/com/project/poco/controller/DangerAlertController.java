package com.project.poco.controller;

import com.project.poco.entity.DangerAlert;
import com.project.poco.service.DangerAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Android ServerApi.kt 의 SoundEventApi.createDangerAlert 와 짝이 맞는 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/danger-alerts")
public class DangerAlertController {

    private final DangerAlertService service;

    @PostMapping
    public void create(@RequestBody DangerAlert request) {
        service.save(request);
    }

    // 앱에는 없지만, Postman으로 잘 저장됐는지 확인용으로 넣어둠
    @GetMapping
    public List<DangerAlert> get(@RequestParam(required = false) Long deviceId) {
        return (deviceId == null) ? service.findAll() : service.findByDeviceId(deviceId);
    }
}
