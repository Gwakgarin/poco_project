package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(columnNames = "userId"))
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 소유 피보호자(users.id), 1인 1기기 가정

    private Float micSensitivity;
    private Boolean micOn;
    private Boolean gpsOn;
    private Integer batteryPercent;

    private Double lastLocationLat;
    private Double lastLocationLng;
    private String lastLocationLabel;

    private LocalDateTime lastSeenAt;
}
