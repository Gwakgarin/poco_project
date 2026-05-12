package com.project.poco.service;

import com.project.poco.entity.SoundEvent;
import com.project.poco.repository.SoundEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SoundEventService {

    private final SoundEventRepository soundEventRepository;

    public SoundEvent save(SoundEvent soundEvent) {
        return soundEventRepository.save(soundEvent);
    }

    public List<SoundEvent> findAll() {
        return soundEventRepository.findAll();
    }
}