package tn.esprit.users_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.users_service.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = ?1")
    void markAllReadByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.userId = ?1")
    void deleteByUserId(Long userId);
}
