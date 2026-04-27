package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums_service.entity.ForumUserBan;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ForumUserBanRepository extends JpaRepository<ForumUserBan, Long> {

    Optional<ForumUserBan> findFirstByUserIdAndBannedUntilAfterOrderByBannedUntilDesc(Long userId, LocalDateTime now);

    void deleteByUserId(Long userId);
}
