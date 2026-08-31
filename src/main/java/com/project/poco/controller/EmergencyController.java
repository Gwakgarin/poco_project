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
    // dispatchedBy는 이제 필수입니다. 본인이 SOS를 직접 누른 경우에도
    // 안드로이드 앱이 로그인된 사용자 본인의 id를 dispatchedBy에 넣어서 보내주세요.
    @PostMapping("/dispatch")
    public Map<String, Object> dispatch(@RequestBody Map<String, Object> body) {
        if (body.get("deviceId") == null || body.get("dispatchedBy") == null) {
            return Map.of(
                    "success", false,
                    "message", "deviceId와 dispatchedBy는 필수입니다. 본인이 요청하는 경우에도 본인의 userId를 dispatchedBy에 넣어주세요."
            );
        }
        Long deviceId = Long.valueOf(body.get("deviceId").toString());
        Long dispatchedBy = Long.valueOf(body.get("dispatchedBy").toString());
        EmergencyDispatch result = emergencyService.dispatch(deviceId, dispatchedBy);
        return Map.of("success", true, "dispatch", result);
    }

    @GetMapping("/history")
    public List<EmergencyDispatch> history(@RequestParam Long deviceId) {
        return emergencyService.findByDeviceId(deviceId);
    }
}