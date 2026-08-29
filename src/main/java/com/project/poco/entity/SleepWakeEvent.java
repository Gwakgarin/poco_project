package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** ERD에 있던 테이블인데 기존 코드엔 없어서 새로 추가했습니다. (outing_events 구조와 동일한 패턴) */
@Entity
@Getter
@Setter
@Table(name = "sleep_wake_events")
public class SleepWakeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    private SleepWakeEventType eventType; // SLEEP | WAKE

    private LocalDateTime timestamp;
}
