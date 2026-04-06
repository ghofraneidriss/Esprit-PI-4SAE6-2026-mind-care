package tn.esprit.traitement_et_consultation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Long doctorId;

    private LocalDateTime appointmentDate;
    private LocalDateTime appointmentEndDate;

    private Boolean isUrgent;

    @Enumerated(EnumType.STRING)
    private AppointmentType type;

    @Enumerated(EnumType.STRING)
    private AppointmentCategory category;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private String googleEventId;

    @Transient
    private Integer priorityScore;

    private String meetLink;
}
