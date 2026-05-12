package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "behavior_sessions")
public class BehaviorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rawFile;
    private String behavior;
    private String mainEvent;
    private Integer startSec;
    private Integer endSec;
    private Integer eventCount;
    private String events;
    private Integer durationSec;
    private String ruleResult;
}