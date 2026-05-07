package tn.esprit.localization_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LocalizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalizationServiceApplication.class, args);
    }
}
