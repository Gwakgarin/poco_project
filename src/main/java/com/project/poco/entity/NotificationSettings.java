package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "notification_settings", uniqueConstraints = @UniqueConstraint(columnNames = "userId"))
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Boolean emergencyAlert;
    private Boolean activityAnomalyAlert;
    private Boolean lowBatteryAlert;
    private Boolean dailySummaryAlert;
}
