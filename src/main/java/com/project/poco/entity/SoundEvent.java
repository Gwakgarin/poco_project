package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ERD 확정: device_id(FK, not null), created_at(datetime, not null) 이 원래 있어야 하는데
 * 기존 엔티티엔 빠져 있어서 추가했습니다.
 */
@Entity
@Getter
@Setter
@Table(name = "sound_events")
public class SoundEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String rawFile;
    private String splitFile;
    private String predLabel;
    private Double predScore;
    private Integer segIndex;
    private Integer startSec;
    private Integer endSec;
    private String smoothedLabel;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
