package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 환자 기기의 "가장 최근 위치" 1건만 저장 (deviceId 당 1행, 새 위치가 오면 덮어씀).
 * Android ServerApi.kt 의 LatestLocationRequest / LatestLocationResponse 와 필드명을 맞춤.
 */
@Entity
@Getter
@Setter
@Table(name = "latest_locations", uniqueConstraints = @UniqueConstraint(columnNames = "deviceId"))
public class LatestLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private Double latitude;
    private Double longitude;
    private Float accuracyMeters;
    private String homeState;
    private Long measuredAtEpochMs;
}
