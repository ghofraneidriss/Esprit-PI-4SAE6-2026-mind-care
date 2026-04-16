package tn.esprit.microservice.volunteer.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long volunteerId;
    private Long volunteerUserId; // user with VOLUNTEER role

    @ManyToOne
    private Mission mission;

    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status; // ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED

    private String notes;
    private String feedback;

    private Double rating;
}
