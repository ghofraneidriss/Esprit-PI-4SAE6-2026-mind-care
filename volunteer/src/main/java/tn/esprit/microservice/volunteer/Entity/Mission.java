package tn.esprit.microservice.volunteer.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String location;
    private String category;
    private String assignee;
    private String duration;
    @Enumerated(EnumType.STRING)
    private Priority priority; // High | Medium | Low

    private Date startDate;
    private Date endDate;

    private Integer requiredVolunteers;

    @Enumerated(EnumType.STRING)
    private MissionStatus status;

    @Enumerated(EnumType.STRING)
    private MissionType type;
}
