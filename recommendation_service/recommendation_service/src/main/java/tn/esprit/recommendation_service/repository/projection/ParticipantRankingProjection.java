package tn.esprit.recommendation_service.repository.projection;

import tn.esprit.recommendation_service.enums.ParticipantType;

public interface ParticipantRankingProjection {
    Long getParticipantId();
    ParticipantType getParticipantType();
    Long getTotalScore();
    Long getParticipations();
}
