package com.project.poco.repository;

import com.project.poco.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByDeviceIdOrDeviceIdIsNull(Long deviceId);
}
