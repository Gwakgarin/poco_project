package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // 지금은 인증 로직이 없어서 평문 저장. 나중에 로그인 붙일 때 암호화 필요!

    @Column(nullable = false)
    private String phoneNumber; // ERD 확정: not null

    // ERD 확정: role 은 int. 0=USER(피보호자), 1=GUARDIAN(보호자). RoleType 상수 참고.
    @Column(nullable = false)
    private Integer role;

    private LocalDateTime joinedAt;

    // FCM 푸시 토큰. 로그인/앱 실행 시 안드로이드가 발급받아서 등록.
    // 토큰이 없을 수도 있어서 nullable. 앱이 재설치되거나 토큰이 갱신되면 덮어씀.
    @Column(length = 500)
    private String fcmToken;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
