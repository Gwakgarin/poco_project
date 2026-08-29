package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ERD 확정: device_id not null (기존엔 null 허용해서 "전체 공지" 기능이 있었는데,
    // ERD대로 not null로 바꾸면서 전체 공지 기능은 제거했습니다)
    @Column(nullable = false)
    private Long deviceId;

    private LocalDateTime time;
    private String title;

    @Lob
    private String description;
}
