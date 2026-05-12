package com.project.poco.repository;

import com.project.poco.entity.BehaviorSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehaviorSessionRepository extends JpaRepository<BehaviorSession, Long> {
}