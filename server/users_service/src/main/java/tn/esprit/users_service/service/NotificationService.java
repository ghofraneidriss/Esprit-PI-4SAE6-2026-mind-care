package tn.esprit.users_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.users_service.repository.NotificationRepository;

/**
 * Wraps bulk notification operations so {@code @Modifying} JPQL runs in a clear transactional boundary.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void deleteAllForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        notificationRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null || id <= 0) {
            return;
        }
        notificationRepository.deleteById(id);
    }
}
