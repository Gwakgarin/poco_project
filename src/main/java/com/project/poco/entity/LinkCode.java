package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "link_codes")
public class LinkCode {

    @Id
    private String code; // 발급되는 연동 코드 자체를 PK로 사용

    private Long userId; // 발급한 피보호자
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt; // nullable
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
