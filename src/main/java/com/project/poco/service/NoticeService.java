package com.project.poco.service;

import com.project.poco.entity.Notice;
import com.project.poco.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<Notice> findByDeviceId(Long deviceId) {
        return noticeRepository.findByDeviceId(deviceId);
    }
}
