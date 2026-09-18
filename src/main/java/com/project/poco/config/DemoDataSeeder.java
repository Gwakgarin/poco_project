package com.project.poco.config;

import com.project.poco.entity.BehaviorSession;
import com.project.poco.entity.DangerAlert;
import com.project.poco.entity.Device;
import com.project.poco.entity.LatestLocation;
import com.project.poco.entity.OutingEvent;
import com.project.poco.entity.RelationLabel;
import com.project.poco.entity.RoleType;
import com.project.poco.entity.SleepWakeEvent;
import com.project.poco.entity.SleepWakeEventType;
import com.project.poco.entity.TransitionType;
import com.project.poco.entity.User;
import com.project.poco.entity.UserLink;
import com.project.poco.repository.BehaviorSessionRepository;
import com.project.poco.repository.DangerAlertRepository;
import com.project.poco.repository.DeviceRepository;
import com.project.poco.repository.LatestLocationRepository;
import com.project.poco.repository.OutingEventRepository;
import com.project.poco.repository.SleepWakeEventRepository;
import com.project.poco.repository.UserLinkRepository;
import com.project.poco.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 4차 발표 시연용 데이터 시더.
 *
 * "demo" 프로필로 실행했을 때만 동작한다 (운영/기본 실행에서는 절대 삽입되지 않음).
 * 예: ./gradlew bootRun --args='--spring.profiles.active=demo'
 *
 * demo_user@poco.app 계정 존재 여부로 이미 시딩됐는지 판단해서, 여러 번 실행해도
 * 중복 생성되지 않는다 (이미 있으면 아무것도 하지 않고 로그만 남김).
 *
 * run() 전체를 @Transactional로 묶어서, 시딩 도중 어디서든 실패하면 User 저장분까지
 * 전부 롤백된다 — 이게 없으면 User만 커밋된 채로 나머지가 실패했을 때 다음 실행이
 * "이미 있음"으로 오판하고 건너뛰어서 영구히 반쪽 데이터가 남는 위험이 있었다.
 *
 * "가사 활동"은 cleaning · laundry · dishwashing 세 가지 behavior 값을 기준으로 정의한다.
 * (meal · cognitive는 가사 활동 집계에 포함하지 않음 — 장기 추세 화면의 "가사 활동" 지표와 동일 기준)
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_USER_EMAIL = "demo_user@poco.app";
    private static final String DEMO_GUARDIAN_EMAIL = "demo_guardian@poco.app";
    private static final String DEMO_PASSWORD = "demo1234!";
    private static final int HISTORY_DAYS = 180; // 최근 6개월
    private static final long SEED = 20260917L; // 매번 같은 모양의 데이터가 나오도록 고정 시드

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserLinkRepository userLinkRepository;
    private final BehaviorSessionRepository behaviorSessionRepository;
    private final OutingEventRepository outingEventRepository;
    private final SleepWakeEventRepository sleepWakeEventRepository;
    private final DangerAlertRepository dangerAlertRepository;
    private final LatestLocationRepository latestLocationRepository;

    public DemoDataSeeder(
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            UserLinkRepository userLinkRepository,
            BehaviorSessionRepository behaviorSessionRepository,
            OutingEventRepository outingEventRepository,
            SleepWakeEventRepository sleepWakeEventRepository,
            DangerAlertRepository dangerAlertRepository,
            LatestLocationRepository latestLocationRepository
    ) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.userLinkRepository = userLinkRepository;
        this.behaviorSessionRepository = behaviorSessionRepository;
        this.outingEventRepository = outingEventRepository;
        this.sleepWakeEventRepository = sleepWakeEventRepository;
        this.dangerAlertRepository = dangerAlertRepository;
        this.latestLocationRepository = latestLocationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(DEMO_USER_EMAIL).isPresent()) {
            System.out.println("[DemoDataSeeder] 이미 시딩된 데모 데이터가 있어 건너뜁니다 (" + DEMO_USER_EMAIL + ")");
            return;
        }

        System.out.println("[DemoDataSeeder] 데모 데이터 생성을 시작합니다...");
        Random random = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();

        User demoUser = new User();
        demoUser.setName("데모 사용자");
        demoUser.setEmail(DEMO_USER_EMAIL);
        demoUser.setPassword(DEMO_PASSWORD);
        demoUser.setPhoneNumber("01011112222");
        demoUser.setRole(RoleType.USER);
        demoUser = userRepository.save(demoUser);

        User demoGuardian = new User();
        demoGuardian.setName("데모 보호자");
        demoGuardian.setEmail(DEMO_GUARDIAN_EMAIL);
        demoGuardian.setPassword(DEMO_PASSWORD);
        demoGuardian.setPhoneNumber("01033334444");
        demoGuardian.setRole(RoleType.GUARDIAN);
        demoGuardian = userRepository.save(demoGuardian);

        Device device = new Device();
        device.setDeviceUuid(UUID.randomUUID().toString());
        device.setUserId(demoUser.getId());
        device.setMicSensitivity(0.5f);
        device.setMicOn(true);
        device.setGpsOn(true);
        device.setBatteryPercent(82);
        device.setLastSeenAt(now.minusMinutes(4));
        device = deviceRepository.save(device);

        UserLink link = new UserLink();
        link.setUserId(demoUser.getId());
        link.setGuardianId(demoGuardian.getId());
        link.setRelationLabel(RelationLabel.FAMILY);
        link.setLinkedAt(now.minusDays(HISTORY_DAYS));
        userLinkRepository.save(link);

        seedBehaviorSessionsAndOutings(device.getId(), now, random);
        seedSleepWakeEvents(device.getId(), now, random);
        seedDangerAlerts(device.getId(), now, random);
        seedLatestLocation(device.getId(), now);

        System.out.println("[DemoDataSeeder] 데모 데이터 생성 완료 — 사용자: " + DEMO_USER_EMAIL
                + " / 보호자: " + DEMO_GUARDIAN_EMAIL + " / 비밀번호(둘 다): " + DEMO_PASSWORD);
    }

    /**
     * 최근 6개월(HISTORY_DAYS) 동안의 가사 활동(cleaning/laundry/dishwashing) 세션과
     * 외출 이벤트를 하루 단위로 생성한다. 과거로 갈수록 빈도가 조금 더 높게 설계해서
     * (오늘에 가까워질수록 활동이 줄어드는) 장기 추세 그래프가 의미 있게 보이도록 한다.
     * 실제 관찰치가 아니라 시연용으로 만든 값이며, 발표에서 "시연 데이터"임을 명시해야 한다.
     */
    private void seedBehaviorSessionsAndOutings(Long deviceId, LocalDateTime now, Random random) {
        String[] houseworkBehaviors = { "cleaning", "laundry", "dishwashing" };
        List<BehaviorSession> sessions = new ArrayList<>();
        List<OutingEvent> outings = new ArrayList<>();

        for (int dayOffset = HISTORY_DAYS; dayOffset >= 0; dayOffset--) { // 0 포함: "오늘"도 채워야 발표 때 24시간 리듬 차트가 비어있지 않음
            LocalDateTime day = now.minusDays(dayOffset);
            // 과거(180일 전)일수록 활동 빈도가 높고, 최근일수록 낮아지도록 선형 보간
            double recencyFactor = dayOffset / (double) HISTORY_DAYS; // 1.0(과거) ~ 0.0(오늘 근접)

            // 가사 활동: 하루 0~3건, recencyFactor가 높을수록(과거일수록) 더 자주 발생
            int houseworkCount = poissonish(random, 0.6 + recencyFactor * 1.6);
            for (int i = 0; i < houseworkCount; i++) {
                String behavior = houseworkBehaviors[random.nextInt(houseworkBehaviors.length)];
                LocalDateTime start = day.withHour(8 + random.nextInt(12)).withMinute(random.nextInt(60));
                LocalDateTime confirmed = start.plusMinutes(2 + random.nextInt(5));
                LocalDateTime end = confirmed.plusMinutes(5 + random.nextInt(20));
                sessions.add(behaviorSession(deviceId, behavior, start, confirmed, end, "timeout"));
            }

            // 식사 · 인지 세션도 소량 채워서 behavior_sessions 테이블 전체 구성은 실제와 비슷하게
            if (random.nextDouble() < 0.5) {
                LocalDateTime start = day.withHour(12).withMinute(random.nextInt(60));
                sessions.add(behaviorSession(deviceId, "meal", start, start.plusMinutes(3), start.plusMinutes(25), "timeout"));
            }
            if (random.nextDouble() < 0.3 + recencyFactor * 0.2) {
                LocalDateTime start = day.withHour(19).withMinute(random.nextInt(60));
                sessions.add(behaviorSession(deviceId, "cognitive", start, start.plusMinutes(1), start.plusMinutes(40), null));
            }

            // 외출: recencyFactor가 높을수록(과거일수록) 외출 확률이 높음
            if (random.nextDouble() < 0.25 + recencyFactor * 0.5) {
                LocalDateTime outAt = day.withHour(9 + random.nextInt(8)).withMinute(random.nextInt(60));
                LocalDateTime backAt = outAt.plusHours(1 + random.nextInt(4));
                outings.add(outingEvent(deviceId, TransitionType.HOME_TO_OUTSIDE, outAt));
                outings.add(outingEvent(deviceId, TransitionType.OUTSIDE_TO_HOME, backAt));
            }
        }

        behaviorSessionRepository.saveAll(sessions);
        outingEventRepository.saveAll(outings);
        System.out.println("[DemoDataSeeder] behavior_sessions " + sessions.size() + "건, outing_events " + outings.size() + "건 생성");
    }

    private BehaviorSession behaviorSession(Long deviceId, String behavior, LocalDateTime start, LocalDateTime confirmed, LocalDateTime end, String endReason) {
        BehaviorSession session = new BehaviorSession();
        session.setDeviceId(deviceId);
        session.setBehavior(behavior);
        session.setStartTime(start);
        session.setConfirmedTime(confirmed);
        session.setEndTime(end);
        session.setEndReason(endReason);
        return session;
    }

    private OutingEvent outingEvent(Long deviceId, TransitionType type, LocalDateTime at) {
        OutingEvent event = new OutingEvent();
        event.setDeviceId(deviceId);
        event.setTransitionType(type);
        event.setTimestamp(at);
        return event;
    }

    /** 최근 2주치 취침·기상 이벤트를 매일 생성한다 (23시 취침 / 다음날 7시 기상, ±30분 변동). */
    private void seedSleepWakeEvents(Long deviceId, LocalDateTime now, Random random) {
        List<SleepWakeEvent> events = new ArrayList<>();
        for (int dayOffset = 14; dayOffset >= 1; dayOffset--) {
            LocalDateTime day = now.minusDays(dayOffset);
            LocalDateTime sleepAt = day.withHour(23).withMinute(random.nextInt(30));
            LocalDateTime wakeAt = day.plusDays(1).withHour(7).withMinute(random.nextInt(30));
            events.add(sleepWakeEvent(deviceId, SleepWakeEventType.SLEEP, sleepAt));
            events.add(sleepWakeEvent(deviceId, SleepWakeEventType.WAKE, wakeAt));
        }
        sleepWakeEventRepository.saveAll(events);
        System.out.println("[DemoDataSeeder] sleep_wake_events " + events.size() + "건 생성");
    }

    private SleepWakeEvent sleepWakeEvent(Long deviceId, SleepWakeEventType type, LocalDateTime at) {
        SleepWakeEvent event = new SleepWakeEvent();
        event.setDeviceId(deviceId);
        event.setEventType(type);
        event.setTimestamp(at);
        return event;
    }

    /** 위험 알림 4건 — DANGER(비명) 1건, CANDIDATE(반복 경적) 3건, 최근 열흘 안에 분산. */
    private void seedDangerAlerts(Long deviceId, LocalDateTime now, Random random) {
        List<DangerAlert> alerts = new ArrayList<>();
        alerts.add(dangerAlert(deviceId, "scream", "DANGER", "scream_detected", now.minusDays(2).withHour(15).withMinute(20)));
        alerts.add(dangerAlert(deviceId, "car_horn", "CANDIDATE", "car_horn_repeated", now.minusDays(4).withHour(11).withMinute(5)));
        alerts.add(dangerAlert(deviceId, "car_horn", "CANDIDATE", "car_horn_repeated", now.minusDays(7).withHour(17).withMinute(45)));
        alerts.add(dangerAlert(deviceId, "car_horn", "CANDIDATE", "car_horn_repeated", now.minusDays(9).withHour(9).withMinute(30)));
        dangerAlertRepository.saveAll(alerts);
        System.out.println("[DemoDataSeeder] danger_alerts " + alerts.size() + "건 생성");
    }

    private DangerAlert dangerAlert(Long deviceId, String soundLabel, String level, String reason, LocalDateTime detectedAt) {
        DangerAlert alert = new DangerAlert();
        alert.setDeviceId(deviceId);
        alert.setSoundLabel(soundLabel);
        alert.setLevel(level);
        alert.setReason(reason);
        alert.setDetectedAt(detectedAt);
        return alert;
    }

    /** 최신 위치 1건 — 서울 시청 인근 좌표(예시), HOME 상태로 기록. */
    private void seedLatestLocation(Long deviceId, LocalDateTime now) {
        LatestLocation location = new LatestLocation();
        location.setDeviceId(deviceId);
        location.setLatitude(37.5665);
        location.setLongitude(126.9780);
        location.setAccuracyMeters(12.0f);
        location.setHomeState("HOME");
        location.setMeasuredAt(now.minusMinutes(6));
        latestLocationRepository.save(location);
        System.out.println("[DemoDataSeeder] latest_locations 1건 생성");
    }

    /** 평균 lambda에 대한 근사 포아송 표본(0 이상 정수). 간단한 구현으로 충분해서 정식 포아송 대신 사용. */
    private int poissonish(Random random, double lambda) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > l);
        return k - 1;
    }
}
