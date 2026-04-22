package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.repository.PostRepository;

/**
 * One-shot DB fixes run at startup (transactional, separate bean so proxies apply).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForumBackfillService {

    private final PostRepository postRepository;

    @Transactional
    public int backfillLastInteractionTimestamps() {
        int n = postRepository.backfillLastInteractionFromCreatedAt();
        if (n > 0) {
            log.info("[Forum] backfilled lastInteractionAt for {} rows", n);
        }
        return n;
    }
}
