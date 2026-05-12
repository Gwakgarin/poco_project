package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "sound_events")
public class SoundEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rawFile;
    private String splitFile;
    private String predLabel;
    private Double predScore;
    private Integer segIndex;
    private Integer startSec;
    private Integer endSec;
    private String smoothedLabel;
}