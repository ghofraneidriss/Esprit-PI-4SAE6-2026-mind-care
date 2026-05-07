package tn.esprit.movement_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SafeZoneDto {
    private Long id;
    private String name;
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
    private Long patientId;
    /** Registered home — caregivers/volunteers reference; alerts on every exit from this circle. */
    private boolean homeReference;
}
