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

    private String representativeEvent;
    private String ruleResult;
    private Integer startSec;
    private Integer endSec;
}