package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "outing_events")
public class OutingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    private TransitionType transitionType; // HOME_TO_OUTSIDE | OUTSIDE_TO_HOME

    private LocalDateTime timestamp;
}
