package tn.esprit.forums_service.entity;

public enum ModerationAction {
    DISMISS,
    /** Remove an active forum ban before it expires (only after a prior ban decision). */
    LIFT_BAN,
    DELETE_COMMENT,
    BAN_1_DAY,
    BAN_3_DAYS,
    BAN_7_DAYS
}
