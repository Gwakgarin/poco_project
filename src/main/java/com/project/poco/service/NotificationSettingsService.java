package com.project.poco.service;

import com.project.poco.entity.NotificationSettings;
import com.project.poco.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository repository;

    public Optional<NotificationSettings> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public NotificationSettings save(NotificationSettings settings) {
        return repository.save(settings);
    }
}
