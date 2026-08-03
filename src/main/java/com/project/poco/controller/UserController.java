package com.project.poco.controller;

import com.project.poco.entity.User;
import com.project.poco.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 회원가입 (body 예시: {"name":"홍길동","email":"a@a.com","password":"1234","role":"USER"})
    @PostMapping("/signup")
    public User signup(@RequestBody User user) {
        return userService.signup(user);
    }

    // 로그인 (지금은 토큰 없이 성공/실패만 반환. body 예시: {"email":"a@a.com","password":"1234"})
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return userService.login(body.get("email"), body.get("password"))
                .<Map<String, Object>>map(user -> Map.of("success", true, "user", user))
                .orElseGet(() -> Map.of("success", false, "message", "이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{userId}")
    public User getOne(@PathVariable Long userId) {
        return userService.findById(userId).orElse(null);
    }
}
