package tn.esprit.users_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.users_service.entity.Notification;
import tn.esprit.users_service.repository.NotificationRepository;
import tn.esprit.users_service.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/notifications")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4210"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationRepository.countByUserIdAndReadFalse(userId)));
    }

    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        notification.setRead(false);
        return new ResponseEntity<>(notificationRepository.save(notification), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        return notificationRepository.findById(id).map(n -> {
            n.setRead(true);
            return ResponseEntity.ok(notificationRepository.save(n));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllRead(@PathVariable Long userId) {
        notificationRepository.markAllReadByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<Void> deleteAllForUserDelete(@PathVariable Long userId) {
        notificationService.deleteAllForUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Same as {@link #deleteAllForUserDelete} — POST is more reliable through some proxies / dev setups than DELETE.
     */
    @PostMapping("/user/{userId}/delete-all")
    public ResponseEntity<Void> deleteAllForUserPost(@PathVariable Long userId) {
        notificationService.deleteAllForUser(userId);
        return ResponseEntity.noContent().build();
    }
}
