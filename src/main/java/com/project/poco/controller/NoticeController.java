package com.project.poco.controller;

import com.project.poco.entity.Notice;
import com.project.poco.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public List<Notice> getByDevice(@RequestParam Long deviceId) {
        return noticeService.findByDeviceId(deviceId);
    }
}
