package com.project.poco.controller;

import com.project.poco.entity.LinkCode;
import com.project.poco.entity.UserLink;
import com.project.poco.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/link")
public class LinkController {

    private final LinkService linkService;

    // 피보호자가 연동코드 발급: POST /api/link/code?userId=1
    @PostMapping("/code")
    public LinkCode issueCode(@RequestParam Long userId) {
        return linkService.issueCode(userId);
    }

    // 보호자가 코드 사용: body 예시 {"code":"ABCD1234","guardianId":2,"relationLabel":"딸"}
    @PostMapping("/redeem")
    public Map<String, Object> redeem(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        Long guardianId = Long.valueOf(body.get("guardianId").toString());
        String relationLabel = (String) body.get("relationLabel");

        return linkService.redeem(code, guardianId, relationLabel)
                .<Map<String, Object>>map(link -> Map.of("success", true, "link", link))
                .orElseGet(() -> Map.of("success", false, "message", "유효하지 않거나 만료된 코드입니다."));
    }

    // 내 연동 목록: GET /api/link/list?userId=1&as=USER  (as=USER 면 피보호자로서, as=GUARDIAN 이면 보호자로서)
    @GetMapping("/list")
    public List<UserLink> list(@RequestParam Long userId, @RequestParam String as) {
        return "GUARDIAN".equalsIgnoreCase(as)
                ? linkService.findAsGuardian(userId)
                : linkService.findAsUser(userId);
    }

    @DeleteMapping("/{linkId}")
    public void delete(@PathVariable Long linkId) {
        linkService.delete(linkId);
    }
}
