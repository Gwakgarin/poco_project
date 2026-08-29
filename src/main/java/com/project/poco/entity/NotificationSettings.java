package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ERD 확정: user_id 자체가 PK (1:1). 기존엔 별도 auto-increment id + userId(unique)
 * 조합이었는데 ERD대로 userId를 그대로 PK로 바꿨습니다.
 */
@Entity
@Getter
@Setter
@Table(name = "notification_settings")
public class NotificationSettings {

    @Id
    private Long userId;

    private Boolean emergencyAlert;
    private Boolean activityAnomalyAlert;
    private Boolean lowBatteryAlert;
    private Boolean dailySummaryAlert;
}
