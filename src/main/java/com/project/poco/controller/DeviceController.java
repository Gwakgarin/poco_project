package com.project.poco.controller;

import com.project.poco.entity.Device;
import com.project.poco.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public Device save(@RequestBody Device device) {
        return deviceService.save(device);
    }

    // 특정 피보호자(userId)의 기기 정보 조회 (mic/gps/배터리/위치 등)
    @GetMapping
    public Device getByUserId(@RequestParam Long userId) {
        return deviceService.findByUserId(userId).orElse(null);
    }
}
