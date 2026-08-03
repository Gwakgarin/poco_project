package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "emergency_dispatches")
public class EmergencyDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;
    private Long dispatchedBy; // 요청한 guardian(users.id), nullable

    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    private ResponseStatus responseStatus; // REQUESTED | DISPATCHED | ARRIVED | CANCELLED

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}
