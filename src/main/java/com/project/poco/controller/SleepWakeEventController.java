package com.project.poco.controller;

import com.project.poco.entity.SleepWakeEvent;
import com.project.poco.service.SleepWakeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sleep-wake-events")
public class SleepWakeEventController {

    private final SleepWakeEventService sleepWakeEventService;

    @PostMapping
    public SleepWakeEvent create(@RequestBody SleepWakeEvent sleepWakeEvent) {
        return sleepWakeEventService.save(sleepWakeEvent);
    }

    @GetMapping
    public List<SleepWakeEvent> getByDevice(@RequestParam Long deviceId) {
        return sleepWakeEventService.findByDeviceId(deviceId);
    }
}
