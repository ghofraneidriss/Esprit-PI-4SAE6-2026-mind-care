package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Backfill at startup + periodic sweep: published posts idle too long become inactive (hidden from public).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForumMaintenanceService {

    private final PostRepository postRepository;
    private final ForumNotificationPublisher forumNotificationPublisher;
    private final PostFollowService postFollowService;
    private final ForumBackfillService forumBackfillService;

    @Value("${mindcare.forum.inactive-idle-minutes:60}")
    private int inactiveIdleMinutes;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Integer.MAX_VALUE)
    public void onApplicationReady(ApplicationReadyEvent event) {
        forumBackfillService.backfillLastInteractionTimestamps();
    }

    @Scheduled(fixedDelayString = "${mindcare.forum.inactive-scan-ms:60000}")
    @Transactional
    public void sweepInactivePosts() {
        if (inactiveIdleMinutes <= 0) {
            return;
        }
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(inactiveIdleMinutes);
        List<Post> stale = postRepository.findPublishedActiveWithLastInteractionBefore(threshold);
        for (Post p : stale) {
            Long postId = p.getId();
            Long authorId = p.getUserId();
            List<Long> followers = postFollowService.followerUserIds(postId);
            p.setInactive(true);
            p.setInactiveSince(LocalDateTime.now());
            postRepository.save(p);

            String title = p.getTitle();
            String shortTitle = title == null ? "…" : (title.length() > 80 ? title.substring(0, 77) + "…" : title);
            if (authorId != null && authorId > 0) {
                String idleEn = idlePeriodPhrase();
                forumNotificationPublisher.notifyEnriched(
                        authorId,
                        "MindCare forum: \"" + shortTitle + "\" was archived (inactive for " + idleEn + ").",
                        "INFO",
                        postId,
                        "FORUM_ARCHIVE_AUTHOR",
                        null,
                        shortTitle,
                        null,
                        null);
            }
            for (Long uid : followers) {
                if (uid != null && uid > 0 && (authorId == null || !uid.equals(authorId))) {
                    forumNotificationPublisher.notifyEnriched(
                            uid,
                            "A thread you followed was archived (inactivity). · \"" + shortTitle + "\"",
                            "INFO",
                            postId,
                            "FORUM_ARCHIVE_FOLLOWER",
                            null,
                            shortTitle,
                            null,
                            null);
                }
            }
        }
        if (!stale.isEmpty()) {
            log.info("[Forum] marked {} post(s) inactive ({})", stale.size(), idlePeriodPhrase());
        }
    }

    /** Human-readable idle threshold for logs (English). */
    private String idlePeriodPhrase() {
        if (inactiveIdleMinutes >= 1440 && inactiveIdleMinutes % 1440 == 0) {
            int days = inactiveIdleMinutes / 1440;
            return days == 1 ? "1 day" : days + " days";
        }
        return inactiveIdleMinutes + " minutes";
    }

}
