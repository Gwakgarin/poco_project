package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 위험 정책이 생성한 위험/위험후보 알림 로그.
 * Android ServerApi.kt 의 DangerAlertRequest 와 필드명을 맞춤.
 */
@Entity
@Getter
@Setter
@Table(name = "danger_alerts")
public class DangerAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private String soundLabel;
    private String level;
    private String reason;
    private String homeState;
    private Double latitude;   // nullable
    private Double longitude;  // nullable
    private Long detectedAtEpochMs;
}
