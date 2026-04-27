package tn.esprit.incident_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.incident_service.entity.IncidentType;
import tn.esprit.incident_service.enums.SeverityLevel;
import tn.esprit.incident_service.repository.IncidentTypeRepository;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class IncidentTypeSeederConfig {

    private final IncidentTypeRepository incidentTypeRepository;

    @Bean
    CommandLineRunner seedIncidentTypes() {
        return args -> {
            if (incidentTypeRepository.count() > 0) {
                return;
            }

            List<IncidentType> defaults = List.of(
                    IncidentType.builder()
                            .name("CHUTE")
                            .description("Chute ou perte d'equilibre du patient")
                            .defaultSeverity(SeverityLevel.HIGH)
                            .points(20)
                            .build(),
                    IncidentType.builder()
                            .name("AGITATION")
                            .description("Episode d'agitation ou comportement inhabituel")
                            .defaultSeverity(SeverityLevel.MEDIUM)
                            .points(12)
                            .build(),
                    IncidentType.builder()
                            .name("ERRANCE")
                            .description("Sortie non autorisee ou deambulation a risque")
                            .defaultSeverity(SeverityLevel.HIGH)
                            .points(18)
                            .build(),
                    IncidentType.builder()
                            .name("OUBLI_TRAITEMENT")
                            .description("Oubli de medicament ou refus de traitement")
                            .defaultSeverity(SeverityLevel.MEDIUM)
                            .points(10)
                            .build(),
                    IncidentType.builder()
                            .name("URGENCE_MEDICALE")
                            .description("Situation medicale urgente necessitant intervention")
                            .defaultSeverity(SeverityLevel.CRITICAL)
                            .points(30)
                            .build()
            );

            incidentTypeRepository.saveAll(defaults);
        };
    }
}
