package com.project.poco.service;

import com.project.poco.entity.BehaviorSession;
import com.project.poco.entity.Device;
import com.project.poco.entity.Notice;
import com.project.poco.repository.BehaviorSessionRepository;
import com.project.poco.repository.DeviceRepository;
import com.project.poco.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 원래 API 설계표대로 notices는 조회 전용(GET)이라 클라이언트가 POST로 만들지 않는다.
 * 대신 조회 시점에 "오늘자 일반 알림이 아직 없으면 간단한 규칙으로 만들어서 저장한다" —
 * 배터리 부족 / 일일 요약 두 가지만. 새 테이블이나 별도 배치 잡 없이, 기존 devices·
 * behavior_sessions 데이터만 보고 판단하는 최소 규칙이다.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final int LOW_BATTERY_THRESHOLD = 20;
    private static final String LOW_BATTERY_TITLE = "배터리 부족 알림";
    private static final String DAILY_SUMMARY_TITLE = "일일 요약 리포트 도착";

    private final NoticeRepository noticeRepository;
    private final DeviceRepository deviceRepository;
    private final BehaviorSessionRepository behaviorSessionRepository;

    public List<Notice> findByDeviceId(Long deviceId) {
        ensureTodayNotices(deviceId);
        return noticeRepository.findByDeviceId(deviceId);
    }

    private void ensureTodayNotices(Long deviceId) {
        List<Notice> existingToday = noticeRepository.findByDeviceId(deviceId).stream()
                .filter(n -> n.getTime() != null && n.getTime().toLocalDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        boolean hasBatteryNotice = existingToday.stream().anyMatch(n -> LOW_BATTERY_TITLE.equals(n.getTitle()));
        boolean hasSummaryNotice = existingToday.stream().anyMatch(n -> DAILY_SUMMARY_TITLE.equals(n.getTitle()));

        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return;

        if (!hasBatteryNotice && device.getBatteryPercent() != null && device.getBatteryPercent() <= LOW_BATTERY_THRESHOLD) {
            Notice notice = new Notice();
            notice.setDeviceId(deviceId);
            notice.setTime(LocalDateTime.now());
            notice.setTitle(LOW_BATTERY_TITLE);
            notice.setDescription("기기 배터리가 " + device.getBatteryPercent() + "%로 떨어졌어요");
            noticeRepository.save(notice);
        }

        if (!hasSummaryNotice) {
            List<BehaviorSession> todaySessions = behaviorSessionRepository.findByDeviceId(deviceId).stream()
                    .filter(s -> s.getStartTime() != null && s.getStartTime().toLocalDate().equals(LocalDate.now()))
                    .collect(Collectors.toList());
            Map<String, Long> countByBehavior = todaySessions.stream()
                    .filter(s -> s.getBehavior() != null)
                    .collect(Collectors.groupingBy(BehaviorSession::getBehavior, Collectors.counting()));
            String summary = countByBehavior.isEmpty()
                    ? "오늘 아직 기록된 활동이 없어요"
                    : countByBehavior.entrySet().stream()
                        .map(e -> behaviorLabel(e.getKey()) + " " + e.getValue() + "회")
                        .collect(Collectors.joining(", ")) + " 감지됐어요";

            Notice notice = new Notice();
            notice.setDeviceId(deviceId);
            notice.setTime(LocalDateTime.now());
            notice.setTitle(DAILY_SUMMARY_TITLE);
            notice.setDescription(summary);
            noticeRepository.save(notice);
        }
    }

    private String behaviorLabel(String behavior) {
        return switch (behavior) {
            case "meal" -> "식사";
            case "cleaning" -> "청소";
            case "laundry" -> "세탁";
            case "dishwashing" -> "설거지";
            case "cognitive" -> "대화·미디어 활동";
            default -> behavior;
        };
    }
}
