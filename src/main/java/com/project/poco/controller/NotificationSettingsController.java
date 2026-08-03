package com.project.poco.controller;

import com.project.poco.entity.NotificationSettings;
import com.project.poco.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService service;

    @GetMapping
    public NotificationSettings getByUserId(@RequestParam Long userId) {
        return service.findByUserId(userId).orElse(null);
    }

    @PutMapping
    public NotificationSettings update(@RequestBody NotificationSettings settings) {
        return service.save(settings);
    }
}
