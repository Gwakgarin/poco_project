package com.project.poco.service;

import com.project.poco.entity.Device;
import com.project.poco.entity.NotificationSettings;
import com.project.poco.entity.RoleType;
import com.project.poco.entity.User;
import com.project.poco.repository.DeviceRepository;
import com.project.poco.repository.NotificationSettingsRepository;
import com.project.poco.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;

    // 회원가입: USER(피보호자)면 기본 device / notification_settings 도 같이 생성
    public User signup(User user) {
        User saved = userRepository.save(user);

        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(saved.getId());
        settings.setEmergencyAlert(true);
        settings.setActivityAnomalyAlert(true);
        settings.setLowBatteryAlert(true);
        settings.setDailySummaryAlert(true);
        notificationSettingsRepository.save(settings);

        if (saved.getRole() == RoleType.USER) {
            Device device = new Device();
            device.setDeviceUuid(java.util.UUID.randomUUID().toString()); // ERD상 device_uuid 필수(not null, unique)라 여기서 자동 발급
            device.setUserId(saved.getId());
            device.setMicSensitivity(0.5f);
            device.setMicOn(true);
            device.setGpsOn(true);
            device.setBatteryPercent(100);
            deviceRepository.save(device);
        }

        return saved;
    }

    // 로그인: 지금은 인증 로직/토큰 없이 이메일+비밀번호만 확인 (추후 보완 필요)
    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
