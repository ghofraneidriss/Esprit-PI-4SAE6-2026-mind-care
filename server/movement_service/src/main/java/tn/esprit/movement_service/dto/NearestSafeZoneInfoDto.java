package tn.esprit.movement_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Business view: patient position relative to one safe zone.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearestSafeZoneInfoDto {
    private Long safeZoneId;
    private String name;
    /** Distance from latest ping to zone center (m). */
    private double distanceMetersToCenter;
    private double radiusMeters;
    /** True if the latest ping lies inside this zone's circle. */
    private boolean insideThisZone;
    /** max(0, distanceToCenter − radius) — 0 when inside. */
    private double distanceMetersToEdge;
}
