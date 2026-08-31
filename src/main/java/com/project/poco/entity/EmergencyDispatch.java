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

    @Column(nullable = false)
    private Long deviceId;

    // ERD 확정: not null. 본인이 SOS를 직접 눌러도 그 "본인"의 userId를 넣어서 보내야 함
    // (누가 요청했는지는 항상 기록되어야 해서 null 허용 안 함 - 안드로이드 앱에서 항상 채워서 보내주세요)
    @Column(nullable = false)
    private Long dispatchedBy;

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