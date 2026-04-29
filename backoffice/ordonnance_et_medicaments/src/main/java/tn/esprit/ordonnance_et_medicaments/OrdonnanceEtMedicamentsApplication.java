package tn.esprit.ordonnance_et_medicaments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class OrdonnanceEtMedicamentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdonnanceEtMedicamentsApplication.class, args);
    }

    /**
     * Bean RestTemplate utilisé par PrescriptionMailService
     * pour appeler le microservice users_service et récupérer l'email du patient.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
