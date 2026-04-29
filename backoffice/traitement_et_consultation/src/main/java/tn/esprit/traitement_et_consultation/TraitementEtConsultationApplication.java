package tn.esprit.traitement_et_consultation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TraitementEtConsultationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraitementEtConsultationApplication.class, args);
    }

}
