package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "behavior_sessions")
public class BehaviorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예전 필드 (안 쓰는 곳 없어서 그대로 둠, 나중에 정리 가능)
    private String representativeEvent;
    private String ruleResult;
    private Integer startSec;
    private Integer endSec;

    // 세션 상태머신(세탁/청소/설거지/식사/인지) 로직이 실제로 만들어내는 필드들
    private String deviceId;       // 어느 기기의 세션인지
    private String behavior;       // "meal" | "cleaning" | "laundry" | "dishwashing" | "cognitive"
    private Long startTime;        // epoch ms, 세션 시작(첫 감지) 시각
    private Long confirmedTime;    // epoch ms, 확정된 시각
    private Long endTime;          // epoch ms, 종료 시각
    private String endReason;      // "timeout" | "dishwashing_trigger" | "no_activity" (meal 전용, 그 외 null)
}