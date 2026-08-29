package com.project.poco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_links")
public class UserLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;      // 피보호자

    @Column(nullable = false)
    private Long guardianId;  // 보호자

    // ERD 확정: enum. FAMILY(가족) | CAREGIVER(요양보호사 등) | FRIEND(지인) | OTHER(기타)
    @Enumerated(EnumType.STRING)
    private RelationLabel relationLabel;

    @Column(nullable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    public void prePersist() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
