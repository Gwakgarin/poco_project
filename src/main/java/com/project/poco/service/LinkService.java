package com.project.poco.service;

import com.project.poco.entity.LinkCode;
import com.project.poco.entity.UserLink;
import com.project.poco.repository.LinkCodeRepository;
import com.project.poco.repository.UserLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LinkService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final LinkCodeRepository linkCodeRepository;
    private final UserLinkRepository userLinkRepository;

    // 피보호자가 연동 코드 발급 (10분 유효)
    public LinkCode issueCode(Long userId) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        LinkCode linkCode = new LinkCode();
        linkCode.setCode(sb.toString());
        linkCode.setUserId(userId);
        linkCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return linkCodeRepository.save(linkCode);
    }

    // 보호자가 코드 사용 -> user_links 생성
    public Optional<UserLink> redeem(String code, Long guardianId, String relationLabel) {
        Optional<LinkCode> found = linkCodeRepository.findById(code);
        if (found.isEmpty()) return Optional.empty();

        LinkCode linkCode = found.get();
        if (linkCode.getUsedAt() != null) return Optional.empty();
        if (linkCode.getExpiresAt().isBefore(LocalDateTime.now())) return Optional.empty();

        UserLink link = new UserLink();
        link.setUserId(linkCode.getUserId());
        link.setGuardianId(guardianId);
        link.setRelationLabel(relationLabel);
        UserLink saved = userLinkRepository.save(link);

        linkCode.setUsedAt(LocalDateTime.now());
        linkCodeRepository.save(linkCode);

        return Optional.of(saved);
    }

    public List<UserLink> findAsUser(Long userId) {
        return userLinkRepository.findByUserId(userId);
    }

    public List<UserLink> findAsGuardian(Long guardianId) {
        return userLinkRepository.findByGuardianId(guardianId);
    }

    public void delete(Long linkId) {
        userLinkRepository.deleteById(linkId);
    }
}
