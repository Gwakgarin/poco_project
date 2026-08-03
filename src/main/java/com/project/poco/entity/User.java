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

    private String name;

    @Column(unique = true)
    private String email;

    private String password; // 지금은 인증 로직이 없어서 평문 저장. 나중에 로그인 붙일 때 암호화 필요!

    private String phoneNumber; // nullable, 긴급상황 화면용

    @Enumerated(EnumType.STRING)
    private Role role; // USER | GUARDIAN

    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
