package tn.esprit.users_service.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users", indexes = @Index(name = "idx_users_role", columnList = "role"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phone;

    /**
     * Stored as VARCHAR so all enum values (PATIENT, DOCTOR, CAREGIVER, VOLUNTEER, ADMIN) persist.
     * If an old DB used MySQL ENUM with fewer values, run:
     * ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL;
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // For PATIENT role: references the caregiver assigned to this patient
    private Long caregiverId;

    /** For PATIENT role: references the volunteer assigned to this patient */
    private Long volunteerId;

    /**
     * Only used at self-registration when role is CAREGIVER or VOLUNTEER: existing patient userId to link.
     * Not persisted on the caregiver/volunteer row.
     */
    @Transient
    @JsonProperty(access = Access.WRITE_ONLY)
    private Long assignedPatientId;
}
