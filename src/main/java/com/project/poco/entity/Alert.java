package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;

    @Enumerated(EnumType.STRING)
    private AlertType type; // MEAL_IRREGULAR | OUTING_DECREASE | COGNITIVE_DECREASE

    private LocalDateTime time;

    @Lob
    private String evidence;
}
