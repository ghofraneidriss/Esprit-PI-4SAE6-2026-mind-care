package tn.esprit.microservice.reclamationms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.entity.ReclamationStatus;
import tn.esprit.microservice.reclamationms.repository.ReclamationRepository;

@Configuration
public class ReclamationDataInitializer {

    @Bean
    CommandLineRunner loadReclamations(ReclamationRepository reclamationRepository) {
        return args -> {
            if (reclamationRepository.count() > 0) {
                return;
            }

            Reclamation r1 = new Reclamation();
            r1.setUserName("Meriem");
            r1.setUserEmail("meriem@example.com");
            r1.setSubject("Probleme acces cours");
            r1.setImageUrl("https://picsum.photos/seed/reclamation-1/600/300");
            r1.setDescription("Je n'arrive pas a ouvrir le module video.");
            r1.setStatus(ReclamationStatus.OPEN);

            Reclamation r2 = new Reclamation();
            r2.setUserName("Hatem");
            r2.setUserEmail("hatem@example.com");
            r2.setSubject("Erreur paiement");
            r2.setImageUrl("https://picsum.photos/seed/reclamation-2/600/300");
            r2.setDescription("Mon paiement est debite mais inscription non activee.");
            r2.setStatus(ReclamationStatus.IN_PROGRESS);

            reclamationRepository.save(r1);
            reclamationRepository.save(r2);
        };
    }
}
