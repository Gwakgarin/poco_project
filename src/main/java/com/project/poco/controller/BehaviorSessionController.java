package com.project.poco.controller;

import com.project.poco.entity.BehaviorSession;
import com.project.poco.service.BehaviorSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/behavior-sessions")
public class BehaviorSessionController {

    private final BehaviorSessionService behaviorSessionService;

    @PostMapping
    public BehaviorSession create(@RequestBody BehaviorSession behaviorSession) {
        return behaviorSessionService.save(behaviorSession);
    }

    @GetMapping
    public List<BehaviorSession> getAll(@RequestParam(required = false) String deviceId) {
        return (deviceId == null) ? behaviorSessionService.findAll() : behaviorSessionService.findByDeviceId(deviceId);
    }
}