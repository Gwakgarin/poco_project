package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 환자 기기의 "가장 최근 위치" 1건만 저장 (deviceId 당 1행, 새 위치가 오면 덮어씀).
 * ERD 확정: device_id 는 int(FK), measured_at 은 datetime, latitude/longitude/home_state 는 not null.
 * (기존엔 deviceId String, measuredAt epoch ms 였는데 ERD 기준으로 정리했습니다.
 *  안드로이드 쪽 LatestLocationRequest/Response 도 같이 맞춰주세요.)
 */
@Entity
@Getter
@Setter
@Table(name = "latest_locations", uniqueConstraints = @UniqueConstraint(columnNames = "deviceId"))
public class LatestLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Float accuracyMeters;

    @Column(nullable = false)
    private String homeState;

    @Column(nullable = false)
    private LocalDateTime measuredAt;

    @PrePersist
    public void prePersist() {
        if (measuredAt == null) {
            measuredAt = LocalDateTime.now();
        }
    }
}
