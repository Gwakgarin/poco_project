package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ERD 확정: device_id 는 int(FK), start_time/confirmed_time/end_time 은 datetime.
 * (기존엔 deviceId String, 시간 필드들이 epoch ms Long 이었는데 ERD 기준으로 정리했습니다.
 *  representativeEvent/ruleResult/startSec/endSec 4개는 ERD엔 없지만 다른 곳에서
 *  쓰고 계신다고 해서 일단 그대로 남겨뒀습니다. 안 쓰시면 나중에 지우셔도 돼요.)
 */
@Entity
@Getter
@Setter
@Table(name = "behavior_sessions")
public class BehaviorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예전 필드 (ERD엔 없지만, 다른 곳에서 쓰고 있다고 하셔서 유지)
    private String representativeEvent;
    private String ruleResult;
    private Integer startSec;
    private Integer endSec;

    // 세션 상태머신(세탁/청소/설거지/식사/인지) 로직이 실제로 만들어내는 필드들
    @Column(nullable = false)
    private Long deviceId;         // 어느 기기의 세션인지 (ERD: int FK, not null)
    private String behavior;       // "meal" | "cleaning" | "laundry" | "dishwashing" | "cognitive"
    private LocalDateTime startTime;      // 세션 시작(첫 감지) 시각
    private LocalDateTime confirmedTime;  // 확정된 시각
    private LocalDateTime endTime;        // 종료 시각
    private String endReason;      // "timeout" | "dishwashing_trigger" | "no_activity" (meal 전용, 그 외 null)
}
