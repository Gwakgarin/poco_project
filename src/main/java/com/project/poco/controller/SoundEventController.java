package com.project.poco.controller;

import com.project.poco.entity.SoundEvent;
import com.project.poco.service.SoundEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sound-events")
public class SoundEventController {

    private final SoundEventService soundEventService;

    @PostMapping
    public SoundEvent create(@RequestBody SoundEvent soundEvent) {
        return soundEventService.save(soundEvent);
    }

    @GetMapping
    public List<SoundEvent> getAll() {
        return soundEventService.findAll();
    }
}