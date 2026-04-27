package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.dto.ForumBanStatusDto;
import tn.esprit.forums_service.entity.ForumUserBan;
import tn.esprit.forums_service.repository.ForumUserBanRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForumBanService {

    private final ForumUserBanRepository forumUserBanRepository;

    public boolean isBanned(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        return forumUserBanRepository
                .findFirstByUserIdAndBannedUntilAfterOrderByBannedUntilDesc(userId, LocalDateTime.now())
                .isPresent();
    }

    public ForumBanStatusDto status(Long userId) {
        if (userId == null || userId <= 0) {
            return new ForumBanStatusDto(false, null);
        }
        return forumUserBanRepository
                .findFirstByUserIdAndBannedUntilAfterOrderByBannedUntilDesc(userId, LocalDateTime.now())
                .map(b -> new ForumBanStatusDto(true, b.getBannedUntil()))
                .orElse(new ForumBanStatusDto(false, null));
    }

    @Transactional
    public void banUser(long userId, int minutes, Long doctorId, String reason) {
        LocalDateTime until = LocalDateTime.now().plusMinutes(minutes);
        forumUserBanRepository.deleteByUserId(userId);
        forumUserBanRepository.save(
                ForumUserBan.builder()
                        .userId(userId)
                        .bannedUntil(until)
                        .createdByDoctorId(doctorId)
                        .reason(reason != null ? reason : "Forum moderation")
                        .createdAt(LocalDateTime.now())
                        .build());
    }

    /** Removes active forum ban rows for this user (e.g. when moderation decision changes to dismiss). */
    @Transactional
    public void liftBan(long userId) {
        if (userId > 0) {
            forumUserBanRepository.deleteByUserId(userId);
        }
    }
}
