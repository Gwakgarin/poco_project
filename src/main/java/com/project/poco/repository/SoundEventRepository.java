package com.project.poco.repository;

import com.project.poco.entity.SoundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoundEventRepository extends JpaRepository<SoundEvent, Long> {
}