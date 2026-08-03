package com.project.poco.controller;

import com.project.poco.entity.EmergencyDispatch;
import com.project.poco.service.EmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;

    // body 예시: {"deviceId":1,"dispatchedBy":2}
    @PostMapping("/dispatch")
    public EmergencyDispatch dispatch(@RequestBody Map<String, Object> body) {
        Long deviceId = Long.valueOf(body.get("deviceId").toString());
        Long dispatchedBy = body.get("dispatchedBy") != null ? Long.valueOf(body.get("dispatchedBy").toString()) : null;
        return emergencyService.dispatch(deviceId, dispatchedBy);
    }

    @GetMapping("/history")
    public List<EmergencyDispatch> history(@RequestParam Long deviceId) {
        return emergencyService.findByDeviceId(deviceId);
    }
}
