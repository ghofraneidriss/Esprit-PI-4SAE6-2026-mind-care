package tn.esprit.localization_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Circular zone. Exactly one per patient may be marked {@code homeReference}:
 * the registered home used as a reference by caregivers and volunteers; movement-service
 * raises an alert on every transition from inside to outside that circle.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SafeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
    private Long patientId;

    @Column(nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private boolean homeReference;
}
