package com.project.poco.controller;

import com.project.poco.entity.OutingEvent;
import com.project.poco.service.OutingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/outing-events")
public class OutingEventController {

    private final OutingEventService outingEventService;

    @PostMapping
    public OutingEvent create(@RequestBody OutingEvent outingEvent) {
        return outingEventService.save(outingEvent);
    }

    @GetMapping
    public List<OutingEvent> getByDevice(@RequestParam Long deviceId) {
        return outingEventService.findByDeviceId(deviceId);
    }
}
