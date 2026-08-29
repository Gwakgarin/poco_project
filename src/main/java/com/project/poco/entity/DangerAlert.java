package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 위험 정책이 생성한 위험/위험후보 알림 로그.
 * ERD 확정: device_id 는 int(FK), detected_at 은 datetime.
 * (기존엔 Android ServerApi.kt 요청 필드에 맞춰 deviceId를 String으로,
 *  detectedAt을 epoch ms로 썼고 homeState/latitude/longitude 도 추가로 있었는데
 *  ERD 기준으로 정리했습니다. 안드로이드 쪽 DangerAlertRequest도 같이 맞춰주세요.)
 */
@Entity
@Getter
@Setter
@Table(name = "danger_alerts")
public class DangerAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    private String soundLabel;
    private String level;
    private String reason;

    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
