package com.project.poco.controller;

import com.project.poco.entity.Alert;
import com.project.poco.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public Alert create(@RequestBody Alert alert) {
        return alertService.save(alert);
    }

    @GetMapping
    public List<Alert> getByDevice(@RequestParam Long deviceId) {
        return alertService.findByDeviceId(deviceId);
    }
}
