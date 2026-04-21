package tn.esprit.movement_service.entity;

public enum AlertType {
    OUT_OF_SAFE_ZONE,
    /** Patient crossed from inside to outside the zone marked as registered home (no duplicate cooldown). */
    LEFT_REGISTERED_HOME,
    IMMOBILE_TOO_LONG,
    RAPID_OR_UNUSUAL_MOVEMENT,
    GPS_NO_DATA
}
