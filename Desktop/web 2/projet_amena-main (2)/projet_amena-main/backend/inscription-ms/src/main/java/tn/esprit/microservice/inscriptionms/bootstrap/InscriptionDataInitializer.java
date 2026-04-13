package tn.esprit.microservice.inscriptionms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.microservice.inscriptionms.entity.Inscription;
import tn.esprit.microservice.inscriptionms.repository.InscriptionRepository;

@Configuration
public class InscriptionDataInitializer {

    @Bean
    CommandLineRunner loadInscriptions(InscriptionRepository inscriptionRepository) {
        return args -> {
            if (inscriptionRepository.count() > 0) {
                return;
            }

            Inscription inscription = new Inscription();
            inscription.setStudentName("Demo Student");
            inscription.setStudentEmail("student@example.com");
            inscription.setCoursId(1L);

            inscriptionRepository.save(inscription);
        };
    }
}
