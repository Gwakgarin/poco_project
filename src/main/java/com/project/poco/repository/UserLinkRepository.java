package com.project.poco.repository;

import com.project.poco.entity.UserLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLinkRepository extends JpaRepository<UserLink, Long> {
    List<UserLink> findByUserId(Long userId);
    List<UserLink> findByGuardianId(Long guardianId);
}
