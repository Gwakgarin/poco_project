package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(columnNames = "userId"))
// ERD엔 없던 lastLocationLat/lastLocationLng/lastLocationLabel 필드는 제거했습니다
// (아무 곳에서도 안 쓰이고 있었고, latest_locations 테이블과 역할이 겹쳐서요).
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ERD 확정: 기기 고유 식별자 (CHAR(36), UUID 형식, unique/not null)
    @Column(name = "device_uuid", nullable = false, unique = true, length = 36)
    private String deviceUuid;

    @Column(nullable = false)
    private Long userId; // 소유 피보호자(users.id), 1인 1기기 가정

    @Column(nullable = false)
    private Float micSensitivity;

    @Column(nullable = false)
    private Boolean micOn;

    private Boolean gpsOn;
    private Integer batteryPercent;

    private LocalDateTime lastSeenAt;
}
